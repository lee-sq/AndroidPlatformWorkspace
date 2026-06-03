package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig
import java.io.File

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备文件扫描后端，负责枚举 /dev 下常见 tty 串口节点并交给工厂创建通道。
 */
class DeviceFileSerialBackend(
    private val deviceDirectory: File = File("/dev"),
    private val channelFactory: (String, SerialConfig) -> SerialChannel = { portPath, config ->
        UnsupportedSerialChannel(portPath = portPath, config = config)
    },
) : SerialBackend {
    private val portPattern = Regex("""tty(S|USB|ACM)\d+""")

    /** 扫描设备目录中常见串口文件，并按文件名排序保证探测顺序稳定。 */
    override fun listPorts(): List<SerialPortInfo> {
        return deviceDirectory
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.name.matches(portPattern) }
            .sortedBy { it.name }
            .map { SerialPortInfo(path = it.absolutePath) }
            .toList()
    }

    /** 根据端口路径和串口配置创建通道，真实实现由注入的 channelFactory 决定。 */
    override fun createChannel(portPath: String, config: SerialConfig): SerialChannel {
        return channelFactory(portPath, config)
    }
}
