package com.holderzone.device.api.cabinet.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 打印内容模型，承载票据标题和多行正文。
 */
data class PrintContent(
    val title: String,
    val lines: List<String> = emptyList(),
    val widthMm: Int? = null,
    val heightMm: Int? = null,
)
