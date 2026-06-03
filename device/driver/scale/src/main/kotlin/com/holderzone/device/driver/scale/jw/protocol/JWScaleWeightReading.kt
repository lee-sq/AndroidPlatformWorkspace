package com.holderzone.device.driver.scale.jw.protocol

import com.holderzone.device.api.scale.model.WeightResult

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 原始称重读数，保留重量结果和协议帧中的诊断字段。
 */
data class JWScaleWeightReading(
    val result: WeightResult,
    val adValue: Long,
    val zeroAdValue: Long,
    val batteryVoltage: Double,
    val temperatureCelsius: Int,
    val coefficient: Long,
    val grossWeight: Double,
    val tareWeight: Double,
    val holdState: Int,
    val raw: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JWScaleWeightReading) return false

        return result == other.result &&
            adValue == other.adValue &&
            zeroAdValue == other.zeroAdValue &&
            batteryVoltage == other.batteryVoltage &&
            temperatureCelsius == other.temperatureCelsius &&
            coefficient == other.coefficient &&
            grossWeight == other.grossWeight &&
            tareWeight == other.tareWeight &&
            holdState == other.holdState &&
            raw.contentEquals(other.raw)
    }

    override fun hashCode(): Int {
        var hash = result.hashCode()
        hash = 31 * hash + adValue.hashCode()
        hash = 31 * hash + zeroAdValue.hashCode()
        hash = 31 * hash + batteryVoltage.hashCode()
        hash = 31 * hash + temperatureCelsius
        hash = 31 * hash + coefficient.hashCode()
        hash = 31 * hash + grossWeight.hashCode()
        hash = 31 * hash + tareWeight.hashCode()
        hash = 31 * hash + holdState
        hash = 31 * hash + raw.contentHashCode()
        return hash
    }
}
