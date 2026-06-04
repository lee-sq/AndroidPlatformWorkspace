package com.holderzone.device.core.sniffer

import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.core.channel.SelfManagedSerialChannel
import com.holderzone.device.core.channel.SerialPortInfo
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.strategy.StrategyRegistry

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 自动探测器，遍历驱动、端口和串口配置，找到第一个可匹配设备。
 */
class AutoSniffer(
    private val serialPortManager: SerialPortManager,
    private val strategyRegistry: StrategyRegistry,
    private val probePipeline: ProbePipeline = ProbePipeline(),
) {
    /** 遍历所有驱动、端口和串口配置，返回第一组探测成功的设备信息。 */
    suspend fun sniffOnce(): SniffResult {
        val drivers = strategyRegistry.all()
        val ports = serialPortManager.listPorts()
        for (driver in drivers) {
            // 优先尝试驱动声明的常用端口，可以缩短真实硬件上的探测时间。
            val orderedPorts = ports.candidatesFor(
                preferredPortPaths = driver.descriptor.preferredPortPaths,
                selfManagedConnection = driver.descriptor.selfManagedConnection,
            )
            for (port in orderedPorts) {
                for (config in driver.descriptor.supportedConfigs) {
                    val channel = if (driver.descriptor.selfManagedConnection) {
                        SelfManagedSerialChannel(port.path, config)
                    } else {
                        serialPortManager.openChannel(port.path, config)
                    }
                    when (val result = probePipeline.probe(driver, channel)) {
                        is ProbeResult.Matched -> {
                            return SniffResult.Matched(
                                driver = driver,
                                channel = channel,
                                probeResult = result,
                            )
                        }
                        is ProbeResult.Error,
                        ProbeResult.Mismatched,
                        -> {
                            // 未命中或探测失败的通道不能留给后续组合复用，避免端口资源泄漏。
                            channel.close()
                        }
                    }
                }
            }
        }
        return SniffResult.NoMatch
    }

    private fun List<SerialPortInfo>.candidatesFor(
        preferredPortPaths: List<String>,
        selfManagedConnection: Boolean,
    ): List<SerialPortInfo> {
        if (preferredPortPaths.isEmpty()) return this

        val preferred = preferredPortPaths.map { preferredPath ->
            firstOrNull { it.path == preferredPath } ?: SerialPortInfo(preferredPath)
        }
        if (selfManagedConnection) return preferred
        return (preferred + filterNot { port -> preferred.any { it.path == port.path } })
    }
}
