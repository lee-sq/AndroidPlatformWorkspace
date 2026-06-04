package com.holderzone.device.driver.scale.ly

import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.scale.ly.protocol.LyScaleProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LyScaleDriverTest {
    @Test
    fun bindCreatesWeighableDeviceWithoutInitializationCommand() = runTest {
        val driver = LyScaleDriver()
        val channel = InMemorySerialChannel(
            portPath = LyScaleDriver.DEFAULT_PORT_PATHS.first(),
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager()

        val device = manager.bindSession(driver, channel)

        assertEquals("${LyScaleDriver.STRATEGY_ID}:${LyScaleDriver.DEFAULT_PORT_PATHS.first()}", device.info.deviceId)
        assertNotNull(manager.queryCapability<IWeighable>(device.info.deviceId))
        assertEquals(emptyList<ByteArray>(), channel.writes)
    }

    @Test
    fun zeroAndTareWriteLyAsciiCommands() = runTest {
        val driver = LyScaleDriver()
        val channel = InMemorySerialChannel(
            portPath = LyScaleDriver.DEFAULT_PORT_PATHS.first(),
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager()
        val device = manager.bindSession(driver, channel)
        val weighable = manager.queryCapability<IWeighable>(device.info.deviceId)!!

        weighable.tare()
        weighable.zero()

        assertEquals(
            listOf("54", "5A"),
            channel.writes.map(LyScaleProtocol::bytesToHex),
        )
    }

    @Test
    fun descriptorUses9600SerialDefaults() {
        val descriptor = LyScaleDriver().descriptor

        assertEquals("scale.liangyue.ascii", descriptor.strategyId)
        assertEquals(listOf("/dev/ttyS4", "/dev/ttyS0"), descriptor.preferredPortPaths)
        assertEquals(9_600, descriptor.supportedConfigs.single().baudRate)
        assertEquals(20, descriptor.priority)
    }
}
