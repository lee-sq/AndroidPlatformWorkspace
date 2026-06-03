package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口管理器，作为上层探测流程访问 SerialBackend 的统一入口。
 */
class SerialPortManager(
    private val backend: SerialBackend = AndroidSerialBackend(),
) {
    /** 返回后端当前能看到的串口端口，供 AutoSniffer 逐一探测。 */
    fun listPorts(): List<SerialPortInfo> = backend.listPorts()

    /** 创建候选通道；是否真正 open 由 ProbePipeline 或 DeviceManager 决定。 */
    fun openChannel(portPath: String, config: SerialConfig): SerialChannel {
        return backend.createChannel(portPath = portPath, config = config)
    }
}
