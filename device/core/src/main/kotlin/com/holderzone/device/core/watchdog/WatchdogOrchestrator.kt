package com.holderzone.device.core.watchdog

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 看门狗编排容器，按 deviceId 管理多个设备的 Watchdog。
 */
class WatchdogOrchestrator {
    private val watchdogs = mutableMapOf<String, Watchdog>()

    fun register(deviceId: String, watchdog: Watchdog = Watchdog()) {
        watchdogs[deviceId] = watchdog
    }

    fun unregister(deviceId: String) {
        watchdogs.remove(deviceId)
    }

    fun get(deviceId: String): Watchdog? = watchdogs[deviceId]
}
