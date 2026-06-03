package com.holderzone.device.core.watchdog

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 连接看门狗，根据最后一次喂狗时间判断主动上报设备是否超时。
 */
class Watchdog(
    private val policy: WatchdogPolicy = WatchdogPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var lastFeedAt: Long = clock()

    /** 收到合法设备帧或心跳响应时刷新活跃时间。 */
    fun feed() {
        lastFeedAt = clock()
    }

    /** 判断距离上次 feed 是否已经超过策略超时时间。 */
    fun isTimeout(): Boolean {
        return clock() - lastFeedAt > policy.timeoutMs
    }

    /** 重置看门狗状态，本质上等同于立即喂狗。 */
    fun reset() {
        feed()
    }
}
