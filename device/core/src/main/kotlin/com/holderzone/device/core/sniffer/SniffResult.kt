package com.holderzone.device.core.sniffer

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 自动探测结果密封类，表示匹配到设备或本轮扫描未命中。
 */
sealed class SniffResult {
    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 自动探测命中结果，携带匹配驱动、保持打开的串口通道和探测详情。
     */
    data class Matched(
        val driver: IDeviceDriver,
        val channel: SerialChannel,
        val probeResult: ProbeResult.Matched,
    ) : SniffResult()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 自动探测未命中结果，表示所有驱动、端口和配置组合都没有匹配。
     */
    data object NoMatch : SniffResult()
}
