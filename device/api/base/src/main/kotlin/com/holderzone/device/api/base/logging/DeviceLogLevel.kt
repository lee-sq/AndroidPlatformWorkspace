package com.holderzone.device.api.base.logging

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 15:50
 * Description: Device 日志等级，业务方可按最低等级筛选自己关心的日志事件。
 */
enum class DeviceLogLevel(val priority: Int) {
    DEBUG(priority = 0),
    INFO(priority = 1),
    WARN(priority = 2),
    ERROR(priority = 3),
    ;

    fun isAtLeast(minimum: DeviceLogLevel): Boolean = priority >= minimum.priority
}
