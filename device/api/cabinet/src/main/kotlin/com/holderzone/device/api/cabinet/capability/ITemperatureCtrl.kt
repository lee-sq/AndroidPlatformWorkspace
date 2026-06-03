package com.holderzone.device.api.cabinet.capability

import com.holderzone.device.api.cabinet.model.TemperatureReading

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 温控能力接口，定义设置目标温度和读取当前温度的动作。
 */
interface ITemperatureCtrl {
    suspend fun setTemperature(celsius: Double): Boolean

    suspend fun getTemperature(): TemperatureReading
}
