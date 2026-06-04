package com.holderzone.device.api.base.strategy

/**
 * Auth：ligen26
 * CrateTime：2026/6/4 11:10
 * Description: 问答式设备轮询策略，由 driver 声明命令，core 负责调度和健康监控。
 */
interface IPollingProvider {
    /** 轮询命令列表；运行时会按顺序循环发送。 */
    val pollingCommands: List<PollingCommand>
}

data class PollingCommand(
    val payload: ByteArray,
    val intervalMs: Long,
) {
    init {
        require(intervalMs > 0L) { "Polling interval must be greater than 0." }
    }
}
