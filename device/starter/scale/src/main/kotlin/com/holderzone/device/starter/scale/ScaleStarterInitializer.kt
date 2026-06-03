package com.holderzone.device.starter.scale

import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.scale.jw.JWScaleDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 电子秤 starter 初始化入口，向 DeviceManager 注册称重领域驱动。
 */
object ScaleStarterInitializer {
    fun init(deviceManager: DeviceManager) {
        deviceManager.registerDriver(JWScaleDriver())
    }
}
