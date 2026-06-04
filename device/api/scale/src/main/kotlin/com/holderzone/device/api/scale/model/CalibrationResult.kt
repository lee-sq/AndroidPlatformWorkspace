package com.holderzone.device.api.scale.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/4 10:20
 * Description: 称重标定结果，业务方可读取 slope 后自行决定是否持久化。
 */
data class CalibrationResult(
    val success: Boolean,
    val slope: Double? = null,
    val rawWeightGrams: Double? = null,
    val zeroOffsetGrams: Double? = null,
    val standardWeightGrams: Double,
)
