package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig
import java.io.File

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Android 默认串口后端，扫描 /dev 设备文件并创建真实串口通道。
 */
class AndroidSerialBackend(
    deviceDirectory: File = File("/dev"),
    private val channelFactory: (String, SerialConfig) -> SerialChannel = { portPath, config ->
        AndroidSerialChannel(portPath = portPath, config = config)
    },
) : SerialBackend {
    private val scanner = DeviceFileSerialBackend(
        deviceDirectory = deviceDirectory,
        channelFactory = channelFactory,
    )

    override fun listPorts(): List<SerialPortInfo> = scanner.listPorts()

    override fun createChannel(portPath: String, config: SerialConfig): SerialChannel {
        return channelFactory(portPath, config)
    }
}
