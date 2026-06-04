package com.holderzone.device.driver.cabinet.jw.serial

import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.cabinet.capability.IPrintable
import com.holderzone.device.api.cabinet.capability.ITemperatureCtrl
import com.holderzone.device.api.scale.capability.ICalibratable
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.cabinet.jw.serial.protocol.JwCabinetProtocol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JwSerialCabinetDriverTest {
    @Test
    fun bindCreatesCabinetCapabilitiesAndWritesInitialProbe() = runTest {
        val driver = JwSerialCabinetDriver()
        val channel = InMemorySerialChannel(
            portPath = JwCabinetProtocol.DEFAULT_CABINET_PORT,
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager(coroutineScope = this)

        val device = manager.bindSession(driver, channel)

        assertEquals("${JwSerialCabinetDriver.STRATEGY_ID}:${JwCabinetProtocol.DEFAULT_CABINET_PORT}", device.info.deviceId)
        assertNotNull(manager.queryCapability<IWeighable>(device.info.deviceId))
        assertNotNull(manager.queryCapability<ICalibratable>(device.info.deviceId))
        assertNotNull(manager.queryCapability<ILockable>(device.info.deviceId))
        assertNotNull(manager.queryCapability<IPrintable>(device.info.deviceId))
        assertNotNull(manager.queryCapability<ITemperatureCtrl>(device.info.deviceId))
        assertEquals(
            "020300000012C5F4",
            JwCabinetProtocol.bytesToHex(channel.writes.first()),
        )

        manager.clear()
        advanceUntilIdle()
    }

    @Test
    fun descriptorUsesJwCabinetDefaults() {
        val descriptor = JwSerialCabinetDriver().descriptor

        assertEquals("cabinet.jw.serial", descriptor.strategyId)
        assertEquals(listOf("/dev/ttyS2"), descriptor.preferredPortPaths)
        assertEquals(9_600, descriptor.supportedConfigs.single().baudRate)
        assertEquals(10, descriptor.priority)
        assertEquals(2_000L, descriptor.probeTimeoutMs)
        assertEquals(500L, descriptor.probeSettleDelayMs)
    }

    @Test
    fun calibrateReturnsCalculatedSlope() = runTest {
        val driver = JwSerialCabinetDriver()
        val channel = InMemorySerialChannel(
            portPath = JwCabinetProtocol.DEFAULT_CABINET_PORT,
            config = driver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager(coroutineScope = this)
        val device = manager.bindSession(driver, channel)
        val weightFrame = JwCabinetProtocol.hexToBytes("020312000000000000000200010005000100000000AF59")

        driver.frameParser.parseFrame(Frame(payload = weightFrame, raw = weightFrame))
        val result = manager.queryCapability<ICalibratable>(device.info.deviceId)!!.calibrate(6_000.0)

        assertEquals(true, result.success)
        assertEquals(2.0, result.slope!!, 0.0001)
        assertEquals(3_000.0, result.rawWeightGrams!!, 0.0001)
        assertEquals(2_000.0, result.zeroOffsetGrams!!, 0.0001)
        assertEquals(6_000.0, result.standardWeightGrams, 0.0001)

        manager.clear()
        advanceUntilIdle()
    }

    @Test
    fun calibrationSlopeIsSavedAndRestoredBySdkStore() = runTest {
        val store = InMemoryJwCalibrationStore()
        val firstDriver = JwSerialCabinetDriver(calibrationStore = store)
        val firstChannel = InMemorySerialChannel(
            portPath = JwCabinetProtocol.DEFAULT_CABINET_PORT,
            config = firstDriver.descriptor.supportedConfigs.first(),
        )
        val manager = DeviceManager(coroutineScope = this)
        val firstDevice = manager.bindSession(firstDriver, firstChannel)
        val weightFrame = JwCabinetProtocol.hexToBytes("020312000000000000000200010005000100000000AF59")

        firstDriver.frameParser.parseFrame(Frame(payload = weightFrame, raw = weightFrame))
        val calibration = manager.queryCapability<ICalibratable>(firstDevice.info.deviceId)!!.calibrate(6_000.0)

        assertEquals(2.0, calibration.slope!!, 0.0001)
        assertEquals(2.0, store.readSlope(JwCabinetProtocol.DEFAULT_CABINET_PORT)!!, 0.0001)
        manager.clear()
        advanceUntilIdle()

        val secondDriver = JwSerialCabinetDriver(calibrationStore = store)
        val secondChannel = InMemorySerialChannel(
            portPath = JwCabinetProtocol.DEFAULT_CABINET_PORT,
            config = secondDriver.descriptor.supportedConfigs.first(),
        )
        val secondDevice = manager.bindSession(secondDriver, secondChannel)
        secondDriver.frameParser.parseFrame(Frame(payload = weightFrame, raw = weightFrame))

        assertEquals(6_000.0, manager.queryCapability<IWeighable>(secondDevice.info.deviceId)!!.weigh().value, 0.0001)

        manager.clear()
        advanceUntilIdle()
    }

    private class InMemoryJwCalibrationStore : JwCalibrationStore {
        private val slopes = mutableMapOf<String, Double>()

        override fun readSlope(portPath: String): Double? = slopes[portPath]

        override fun saveSlope(portPath: String, slope: Double) {
            slopes[portPath] = slope
        }
    }
}
