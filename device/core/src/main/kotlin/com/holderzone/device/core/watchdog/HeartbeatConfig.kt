package com.holderzone.device.core.watchdog

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 心跳配置模型，定义心跳间隔和超时时间。
 */
data class HeartbeatConfig(
    val intervalMs: Long,
    val timeoutMs: Long,
)
