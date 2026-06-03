package com.holderzone.device.core.sniffer

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 探测调度配置，保存自动扫描间隔等调度参数。
 */
class SniffScheduler(
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    companion object {
        const val DEFAULT_INTERVAL_MS: Long = 30_000L
    }
}
