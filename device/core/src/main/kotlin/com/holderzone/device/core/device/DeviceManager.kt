package com.holderzone.device.core.device

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.logging.DeviceLogEntry
import com.holderzone.device.api.base.logging.DeviceLogLevel
import com.holderzone.device.api.base.logging.DeviceLogListener
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.core.capability.CapabilityAggregator
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.sniffer.AutoSniffer
import com.holderzone.device.core.strategy.StrategyRegistry
import com.holderzone.device.core.watchdog.WatchdogPolicy
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备管理门面，负责驱动注册、设备绑定、能力查询、状态流和自动探测入口。
 */
class DeviceManager(
    private val strategyRegistry: StrategyRegistry = StrategyRegistry(),
    private val deviceRegistry: DeviceRegistry = DeviceRegistry(),
    private val capabilityAggregator: CapabilityAggregator = CapabilityAggregator(),
    private val serialPortManager: SerialPortManager = SerialPortManager(),
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    watchdogPolicy: WatchdogPolicy = WatchdogPolicy(),
) {
    /** 每个设备独立的连接状态流，按 deviceId 延迟创建。 */
    private val stateFlows = mutableMapOf<String, MutableStateFlow<ConnectionState>>()
    private val globalStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val deviceRecordsFlow = MutableStateFlow<List<DeviceRecord>>(emptyList())
    private val mutableDevicesFlow = MutableStateFlow<List<IDevice>>(emptyList())
    private val connectionEventsFlow = MutableSharedFlow<DeviceConnectionEvent>(
        extraBufferCapacity = CONNECTION_EVENT_BUFFER_CAPACITY,
    )
    private val logsFlow = MutableSharedFlow<DeviceLogEntry>(
        extraBufferCapacity = LOG_EVENT_BUFFER_CAPACITY,
    )
    private val logListeners = CopyOnWriteArraySet<DeviceLogListener>()
    private val runtimeHost = object : DeviceRuntimeHost {
        override suspend fun bindDeviceInternal(driver: IDeviceDriver, channel: SerialChannel): IDevice {
            return this@DeviceManager.bindDeviceInternal(driver, channel)
        }

        override fun unbindDevice(deviceId: String) {
            this@DeviceManager.unbindDevice(deviceId)
        }

        override fun updateState(deviceId: String, state: ConnectionState) {
            this@DeviceManager.updateState(deviceId, state)
        }

        override fun markGlobalState(state: ConnectionState) {
            this@DeviceManager.markGlobalState(state)
        }

        override fun emitLog(
            level: DeviceLogLevel,
            tag: String,
            message: String,
            deviceId: String?,
            strategyId: String?,
            portPath: String?,
            throwable: Throwable?,
        ) {
            this@DeviceManager.emitLog(
                level = level,
                tag = tag,
                message = message,
                deviceId = deviceId,
                strategyId = strategyId,
                portPath = portPath,
                throwable = throwable,
            )
        }
    }
    private val runtime = DeviceRuntime(
        host = runtimeHost,
        autoSniffer = AutoSniffer(
            serialPortManager = serialPortManager,
            strategyRegistry = strategyRegistry,
        ),
        serialPortManager = serialPortManager,
        scope = coroutineScope,
        watchdogPolicy = watchdogPolicy,
    )

    val registeredDrivers: List<IDeviceDriver>
        get() = strategyRegistry.all()

    val globalState: StateFlow<ConnectionState>
        get() = globalStateFlow.asStateFlow()

    val deviceRecords: StateFlow<List<DeviceRecord>>
        get() = deviceRecordsFlow.asStateFlow()

    val devicesFlow: StateFlow<List<IDevice>>
        get() = mutableDevicesFlow.asStateFlow()

    val devices: StateFlow<List<IDevice>>
        get() = devicesFlow

    val connectionEvents: SharedFlow<DeviceConnectionEvent>
        get() = connectionEventsFlow.asSharedFlow()

    val logs: SharedFlow<DeviceLogEntry>
        get() = logsFlow.asSharedFlow()

    var minLogLevel: DeviceLogLevel = DeviceLogLevel.WARN

    /** 注册一个驱动策略，后续自动探测会按 priority 和 strategyId 排序尝试。 */
    fun registerDriver(driver: IDeviceDriver) {
        strategyRegistry.register(driver)
    }

    /** 取消注册指定驱动策略，已绑定设备不会在这里被自动解绑。 */
    fun unregisterDriver(strategyId: String) {
        strategyRegistry.unregister(strategyId)
    }

    /** 打开通道、执行驱动初始化、创建设备、聚合能力，并发布绑定事件。 */
    private suspend fun bindDeviceInternal(driver: IDeviceDriver, channel: SerialChannel): IDevice {
        channel.open()
        check(driver.deviceFactory.initialize(channel)) {
            "Driver ${driver.descriptor.strategyId} failed to initialize ${channel.portPath}."
        }

        val device = driver.deviceFactory.createDevice(channel)
        val capabilities = capabilityAggregator.aggregate(
            device = device,
            capabilityTypes = driver.descriptor.capabilities,
        )
        deviceRegistry.bind(
            device = device,
            driver = driver,
            capabilities = capabilities,
        )
        stateFlows.getOrPut(device.info.deviceId) {
            MutableStateFlow(ConnectionState.CONNECTED)
        }.value = ConnectionState.CONNECTED
        publishDevices()
        val record = deviceRegistry.find(device.info.deviceId)
        if (record != null) {
            publishConnectionEvent(DeviceConnectionEvent.Bound(record))
        }
        emitLog(
            level = DeviceLogLevel.INFO,
            tag = TAG,
            message = "设备已绑定：${device.info.deviceId}",
            deviceId = device.info.deviceId,
            strategyId = driver.descriptor.strategyId,
            portPath = channel.portPath,
            throwable = null,
        )
        return device
    }

    suspend fun bindSession(driver: IDeviceDriver, channel: SerialChannel): IDevice {
        return runtime.bindSession(driver, channel)
    }

    /** 启动一次自动探测任务；命中设备后会进入运行时绑定流程。 */
    fun startAutoSniffing() {
        runtime.startAutoSniffing()
    }

    /** 停止当前自动探测任务，不影响已经绑定的设备会话。 */
    fun stopAutoSniffing() {
        runtime.stopAutoSniffing()
    }

    /** 停止指定设备会话，关闭通道并从注册表解绑。 */
    fun stopDevice(deviceId: String) {
        runtime.stopDevice(deviceId)
    }

    fun getDevice(deviceId: String): IDevice? = deviceRegistry.find(deviceId)?.device

    fun getDevices(): List<IDevice> = deviceRegistry.all().map { it.device }

    inline fun <reified T : Any> queryCapability(deviceId: String): T? {
        return queryCapability(deviceId, T::class.java)
    }

    fun <T : Any> queryCapability(deviceId: String, capabilityType: Class<T>): T? {
        val capability = deviceRegistry.find(deviceId)?.capabilities?.get(capabilityType)
        return capabilityType.cast(capability)
    }

    /** 从注册表移除设备，更新状态流，并向上层广播 Unbound 事件。 */
    fun unbindDevice(deviceId: String) {
        val record = deviceRegistry.unbind(deviceId) ?: return
        stateFlows.getOrPut(deviceId) {
            MutableStateFlow(ConnectionState.DISCONNECTED)
        }.value = ConnectionState.DISCONNECTED
        publishDevices()
        publishConnectionEvent(DeviceConnectionEvent.Unbound(deviceId, record))
        emitLog(
            level = DeviceLogLevel.INFO,
            tag = TAG,
            message = "设备已解绑：$deviceId",
            deviceId = deviceId,
            strategyId = record.driver.descriptor.strategyId,
            portPath = null,
            throwable = null,
        )
    }

    fun observeDevice(deviceId: String): StateFlow<ConnectionState> {
        return stateFlows.getOrPut(deviceId) {
            MutableStateFlow(
                deviceRegistry.find(deviceId)?.state ?: ConnectionState.DISCONNECTED,
            )
        }.asStateFlow()
    }

    /** 更新设备状态，并在状态实际变化时发布 StateChanged 事件。 */
    fun updateState(deviceId: String, state: ConnectionState) {
        val previousState = deviceRegistry.find(deviceId)?.state ?: stateFlows[deviceId]?.value
        deviceRegistry.updateState(deviceId, state)
        stateFlows.getOrPut(deviceId) { MutableStateFlow(state) }.value = state
        publishDevices()
        if (previousState != state) {
            publishConnectionEvent(
                DeviceConnectionEvent.StateChanged(
                    deviceId = deviceId,
                    previousState = previousState,
                    state = state,
                    record = deviceRegistry.find(deviceId),
                ),
            )
            emitStateLog(deviceId = deviceId, previousState = previousState, state = state)
        }
    }

    fun markGlobalState(state: ConnectionState) {
        globalStateFlow.value = state
    }

    /** 清理运行时、驱动注册表、设备注册表和所有状态缓存。 */
    fun clear() {
        runtime.stopAll()
        strategyRegistry.clear()
        deviceRegistry.clear()
        stateFlows.values.forEach { it.value = ConnectionState.DISCONNECTED }
        stateFlows.clear()
        globalStateFlow.value = ConnectionState.DISCONNECTED
        publishDevices()
        publishConnectionEvent(DeviceConnectionEvent.Cleared)
        emitLog(
            level = DeviceLogLevel.INFO,
            tag = TAG,
            message = "设备管理器已清空。",
            deviceId = null,
            strategyId = null,
            portPath = null,
            throwable = null,
        )
    }

    fun addLogListener(listener: DeviceLogListener): Closeable {
        logListeners += listener
        return Closeable { removeLogListener(listener) }
    }

    fun removeLogListener(listener: DeviceLogListener) {
        logListeners -= listener
    }

    fun emitLog(
        level: DeviceLogLevel,
        tag: String,
        message: String,
        deviceId: String? = null,
        strategyId: String? = null,
        portPath: String? = null,
        throwable: Throwable? = null,
    ) {
        if (!level.isAtLeast(minLogLevel)) return

        val entry = DeviceLogEntry(
            level = level,
            tag = tag,
            message = message,
            deviceId = deviceId,
            strategyId = strategyId,
            portPath = portPath,
            throwable = throwable,
        )
        logsFlow.tryEmit(entry)
        logListeners.forEach { listener ->
            runCatching {
                listener.onLog(entry)
            }
        }
    }

    private fun publishDevices() {
        val records = deviceRegistry.all()
        deviceRecordsFlow.value = records
        mutableDevicesFlow.value = records.map { it.device }
    }

    private fun publishConnectionEvent(event: DeviceConnectionEvent) {
        connectionEventsFlow.tryEmit(event)
    }

    private fun emitStateLog(deviceId: String, previousState: ConnectionState?, state: ConnectionState) {
        val level = when (state) {
            ConnectionState.DEGRADED,
            ConnectionState.RECONNECTING,
            -> DeviceLogLevel.WARN
            else -> DeviceLogLevel.INFO
        }
        emitLog(
            level = level,
            tag = TAG,
            message = "设备状态变更：$deviceId，${previousState ?: "-"} -> $state",
            deviceId = deviceId,
            strategyId = deviceRegistry.find(deviceId)?.driver?.descriptor?.strategyId,
            portPath = null,
            throwable = null,
        )
    }

    companion object {
        private const val CONNECTION_EVENT_BUFFER_CAPACITY = 64
        private const val LOG_EVENT_BUFFER_CAPACITY = 128
        private const val TAG = "DeviceManager"

        private val shared = DeviceManager()

        fun getInstance(): DeviceManager = shared
    }
}
