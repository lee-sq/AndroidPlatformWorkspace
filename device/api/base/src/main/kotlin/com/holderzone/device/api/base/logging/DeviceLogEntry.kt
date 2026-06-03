package com.holderzone.device.api.base.logging

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 15:50
 * Description: Device 侧产生的轻量日志事件，只描述发生了什么，不决定日志如何记录。
 */
data class DeviceLogEntry(
    val level: DeviceLogLevel,
    val tag: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
    val strategyId: String? = null,
    val portPath: String? = null,
    val throwable: Throwable? = null,
)
