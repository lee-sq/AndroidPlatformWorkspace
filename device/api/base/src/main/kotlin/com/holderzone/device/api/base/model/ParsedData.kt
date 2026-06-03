package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 协议解析后的结构化数据容器，用 type 和字段表承载 driver 识别出的帧内容。
 */
data class ParsedData(
    val type: String,
    val fields: Map<String, Any?> = emptyMap(),
)
