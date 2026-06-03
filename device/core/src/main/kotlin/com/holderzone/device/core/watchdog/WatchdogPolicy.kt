package com.holderzone.device.core.watchdog

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 看门狗策略模型，定义超时时间和最大重试次数。
 */
data class WatchdogPolicy(
    val timeoutMs: Long = 10_000L,
    val maxRetryCount: Int = 3,
)
