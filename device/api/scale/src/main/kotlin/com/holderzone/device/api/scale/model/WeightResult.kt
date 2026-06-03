package com.holderzone.device.api.scale.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 称重结果模型，记录重量值、单位、稳定状态和采集时间。
 */
data class WeightResult(
    val value: Double,
    val unit: WeightUnit,
    val stable: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    val grams: Double
        get() = unit.toGrams(value)
}
