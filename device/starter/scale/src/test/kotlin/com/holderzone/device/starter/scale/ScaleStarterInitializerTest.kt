package com.holderzone.device.starter.scale

import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.scale.jw.JWScaleDriver
import com.holderzone.device.driver.scale.ly.LyScaleDriver
import org.junit.Assert.assertEquals
import org.junit.Test

class ScaleStarterInitializerTest {
    @Test
    fun initRegistersJwAndLyScaleDrivers() {
        val manager = DeviceManager()

        ScaleStarterInitializer.init(manager)

        assertEquals(
            listOf(JWScaleDriver.STRATEGY_ID, LyScaleDriver.STRATEGY_ID),
            manager.registeredDrivers.map { it.descriptor.strategyId },
        )
    }
}
