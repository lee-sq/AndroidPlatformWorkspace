package com.holderzone.device.api.scale.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 重量单位枚举，提供不同单位换算成克的换算因子。
 */
enum class WeightUnit {
    GRAM,
    KILOGRAM,
    POUND,
    OUNCE;

    fun toGrams(value: Double): Double {
        return value * gramsFactor
    }

    val gramsFactor: Double
        get() = when (this) {
            GRAM -> 1.0
            KILOGRAM -> 1_000.0
            POUND -> 453.59237
            OUNCE -> 28.349523125
        }
}
