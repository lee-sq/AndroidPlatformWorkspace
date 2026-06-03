package com.holderzone.device.core

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.CommunicationMode
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.model.DeviceCategory
import com.holderzone.device.api.base.model.DeviceInfo
import com.holderzone.device.api.base.model.DriverDescriptor
import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ParsedData
import com.holderzone.device.api.base.model.Parity
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.api.base.strategy.IDeviceFactory
import com.holderzone.device.api.base.strategy.IFrameParser
import com.holderzone.device.api.base.strategy.IHeartbeatProvider
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.device.DeviceConnectionEvent
import com.holderzone.device.core.device.DeviceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceManagerTest {
    @Test
    fun registeredDriversAreVisible() {
        val manager = DeviceManager()
        val driver = FakeScaleDriver()

        manager.registerDriver(driver)

        assertEquals(listOf(driver), manager.registeredDrivers)
    }

    @Test
    fun queryCapabilityReturnsImplementedCapability() = runTest {
        val manager = DeviceManager()
        val driver = FakeScaleDriver()
        val device = manager.bindDevice(driver, fakeChannel())

        val weighable = manager.queryCapability<IWeighable>(device.info.deviceId)
        assertNotNull(weighable)
        assertEquals(42.0, weighable!!.weigh().value, 0.0)
        assertEquals(42.0, weighable.weigh().grams, 0.0)
    }

    @Test
    fun queryCapabilityDoesNotReturnUnsupportedCapability() = runTest {
        val manager = DeviceManager()
        val driver = FakeScaleDriver()
        val device = manager.bindDevice(driver, fakeChannel())

        assertNull(manager.queryCapability<ILockable>(device.info.deviceId))
    }

    @Test
    fun devicesFlowPublishesDeviceSnapshots() = runTest {
        val manager = DeviceManager()
        val driver = FakeScaleDriver()

        assertEquals(emptyList<IDevice>(), manager.devices.value)
        assertEquals(0, manager.deviceRecords.value.size)

        val device = manager.bindDevice(driver, fakeChannel())

        assertEquals(listOf(device), manager.devices.value)
        assertEquals(ConnectionState.CONNECTED, manager.deviceRecords.value.single().state)

        manager.updateState(device.info.deviceId, ConnectionState.DEGRADED)

        assertEquals(ConnectionState.DEGRADED, manager.deviceRecords.value.single().state)

        manager.unbindDevice(device.info.deviceId)

        assertEquals(emptyList<IDevice>(), manager.devices.value)
        assertEquals(0, manager.deviceRecords.value.size)
    }

    @Test
    fun connectionEventsPublishBindStateAndUnbindChanges() = runTest {
        val manager = DeviceManager()
        val driver = FakeScaleDriver()
        val events = mutableListOf<DeviceConnectionEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.connectionEvents.collect(events::add)
        }

        val device = manager.bindDevice(driver, fakeChannel())
        manager.updateState(device.info.deviceId, ConnectionState.DEGRADED)
        manager.unbindDevice(device.info.deviceId)

        assertEquals(3, events.size)
        assertEquals(device.info.deviceId, (events[0] as DeviceConnectionEvent.Bound).record.device.info.deviceId)
        assertEquals(ConnectionState.CONNECTED, (events[1] as DeviceConnectionEvent.StateChanged).previousState)
        assertEquals(ConnectionState.DEGRADED, (events[1] as DeviceConnectionEvent.StateChanged).state)
        assertEquals(device.info.deviceId, (events[2] as DeviceConnectionEvent.Unbound).deviceId)

        collectJob.cancel()
    }

    private fun fakeChannel(): SerialChannel = InMemorySerialChannel(
        portPath = "/dev/ttyS0",
        config = SerialConfig(baudRate = 9_600, parity = Parity.NONE),
    )

    private class FakeScaleDevice : IDevice, IWeighable {
        override val info = DeviceInfo(
            deviceId = "fake-scale",
            strategyId = "scale.fake",
            vendorName = "HolderZone",
            deviceModel = "FakeScale",
            deviceCategory = DeviceCategory.SCALE,
        )

        override suspend fun weigh(): WeightResult = WeightResult(
            value = 42.0,
            unit = WeightUnit.GRAM,
        )

        override fun observeWeight(): Flow<WeightResult> = flowOf(
            WeightResult(
                value = 42.0,
                unit = WeightUnit.GRAM,
            ),
        )

        override suspend fun tare() = Unit

        override suspend fun zero() = Unit
    }

    private class FakeScaleDriver : IDeviceDriver {
        override val descriptor = DriverDescriptor(
            strategyId = "scale.fake",
            vendorName = "HolderZone",
            deviceModel = "FakeScale",
            deviceCategory = DeviceCategory.SCALE,
            communicationMode = CommunicationMode.PASSIVE_RESPONSE,
            supportedConfigs = listOf(SerialConfig(baudRate = 9_600, parity = Parity.NONE)),
            capabilities = setOf(IWeighable::class),
        )

        override val probeStrategy = object : IProbeStrategy {
            override fun buildProbeFrame(): ByteArray = byteArrayOf(0x01)

            override fun validateResponse(response: ByteArray): ProbeResult = ProbeResult.Matched(
                deviceModel = "FakeScale",
            )
        }

        override val heartbeatProvider: IHeartbeatProvider? = null

        override val frameParser = object : IFrameParser {
            override fun extractFrame(raw: ByteArray): Frame? = Frame(payload = raw)

            override fun parseFrame(frame: Frame): ParsedData? = ParsedData(type = "fake")
        }

        override val deviceFactory = object : IDeviceFactory {
            override suspend fun initialize(channel: SerialChannel): Boolean = true

            override fun createDevice(channel: SerialChannel): IDevice = FakeScaleDevice()
        }
    }
}
