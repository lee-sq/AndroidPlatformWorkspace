package com.holderzone.device.driver.scale.jw

import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.scale.jw.protocol.JWScale3568Protocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JWScaleDriverTest {
    @Test
    fun bindCreatesWeighableDeviceAndWritesInitializationCommand() = runTest {
        val driver = JWScaleDriver()
        val channel = InMemorySerialChannel(
            portPath = JWScaleDriver.DEFAULT_PORT_PATH.first(),
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager()

        val device = manager.bindSession(driver, channel)

        assertEquals("${JWScaleDriver.STRATEGY_ID}:${JWScaleDriver.DEFAULT_PORT_PATH.first()}", device.info.deviceId)
        assertNotNull(manager.queryCapability<IWeighable>(device.info.deviceId))
        assertEquals(
            "01030100001045FA",
            JWScale3568Protocol.bytesToHex(channel.writes.single()),
        )
    }

    @Test
    fun zeroAndTareWriteControlCommands() = runTest {
        val driver = JWScaleDriver()
        val channel = InMemorySerialChannel(
            portPath = JWScaleDriver.DEFAULT_PORT_PATH.first(),
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager()
        val device = manager.bindSession(driver, channel)
        val weighable = manager.queryCapability<IWeighable>(device.info.deviceId)!!

        weighable.zero()
        weighable.tare()

        assertEquals(
            listOf(
                "01030100001045FA",
                "0110000000020400010000A26F",
                "0110000000020400000001326F",
            ),
            channel.writes.map(JWScale3568Protocol::bytesToHex),
        )
    }

    @Test
    fun descriptorUses3568SerialDefaults() {
        val descriptor = JWScaleDriver().descriptor

        assertEquals("scale.jingwei", descriptor.strategyId)
        assertEquals(listOf("/dev/ttyS7", "/dev/ttyS2"), descriptor.preferredPortPaths)
        assertEquals(19_200, descriptor.supportedConfigs.single().baudRate)
        assertEquals(10, descriptor.priority)
    }
}
