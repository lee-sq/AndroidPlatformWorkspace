package com.holderzone.device.api.base.logging

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 15:50
 * Description: Device 日志事件回调，供 Java 或非协程业务方接入自己的日志系统。
 */
fun interface DeviceLogListener {
    fun onLog(entry: DeviceLogEntry)
}
