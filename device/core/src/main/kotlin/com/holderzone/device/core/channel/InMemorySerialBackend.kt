package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 内存串口后端，供单元测试或模拟环境注入端口和通道。
 */
class InMemorySerialBackend(
    ports: List<SerialPortInfo> = emptyList(),
    private val channelFactory: (String, SerialConfig) -> InMemorySerialChannel = { portPath, config ->
        InMemorySerialChannel(portPath = portPath, config = config)
    },
) : SerialBackend {
    private val ports = ports.toMutableList()
    private val channels = mutableListOf<InMemorySerialChannel>()

    val createdChannels: List<InMemorySerialChannel>
        get() = channels.toList()

    override fun listPorts(): List<SerialPortInfo> = ports.toList()

    override fun createChannel(portPath: String, config: SerialConfig): SerialChannel {
        return channelFactory(portPath, config).also { channels += it }
    }

    fun setPorts(nextPorts: List<SerialPortInfo>) {
        ports.clear()
        ports += nextPorts
    }
}
