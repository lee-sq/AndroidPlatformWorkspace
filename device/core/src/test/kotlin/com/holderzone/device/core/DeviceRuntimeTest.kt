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
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.api.base.strategy.IDeviceFactory
import com.holderzone.device.api.base.strategy.IFrameParser
import com.holderzone.device.api.base.strategy.IHeartbeatProvider
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.FrameStreamReader
import com.holderzone.device.core.channel.InMemorySerialBackend
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.channel.SerialPortInfo
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.device.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRuntimeTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startAutoSniffingBindsMatchedDevice() = runTest(dispatcher) {
        val backend = InMemorySerialBackend(
            ports = listOf(SerialPortInfo(path = "/dev/ttyS0")),
            channelFactory = { portPath, config ->
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                    readBuffer = ArrayDeque(listOf("MATCH".encodeToByteArray())),
                )
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = TestScope(dispatcher),
        )
        manager.registerDriver(RuntimeTestDriver())

        manager.startAutoSniffing()
        runCurrent()

        val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0"
        assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
        assertNotNull(manager.queryCapability<IWeighable>(deviceId))
        manager.clear()
        runCurrent()
    }

    @Test
    fun startAutoSniffingTriesPreferredPortsBeforeOtherPorts() = runTest(dispatcher) {
        val openedPorts = mutableListOf<String>()
        val backend = InMemorySerialBackend(
            ports = listOf(
                SerialPortInfo(path = "/dev/ttyS0"),
                SerialPortInfo(path = "/dev/ttyS7"),
            ),
            channelFactory = { portPath, config ->
                openedPorts += portPath
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                    readBuffer = ArrayDeque(listOf("MATCH".encodeToByteArray())),
                )
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = TestScope(dispatcher),
        )
        manager.registerDriver(RuntimeTestDriver(preferredPortPaths = listOf("/dev/ttyS7")))

        manager.startAutoSniffing()
        runCurrent()

        assertEquals("/dev/ttyS7", openedPorts.first())
        manager.clear()
        runCurrent()
    }

    @Test
    fun frameStreamReaderExtractsHalfAndStickyPackets() {
        val parser = RuntimeFrameParser()
        val reader = FrameStreamReader(parser)

        assertEquals(emptyList<Frame>(), reader.append("12".encodeToByteArray()))

        val frames = reader.append("|34|".encodeToByteArray())

        assertEquals(2, frames.size)
        assertEquals("12|".encodeToByteArray().toList(), frames[0].raw.toList())
        assertEquals("34|".encodeToByteArray().toList(), frames[1].raw.toList())
    }

    private class RuntimeTestDriver(
        private val preferredPortPaths: List<String> = emptyList(),
    ) : IDeviceDriver {
        val parser = RuntimeFrameParser()

        override val descriptor = DriverDescriptor(
            strategyId = STRATEGY_ID,
            vendorName = "HolderZone",
            deviceModel = "RuntimeTest",
            deviceCategory = DeviceCategory.SCALE,
            communicationMode = CommunicationMode.PASSIVE_RESPONSE,
            supportedConfigs = listOf(SerialConfig(baudRate = 9_600)),
            preferredPortPaths = preferredPortPaths,
            capabilities = setOf(IWeighable::class),
        )

        override val probeStrategy = object : IProbeStrategy {
            override fun buildProbeFrame(): ByteArray = "PROBE".encodeToByteArray()

            override fun validateResponse(response: ByteArray): ProbeResult {
                return if (response.decodeToString() == "MATCH") {
                    ProbeResult.Matched(deviceModel = "RuntimeTest")
                } else {
                    ProbeResult.Mismatched
                }
            }
        }

        override val heartbeatProvider: IHeartbeatProvider? = null

        override val frameParser: IFrameParser = parser

        override val deviceFactory = object : IDeviceFactory {
            override suspend fun initialize(channel: SerialChannel): Boolean = true

            override fun createDevice(channel: SerialChannel): IDevice = RuntimeTestDevice(channel.portPath)
        }

        companion object {
            const val STRATEGY_ID = "scale.runtime-test"
        }
    }

    private class RuntimeFrameParser : IFrameParser {
        val parsedFrames = mutableListOf<Frame>()

        override fun extractFrame(raw: ByteArray): Frame? {
            val index = raw.indexOf('|'.code.toByte())
            if (index < 0) return null
            return Frame(
                payload = raw.copyOfRange(0, index),
                raw = raw.copyOfRange(0, index + 1),
            )
        }

        override fun parseFrame(frame: Frame): ParsedData {
            parsedFrames += frame
            return ParsedData(type = "runtime-test")
        }
    }

    private class RuntimeTestDevice(
        portPath: String,
    ) : IDevice, IWeighable {
        override val info = DeviceInfo(
            deviceId = "${RuntimeTestDriver.STRATEGY_ID}:$portPath",
            strategyId = RuntimeTestDriver.STRATEGY_ID,
            vendorName = "HolderZone",
            deviceModel = "RuntimeTest",
            deviceCategory = DeviceCategory.SCALE,
        )

        override suspend fun weigh(): WeightResult = WeightResult(
            value = 1.0,
            unit = WeightUnit.GRAM,
        )

        override fun observeWeight(): Flow<WeightResult> = flowOf(
            WeightResult(
                value = 1.0,
                unit = WeightUnit.GRAM,
            ),
        )

        override suspend fun tare() = Unit

        override suspend fun zero() = Unit
    }
}
