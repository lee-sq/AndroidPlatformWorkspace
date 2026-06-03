package com.holderzone.device.core.watchdog

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 心跳调度判断器，根据经过时间判断是否应该发送下一次心跳。
 */
class HeartbeatScheduler(
    val config: HeartbeatConfig,
) {
    fun shouldHeartbeat(elapsedMs: Long): Boolean = elapsedMs >= config.intervalMs
}
