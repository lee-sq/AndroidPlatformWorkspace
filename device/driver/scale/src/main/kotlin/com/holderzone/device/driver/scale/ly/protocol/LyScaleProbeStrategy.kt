package com.holderzone.device.driver.scale.ly.protocol

import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.driver.scale.ly.LyScaleDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 16:48
 * Description: 亮悦电子秤探测策略，监听主动上报帧并以可解析重量帧作为匹配依据。
 */
class LyScaleProbeStrategy(
    private val protocol: LyScaleProtocol = LyScaleProtocol,
) : IProbeStrategy {
    override fun buildProbeFrame(): ByteArray = ByteArray(0)

    override fun validateResponse(response: ByteArray): ProbeResult {
        val slice = protocol.findFrameSlice(response) ?: return ProbeResult.Mismatched
        val reading = protocol.parseFrame(slice.frame) ?: return ProbeResult.Mismatched
        return ProbeResult.Matched(
            deviceModel = LyScaleDriver.DEVICE_MODEL,
            firmwareVersion = reading.flag,
        )
    }
}
