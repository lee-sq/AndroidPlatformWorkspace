package com.holderzone.device.api.scale.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 显示内容模型，承载主文本和可选副文本。
 */
data class DisplayContent(
    val primaryText: String,
    val secondaryText: String? = null,
)
