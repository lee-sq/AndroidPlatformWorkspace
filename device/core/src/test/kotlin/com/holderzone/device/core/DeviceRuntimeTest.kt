package com.holderzone.device.core

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.logging.DeviceLogEntry
import com.holderzone.device.api.base.logging.DeviceLogLevel
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
import com.holderzone.device.api.base.strategy.IPollingProvider
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.api.base.strategy.PollingCommand
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.FrameStreamReader
import com.holderzone.device.core.channel.SerialBackend
import com.holderzone.device.core.channel.InMemorySerialBackend
import com.holderzone.device.core.channel.InMemorySerialChannel
import com.holderzone.device.core.channel.SerialPortInfo
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.device.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
            coroutineScope = this,
        )
        manager.registerDriver(RuntimeTestDriver())

        manager.startAutoSniffing()
        runCurrent()

        val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0"
        assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
        assertNotNull(manager.queryCapability<IWeighable>(deviceId))
        manager.clear()
        advanceUntilIdle()
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
            coroutineScope = this,
        )
        manager.registerDriver(RuntimeTestDriver(preferredPortPaths = listOf("/dev/ttyS7")))

        manager.startAutoSniffing()
        runCurrent()

        assertEquals("/dev/ttyS7", openedPorts.first())
        manager.clear()
        advanceUntilIdle()
    }

    @Test
    fun startAutoSniffingTriesPreferredPortsEvenWhenListPortsMissesThem() = runTest(dispatcher) {
        val openedPorts = mutableListOf<String>()
        val backend = InMemorySerialBackend(
            ports = listOf(SerialPortInfo(path = "/dev/ttyS0")),
            channelFactory = { portPath, config ->
                openedPorts += portPath
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                    readBuffer = ArrayDeque(
                        if (portPath == "/dev/ttyS2") {
                            listOf("MATCH".encodeToByteArray())
                        } else {
                            listOf("MISS".encodeToByteArray())
                        },
                    ),
                )
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = this,
        )
        manager.registerDriver(RuntimeTestDriver(preferredPortPaths = listOf("/dev/ttyS2")))

        manager.startAutoSniffing()
        runCurrent()

        val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS2"
        assertEquals("/dev/ttyS2", openedPorts.first())
        assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
        manager.clear()
        advanceUntilIdle()
    }

    @Test
    fun startAutoSniffingPublishesLifecycleLogsWhenInfoIsEnabled() = runTest(dispatcher) {
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
            coroutineScope = this,
        )
        val logs = mutableListOf<DeviceLogEntry>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.logs.collect(logs::add)
        }
        manager.minLogLevel = DeviceLogLevel.INFO
        manager.registerDriver(RuntimeTestDriver())

        manager.startAutoSniffing()
        runCurrent()

        assertEquals(
            listOf(
                "开始自动探测设备。",
                "自动探测命中设备：${RuntimeTestDriver.STRATEGY_ID}，端口：/dev/ttyS0。",
                "设备已绑定：${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0",
            ),
            logs.map { it.message },
        )

        collectJob.cancel()
        manager.clear()
        advanceUntilIdle()
    }

    @Test
    fun startAutoSniffingWaitsForDelayedActiveReportWithinProbeWindow() = runTest(dispatcher) {
        val backend = InMemorySerialBackend(
            ports = listOf(SerialPortInfo(path = "/dev/ttyS0")),
            channelFactory = { portPath, config ->
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                    readBuffer = ArrayDeque(
                        listOf(
                            ByteArray(0),
                            ByteArray(0),
                            "MATCH".encodeToByteArray(),
                        ),
                    ),
                )
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = this,
        )
        manager.registerDriver(
            RuntimeTestDriver(
                communicationMode = CommunicationMode.ACTIVE_REPORT,
                probeTimeoutMs = 300L,
            ),
        )

        try {
            manager.startAutoSniffing()
            advanceTimeBy(25L)
            runCurrent()

            val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0"
            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun startAutoSniffingHonorsProbeSettleDelayBeforeReadingReport() = runTest(dispatcher) {
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
            coroutineScope = this,
        )
        manager.registerDriver(
            RuntimeTestDriver(
                probeSettleDelayMs = 300L,
            ),
        )

        try {
            manager.startAutoSniffing()
            runCurrent()

            val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0"
            assertEquals(ConnectionState.DISCONNECTED, manager.observeDevice(deviceId).value)

            advanceTimeBy(299L)
            runCurrent()
            assertEquals(ConnectionState.DISCONNECTED, manager.observeDevice(deviceId).value)

            advanceTimeBy(1L)
            runCurrent()
            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun startAutoSniffingBindsSelfManagedDriverWithoutReadingSerialBytes() = runTest(dispatcher) {
        val openedChannels = mutableListOf<InMemorySerialChannel>()
        val backend = InMemorySerialBackend(
            ports = listOf(SerialPortInfo(path = "/dev/ttyS0")),
            channelFactory = { portPath, config ->
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                ).also(openedChannels::add)
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = this,
        )
        manager.registerDriver(
            RuntimeTestDriver(
                selfManagedConnection = true,
            ),
        )

        try {
            manager.startAutoSniffing()
            runCurrent()

            val deviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS0"
            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(deviceId).value)
            assertEquals(emptyList<InMemorySerialChannel>(), openedChannels)
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun pollingDriverWritesCommandsFromCoreAndParsesResponses() = runTest(dispatcher) {
        val driver = RuntimeTestDriver(
            pollingCommands = listOf(
                PollingCommand(
                    payload = "POLL_WEIGHT".encodeToByteArray(),
                    intervalMs = 100L,
                ),
                PollingCommand(
                    payload = "POLL_DOOR".encodeToByteArray(),
                    intervalMs = 100L,
                ),
            ),
        )
        val manager = DeviceManager(coroutineScope = this)
        val channel = InMemorySerialChannel(
            portPath = "/dev/ttyS2",
            config = SerialConfig(baudRate = 9_600),
            readBuffer = ArrayDeque(listOf("DATA|".encodeToByteArray())),
        )

        try {
            val device = manager.bindSession(driver, channel)
            runCurrent()

            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(device.info.deviceId).value)
            assertEquals(1, driver.parser.parsedFrames.size)
            assertEquals(listOf("POLL_WEIGHT"), channel.writes.map { it.decodeToString() })

            advanceTimeBy(100L)
            runCurrent()

            assertEquals(
                listOf("POLL_WEIGHT", "POLL_DOOR"),
                channel.writes.map { it.decodeToString() },
            )
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun reconnectTriesOriginalPortBeforeFullSniff() = runTest(dispatcher) {
        val openedPorts = mutableListOf<String>()
        val backend = InMemorySerialBackend(
            ports = listOf(SerialPortInfo(path = "/dev/ttyS9")),
            channelFactory = { portPath, config ->
                openedPorts += portPath
                InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                )
            },
        )
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = this,
        )
        val channel = FailingReadSerialChannel(
            portPath = "/dev/ttyS2",
            config = SerialConfig(baudRate = 9_600),
        )

        try {
            val device = manager.bindSession(RuntimeTestDriver(), channel)
            runCurrent()

            assertEquals(ConnectionState.RECONNECTING, manager.observeDevice(device.info.deviceId).value)

            advanceTimeBy(1_000L)
            runCurrent()

            assertEquals(listOf("/dev/ttyS2"), openedPorts)
            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(device.info.deviceId).value)
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun reconnectFallsBackToFullSniffWhenOriginalPortFails() = runTest(dispatcher) {
        val openedPorts = mutableListOf<String>()
        val backend = object : SerialBackend {
            override fun listPorts(): List<SerialPortInfo> = listOf(SerialPortInfo(path = "/dev/ttyS1"))

            override fun createChannel(portPath: String, config: SerialConfig): SerialChannel {
                openedPorts += portPath
                if (portPath == "/dev/ttyS2") {
                    error("Original port unavailable.")
                }
                return InMemorySerialChannel(
                    portPath = portPath,
                    config = config,
                    readBuffer = ArrayDeque(listOf("MATCH".encodeToByteArray())),
                )
            }
        }
        val manager = DeviceManager(
            serialPortManager = SerialPortManager(backend),
            coroutineScope = this,
        )
        val driver = RuntimeTestDriver()
        manager.registerDriver(driver)
        val channel = FailingReadSerialChannel(
            portPath = "/dev/ttyS2",
            config = SerialConfig(baudRate = 9_600),
        )

        try {
            val oldDevice = manager.bindSession(driver, channel)
            runCurrent()

            assertEquals(ConnectionState.RECONNECTING, manager.observeDevice(oldDevice.info.deviceId).value)

            advanceTimeBy(1_000L)
            runCurrent()

            val nextDeviceId = "${RuntimeTestDriver.STRATEGY_ID}:/dev/ttyS1"
            assertEquals(listOf("/dev/ttyS2", "/dev/ttyS1"), openedPorts)
            assertEquals(ConnectionState.CONNECTED, manager.observeDevice(nextDeviceId).value)
        } finally {
            manager.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun readLoopFailurePublishesErrorAndWarnLogsWithDefaultMinimumLevel() = runTest(dispatcher) {
        val manager = DeviceManager(
            coroutineScope = this,
        )
        val logs = mutableListOf<DeviceLogEntry>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.logs.collect(logs::add)
        }
        val channel = FailingReadSerialChannel(
            portPath = "/dev/ttyS0",
            config = SerialConfig(baudRate = 9_600),
        )

        val device = manager.bindSession(RuntimeTestDriver(), channel)
        runCurrent()

        assertEquals(ConnectionState.RECONNECTING, manager.observeDevice(device.info.deviceId).value)
        assertEquals(
            listOf(DeviceLogLevel.ERROR, DeviceLogLevel.WARN, DeviceLogLevel.WARN, DeviceLogLevel.WARN),
            logs.map { it.level },
        )
        assertEquals(
            listOf(
                "设备读循环异常：${device.info.deviceId}",
                "设备状态变更：${device.info.deviceId}，CONNECTED -> DEGRADED",
                "设备已安排重连：${device.info.deviceId}",
                "设备状态变更：${device.info.deviceId}，DEGRADED -> RECONNECTING",
            ),
            logs.map { it.message },
        )
        assertEquals("Read failed.", logs.first().throwable?.message)

        collectJob.cancel()
        manager.clear()
        advanceUntilIdle()
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
        private val communicationMode: CommunicationMode = CommunicationMode.PASSIVE_RESPONSE,
        private val probeTimeoutMs: Long? = null,
        private val probeSettleDelayMs: Long = 0L,
        private val selfManagedConnection: Boolean = false,
        override val pollingCommands: List<PollingCommand> = emptyList(),
    ) : IDeviceDriver,
        IPollingProvider {
        val parser = RuntimeFrameParser()

        override val descriptor = DriverDescriptor(
            strategyId = STRATEGY_ID,
            vendorName = "HolderZone",
            deviceModel = "RuntimeTest",
            deviceCategory = DeviceCategory.SCALE,
            communicationMode = communicationMode,
            supportedConfigs = listOf(SerialConfig(baudRate = 9_600)),
            preferredPortPaths = preferredPortPaths,
            probeTimeoutMs = probeTimeoutMs,
            probeSettleDelayMs = probeSettleDelayMs,
            selfManagedConnection = selfManagedConnection,
            capabilities = setOf(IWeighable::class),
        )

        override val probeStrategy = object : IProbeStrategy {
            override fun buildProbeFrame(): ByteArray = "PROBE".encodeToByteArray()

            override fun validateResponse(response: ByteArray): ProbeResult {
                return if (selfManagedConnection || response.decodeToString() == "MATCH") {
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

    private class FailingReadSerialChannel(
        override val portPath: String,
        override val config: SerialConfig,
    ) : SerialChannel {
        override var isOpen: Boolean = false
            private set

        override suspend fun open() {
            isOpen = true
        }

        override suspend fun close() {
            isOpen = false
        }

        override suspend fun write(data: ByteArray) = Unit

        override suspend fun read(timeoutMs: Long): ByteArray {
            error("Read failed.")
        }
    }
}
