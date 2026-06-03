package com.holderzone.device.core.device

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.logging.DeviceLogLevel
import com.holderzone.device.api.base.model.CommunicationMode
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.core.channel.FrameStreamReader
import com.holderzone.device.core.channel.SerialReadLoop
import com.holderzone.device.core.sniffer.AutoSniffer
import com.holderzone.device.core.sniffer.SniffResult
import com.holderzone.device.core.watchdog.Watchdog
import com.holderzone.device.core.watchdog.WatchdogPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备运行时编排器，串联自动探测、设备绑定、读循环、看门狗和异常重连。
 */
class DeviceRuntime(
    private val deviceManager: DeviceManager,
    private val autoSniffer: AutoSniffer,
    private val scope: CoroutineScope,
    private val watchdogPolicy: WatchdogPolicy = WatchdogPolicy(),
    private val reconnectDelayMs: Long = DEFAULT_RECONNECT_DELAY_MS,
) {
    /** 已绑定设备的运行会话，key 为 deviceId，便于按设备关闭和重连。 */
    private val sessions = mutableMapOf<String, DeviceSession>()
    private val reconnectJobs = mutableMapOf<String, Job>()
    private var sniffJob: Job? = null

    val isSniffing: Boolean
        get() = sniffJob?.isActive == true

    /** 启动一轮自动探测：进入 SNIFFING，命中则绑定，未命中则回到 DISCONNECTED。 */
    fun startAutoSniffing() {
        if (isSniffing) return

        sniffJob = scope.launch {
            deviceManager.emitLog(
                level = DeviceLogLevel.INFO,
                tag = TAG,
                message = "开始自动探测设备。",
            )
            deviceManager.markGlobalState(ConnectionState.SNIFFING)
            while (isActive) {
                when (val result = autoSniffer.sniffOnce()) {
                    is SniffResult.Matched -> {
                        deviceManager.emitLog(
                            level = DeviceLogLevel.INFO,
                            tag = TAG,
                            message = "自动探测命中设备：${result.driver.descriptor.strategyId}，端口：${result.channel.portPath}。",
                            strategyId = result.driver.descriptor.strategyId,
                            portPath = result.channel.portPath,
                        )
                        bindMatched(result)
                        return@launch
                    }
                    SniffResult.NoMatch -> {
                        deviceManager.emitLog(
                            level = DeviceLogLevel.INFO,
                            tag = TAG,
                            message = "本轮自动探测未匹配到设备，等待后继续扫描。",
                        )
                        delay(SNIFF_RETRY_DELAY_MS)
                    }
                }
            }
        }
    }

    fun stopAutoSniffing() {
        sniffJob?.cancel()
        sniffJob = null
    }

    /** 将自动探测结果中的 driver 和已打开通道交给正式会话绑定。 */
    suspend fun bindMatched(result: SniffResult.Matched) {
        bindSession(
            driver = result.driver,
            channel = result.channel,
        )
    }

    /** 创建完整设备会话：绑定设备、启动读循环、按主动上报模式启动看门狗。 */
    suspend fun bindSession(driver: IDeviceDriver, channel: SerialChannel): IDevice {
        val device = deviceManager.bindDevice(driver, channel)
        val watchdog = Watchdog(policy = watchdogPolicy)
        val frameReader = FrameStreamReader(driver.frameParser)

        val readLoop = SerialReadLoop(
            channel = channel,
            scope = scope,
            onBytes = { bytes ->
                // 串口读到的是连续字节流，这里先交给 FrameStreamReader 处理半包和粘包。
                val frames = frameReader.append(bytes)
                if (frames.isNotEmpty()) {
                    // 主动上报设备只要持续产出合法帧，就说明链路仍然活跃。
                    watchdog.feed()
                }
                frames.forEach(driver.frameParser::parseFrame)
            },
            onError = { throwable ->
                deviceManager.emitLog(
                    level = DeviceLogLevel.ERROR,
                    tag = TAG,
                    message = "设备读循环异常：${device.info.deviceId}",
                    deviceId = device.info.deviceId,
                    strategyId = driver.descriptor.strategyId,
                    portPath = channel.portPath,
                    throwable = throwable,
                )
                deviceManager.updateState(device.info.deviceId, ConnectionState.DEGRADED)
                scheduleReconnect(device.info.deviceId)
            },
        )

        sessions[device.info.deviceId]?.close()
        sessions[device.info.deviceId] = DeviceSession(
            driver = driver,
            channel = channel,
            readLoop = readLoop,
            watchdog = watchdog,
        )
        readLoop.start()

        if (driver.descriptor.communicationMode == CommunicationMode.ACTIVE_REPORT) {
            startWatchdog(device.info.deviceId)
        }
        return device
    }

    fun stopDevice(deviceId: String) {
        scope.launch {
            reconnectJobs.remove(deviceId)?.cancel()
            sessions.remove(deviceId)?.close()
            deviceManager.unbindDevice(deviceId)
        }
    }

    fun stopAll() {
        scope.launch {
            stopAllNow()
        }
    }

    /** 同步停止所有运行会话，常用于 clear 或测试环境中的资源释放。 */
    suspend fun stopAllNow() {
        stopAutoSniffing()
        reconnectJobs.values.forEach { job -> job.cancel() }
        reconnectJobs.clear()
        sessions.values.map { session ->
            scope.launch {
                session.close()
            }
        }.joinAll()
        sessions.clear()
    }

    private fun startWatchdog(deviceId: String) {
        scope.launch {
            while (sessions.containsKey(deviceId)) {
                delay(watchdogPolicy.timeoutMs)
                val session = sessions[deviceId] ?: return@launch
                if (session.watchdog.isTimeout()) {
                    // 超过策略时间没有收到合法帧，先降级，再关闭旧会话并重新扫描。
                    deviceManager.emitLog(
                        level = DeviceLogLevel.WARN,
                        tag = TAG,
                        message = "设备看门狗超时：$deviceId",
                        deviceId = deviceId,
                        strategyId = session.driver.descriptor.strategyId,
                        portPath = session.channel.portPath,
                    )
                    deviceManager.updateState(deviceId, ConnectionState.DEGRADED)
                    scheduleReconnect(deviceId)
                    return@launch
                }
            }
        }
    }

    /** 关闭异常会话，标记 RECONNECTING，延迟后解绑并重新进入自动探测。 */
    private fun scheduleReconnect(deviceId: String) {
        if (reconnectJobs[deviceId]?.isActive == true) return

        reconnectJobs[deviceId] = scope.launch {
            val session = sessions.remove(deviceId) ?: return@launch
            deviceManager.emitLog(
                level = DeviceLogLevel.WARN,
                tag = TAG,
                message = "设备已安排重连：$deviceId",
                deviceId = deviceId,
                strategyId = session.driver.descriptor.strategyId,
                portPath = session.channel.portPath,
            )
            session.close()
            deviceManager.updateState(deviceId, ConnectionState.RECONNECTING)
            delay(reconnectDelayMs)
            deviceManager.unbindDevice(deviceId)
            reconnectJobs.remove(deviceId)
            startAutoSniffing()
        }
    }

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 运行时设备会话，保存绑定后的驱动、通道、读循环和看门狗并负责统一关闭。
     */
    private class DeviceSession(
        val driver: IDeviceDriver,
        val channel: SerialChannel,
        val readLoop: SerialReadLoop,
        val watchdog: Watchdog,
    ) {
        suspend fun close() {
            readLoop.stop()
            channel.close()
        }
    }

    companion object {
        const val DEFAULT_RECONNECT_DELAY_MS: Long = 1_000L
        private const val SNIFF_RETRY_DELAY_MS = 1_000L
        private const val TAG = "DeviceRuntime"
    }
}
