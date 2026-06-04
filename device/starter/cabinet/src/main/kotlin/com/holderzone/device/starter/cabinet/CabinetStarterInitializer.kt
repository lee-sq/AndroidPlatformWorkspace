package com.holderzone.device.starter.cabinet

import android.content.Context
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.cabinet.jw.serial.JwSerialCabinetDriver
import com.holderzone.device.driver.cabinet.star.StarCabinetDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 柜体 starter 初始化入口，向 DeviceManager 注册柜体领域驱动。
 */
object CabinetStarterInitializer {
    fun init(deviceManager: DeviceManager) {
        deviceManager.registerDriver(JwSerialCabinetDriver())
    }

    fun init(deviceManager: DeviceManager, context: Context) {
        deviceManager.registerDriver(JwSerialCabinetDriver(context.applicationContext))
        deviceManager.registerDriver(StarCabinetDriver(context.applicationContext))
    }
}
