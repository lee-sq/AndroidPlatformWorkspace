package com.holderzone.device.driver.scale.ly.protocol

import com.holderzone.device.api.scale.model.WeightResult

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 16:48
 * Description: 亮悦电子秤原始读数，保留协议中的皮重、稳定标记和原始 ASCII 帧。
 */
data class LyScaleReading(
    val result: WeightResult,
    val tareWeight: Double,
    val netMode: Boolean,
    val zero: Boolean,
    val flag: String,
    val rawFrame: String,
)
