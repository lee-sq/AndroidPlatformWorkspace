package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口后端接口，抽象端口枚举和通道创建，使真实串口和模拟后端可替换。
 */
interface SerialBackend {
    /** 枚举当前可探测的串口端口。 */
    fun listPorts(): List<SerialPortInfo>

    /** 为指定端口和串口配置创建一个尚未绑定业务设备的通信通道。 */
    fun createChannel(portPath: String, config: SerialConfig): SerialChannel
}
