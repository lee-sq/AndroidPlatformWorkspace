package com.holderzone.device.starter.cabinet

import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.cabinet.x.CabinetXDriver
import com.holderzone.device.driver.cabinet.y.CabinetYDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 柜体 starter 初始化入口，向 DeviceManager 注册柜体领域驱动。
 */
object CabinetStarterInitializer {
    fun init(deviceManager: DeviceManager) {
        deviceManager.registerDriver(CabinetXDriver())
        deviceManager.registerDriver(CabinetYDriver())
    }
}
