package com.holderzone.device.api.base.strategy

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.ProbeResult

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 19:20
 * Description: 厂商 SDK 自管理连接探测策略，可感知当前候选端口但不通过 core 串口通道读写协议。
 */
interface ISelfManagedProbeStrategy : IProbeStrategy {
    fun validateChannel(channel: SerialChannel): ProbeResult
}
