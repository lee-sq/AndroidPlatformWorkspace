package com.holderzone.device.api.cabinet.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 打印结果模型，描述打印是否成功以及可选提示信息。
 */
data class PrintResult(
    val success: Boolean,
    val message: String? = null,
)
