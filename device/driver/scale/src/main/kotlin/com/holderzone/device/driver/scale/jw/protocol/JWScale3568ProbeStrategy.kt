package com.holderzone.device.driver.scale.jw.protocol

import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.driver.scale.jw.JWScaleDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 探测策略，通过读取版本命令和 CRC 校验确认设备身份。
 */
class JWScale3568ProbeStrategy(
    private val protocol: JWScale3568Protocol = JWScale3568Protocol,
) : IProbeStrategy {
    override fun buildProbeFrame(): ByteArray = protocol.readVersionCommand

    override fun validateResponse(response: ByteArray): ProbeResult {
        val frame = protocol.findFrame(response) ?: return ProbeResult.Mismatched
        if (!protocol.isValidCrc(frame)) return ProbeResult.Mismatched

        return ProbeResult.Matched(deviceModel = JWScaleDriver.DEVICE_MODEL)
    }
}
