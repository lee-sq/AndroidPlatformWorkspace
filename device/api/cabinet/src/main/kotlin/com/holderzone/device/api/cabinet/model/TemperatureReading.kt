package com.holderzone.device.api.cabinet.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 温度读数模型，记录摄氏温度和采集时间。
 */
data class TemperatureReading(
    val celsius: Double,
    val timestampMs: Long = System.currentTimeMillis(),
)
