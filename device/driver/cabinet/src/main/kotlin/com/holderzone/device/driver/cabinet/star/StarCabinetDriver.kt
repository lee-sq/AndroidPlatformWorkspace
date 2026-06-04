package com.holderzone.device.driver.cabinet.star

import android.content.Context
import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.ICloseableDevice
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.CommunicationMode
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
import com.holderzone.device.api.base.strategy.ISelfManagedProbeStrategy
import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.cabinet.capability.IPrintable
import com.holderzone.device.api.cabinet.capability.ITemperatureCtrl
import com.holderzone.device.api.cabinet.model.DoorAddress
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.PrintContent
import com.holderzone.device.api.cabinet.model.PrintResult
import com.holderzone.device.api.cabinet.model.PrinterStatus
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.driver.cabinet.print.TsplLabelRenderer
import com.xingx.lock.grid.GridLockConfig
import com.xingx.lock.grid.GridLockController
import com.xingx.lock.grid.listener.OnGridLocksListener
import com.xingx.lock.grid.listener.OnGridTempListener
import com.xingx.lock.grid.listener.OnWeightingListener
import com.xingx.print.gy.PrintController
import com.kongqw.serialportlibrary.command.Label
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:45
 * Description: Star 留样柜 driver，包装厂商 GridLockController/PrintController SDK。
 */
class StarCabinetDriver(
    private val context: Context,
    private val cabinetPortPath: String = DEFAULT_CABINET_PORT,
    private val printerPortPath: String = DEFAULT_PRINTER_PORT,
    private val printerBaudRate: Int = DEFAULT_PRINTER_BAUD_RATE,
) : IDeviceDriver {
    private val state = StarCabinetSdkState()
    private var matchedPortPath: String = cabinetPortPath

    override val descriptor: DriverDescriptor = DriverDescriptor(
        strategyId = STRATEGY_ID,
        vendorName = VENDOR_NAME,
        deviceModel = DEVICE_MODEL,
        deviceCategory = DeviceCategory.CABINET,
        communicationMode = CommunicationMode.PASSIVE_RESPONSE,
        supportedConfigs = listOf(SerialConfig(baudRate = DEFAULT_CABINET_BAUD_RATE)),
        preferredPortPaths = listOf(cabinetPortPath),
        capabilities = setOf(
            IWeighable::class,
            ILockable::class,
            IPrintable::class,
            ITemperatureCtrl::class,
        ),
        priority = 5,
        probeTimeoutMs = 3_000L,
        probeSettleDelayMs = 500L,
        selfManagedConnection = true,
    )

    override val probeStrategy: IProbeStrategy = StarCabinetProbeStrategy(
        context = context,
        state = state,
        cabinetPortPath = cabinetPortPath,
        printerPortPath = printerPortPath,
        printerBaudRate = printerBaudRate,
        onMatchedPort = { portPath -> matchedPortPath = portPath },
    )

    override val heartbeatProvider: IHeartbeatProvider? = null

    override val frameParser: IFrameParser = StarNoopFrameParser

    override val deviceFactory: IDeviceFactory = StarCabinetFactory(
        context = context,
        state = state,
        cabinetPortPath = { matchedPortPath },
        printerPortPath = printerPortPath,
        printerBaudRate = printerBaudRate,
    )

    companion object {
        const val STRATEGY_ID = "cabinet.star.sdk"
        const val VENDOR_NAME = "Star"
        const val DEVICE_MODEL = "Star Cabinet"
        const val DEFAULT_CABINET_PORT = "/dev/ttyS3"
        const val DEFAULT_CABINET_BAUD_RATE = 115_200
        const val DEFAULT_PRINTER_PORT = "/dev/ttyS0"
        const val DEFAULT_PRINTER_BAUD_RATE = 115_200
        const val DEFAULT_LABEL_WIDTH_MM = 50
        const val DEFAULT_LABEL_HEIGHT_MM = 50
    }
}

private class StarCabinetProbeStrategy(
    private val context: Context,
    private val state: StarCabinetSdkState,
    private val cabinetPortPath: String,
    private val printerPortPath: String,
    private val printerBaudRate: Int,
    private val onMatchedPort: (String) -> Unit,
) : ISelfManagedProbeStrategy {
    override fun buildProbeFrame(): ByteArray = ByteArray(0)

    override fun validateResponse(response: ByteArray): ProbeResult = ProbeResult.Mismatched

    override fun validateChannel(channel: SerialChannel): ProbeResult {
        return runCatching {
            val currentPortPath = channel.portPath.ifBlank { cabinetPortPath }
            state.start(
                context = context,
                cabinetPortPath = currentPortPath,
                cabinetBaudRate = StarCabinetDriver.DEFAULT_CABINET_BAUD_RATE,
                printerPortPath = printerPortPath,
                printerBaudRate = printerBaudRate,
                openPrinter = false,
            )
            GridLockController.instance().readWeightContinuous(true)
            if (!waitForSdkResponse()) {
                state.stopBlocking()
                return ProbeResult.Mismatched
            }
            onMatchedPort(currentPortPath)
            ProbeResult.Matched(deviceModel = StarCabinetDriver.DEVICE_MODEL)
        }.getOrElse { throwable ->
            ProbeResult.Error(
                reason = throwable.message ?: "Star SDK probe failed.",
                cause = throwable,
            )
        }
    }

    private fun waitForSdkResponse(): Boolean = runBlocking(Dispatchers.IO) {
        withTimeoutOrNull(PROBE_RESPONSE_TIMEOUT_MS) {
            while (true) {
                val statusMatched = runCatching {
                    val status = GridLockController.instance().queryStatus()
                    status.isNotEmpty() && status.keys.any { it > 0 }
                }.getOrDefault(false)
                if (statusMatched) return@withTimeoutOrNull true

                val weightMatched = runCatching {
                    state.hasWeightUpdate()
                }.getOrDefault(false)
                if (weightMatched) return@withTimeoutOrNull true

                delay(PROBE_RETRY_DELAY_MS)
            }
        } == true
    }

    companion object {
        private const val PROBE_RESPONSE_TIMEOUT_MS = 2_000L
        private const val PROBE_RETRY_DELAY_MS = 100L
    }
}

private object StarNoopFrameParser : IFrameParser {
    override fun extractFrame(raw: ByteArray): Frame? = null

    override fun parseFrame(frame: Frame): ParsedData? = null
}

private class StarCabinetFactory(
    private val context: Context,
    private val state: StarCabinetSdkState,
    private val cabinetPortPath: () -> String,
    private val printerPortPath: String,
    private val printerBaudRate: Int,
) : IDeviceFactory {
    override suspend fun initialize(channel: SerialChannel): Boolean {
        if (channel.isOpen) {
            channel.close()
        }
        state.start(
            context = context,
            cabinetPortPath = cabinetPortPath(),
            cabinetBaudRate = StarCabinetDriver.DEFAULT_CABINET_BAUD_RATE,
            printerPortPath = printerPortPath,
            printerBaudRate = printerBaudRate,
            openPrinter = true,
        )
        GridLockController.instance().readWeightContinuous(true)
        return true
    }

    override fun createDevice(channel: SerialChannel): IDevice {
        return StarCabinetDevice(
            portPath = channel.portPath,
            state = state,
        )
    }
}

private class StarCabinetDevice(
    portPath: String,
    private val state: StarCabinetSdkState,
) : ICloseableDevice,
    IWeighable,
    ILockable,
    IPrintable,
    ITemperatureCtrl {
    override val info: DeviceInfo = DeviceInfo(
        deviceId = "${StarCabinetDriver.STRATEGY_ID}:$portPath",
        strategyId = StarCabinetDriver.STRATEGY_ID,
        vendorName = StarCabinetDriver.VENDOR_NAME,
        deviceModel = StarCabinetDriver.DEVICE_MODEL,
        deviceCategory = DeviceCategory.CABINET,
    )

    override suspend fun close() {
        state.stop()
    }

    override suspend fun weigh(): WeightResult = state.latestWeight()

    override fun observeWeight(): Flow<WeightResult> = state.observeWeight()

    override suspend fun tare() {
        zero()
    }

    override suspend fun zero() {
        GridLockController.instance().peelZero()
    }

    override suspend fun unlock(address: DoorAddress): Boolean {
        if (address.isCompartment) return false
        GridLockController.instance().openLocks(null, address.primaryIndex + 1)
        return true
    }

    override suspend fun lock(address: DoorAddress): Boolean = false

    override suspend fun queryDoorState(address: DoorAddress): DoorState {
        if (address.isCompartment) return DoorState.UNKNOWN
        return state.latestDoorState(address.primaryIndex)
    }

    override suspend fun print(content: PrintContent): PrintResult {
        val label = TsplLabelRenderer.renderStarLabel(
            content = content,
            defaultWidthMm = StarCabinetDriver.DEFAULT_LABEL_WIDTH_MM,
            defaultHeightMm = StarCabinetDriver.DEFAULT_LABEL_HEIGHT_MM,
        )
        val result = state.printAndAwaitCompletion(label)
        return if (result.success) {
            PrintResult(success = true)
        } else {
            PrintResult(
                success = false,
                message = result.message ?: "Star 打印失败。",
            )
        }
    }

    override suspend fun getPrinterStatus(): PrinterStatus = state.queryPrinterStatus()

    override suspend fun setTemperature(celsius: Double): Boolean {
        return runCatching {
            GridLockController.instance().setTCConfigTemperature(celsius.toFloat(), null)
            true
        }.getOrDefault(false)
    }

    override suspend fun getTemperature(): TemperatureReading = state.latestTemperature()
}

private class StarCabinetSdkState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)
    private val weightUpdates = MutableSharedFlow<WeightResult>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val temperatureUpdates = MutableSharedFlow<TemperatureReading>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val latestWeight = MutableStateFlow<WeightResult?>(null)
    private val latestTemperature = MutableStateFlow<TemperatureReading?>(null)
    private val latestDoorStates = MutableStateFlow<Map<Int, DoorState>>(emptyMap())
    private val printerState = MutableStateFlow(PrinterStatus.UNKNOWN)

    private var tempListener: OnGridTempListener? = null
    private var doorListener: OnGridLocksListener? = null
    private var weightListener: OnWeightingListener? = null
    private var printerOpen: Boolean = false

    fun start(
        context: Context,
        cabinetPortPath: String,
        cabinetBaudRate: Int,
        printerPortPath: String,
        printerBaudRate: Int,
        openPrinter: Boolean,
    ) {
        if (!running.compareAndSet(false, true)) {
            if (openPrinter) {
                openPrinter(context, printerPortPath, printerBaudRate)
            }
            return
        }
        tempListener = object : OnGridTempListener {
            override fun onTemp(temp1: Float, temp2: Float) {
                val reading = TemperatureReading(celsius = temp1.toDouble())
                latestTemperature.value = reading
                temperatureUpdates.tryEmit(reading)
            }
        }
        doorListener = object : OnGridLocksListener {
            override fun onAllClose(allClose: Boolean, openLocks: MutableMap<Int, Boolean>) = Unit

            override fun onLockStatus(isChange: Boolean, lockStatus: MutableMap<Int, Boolean>) {
                if (!isChange) return
                latestDoorStates.value = lockStatus.mapKeys { entry -> entry.key - 1 }
                    .mapValues { entry ->
                        if (entry.value) DoorState.OPEN else DoorState.CLOSED
                    }
            }
        }
        weightListener = object : OnWeightingListener {
            override fun onWeighting(weight: Double) {
                val reading = WeightResult(
                    value = weight,
                    unit = WeightUnit.GRAM,
                )
                latestWeight.value = reading
                weightUpdates.tryEmit(reading)
            }

            override fun onError(msg: String) = Unit
        }

        val controller = GridLockController.instance()
        controller.config(
            GridLockConfig().apply {
                path = cabinetPortPath
                baudRate = cabinetBaudRate
            },
        )
        controller.setOnTempListener(tempListener)
        controller.setOnGridLocksListener(doorListener)
        controller.setOnWeightingListener(weightListener)
        controller.start()
        if (openPrinter) {
            openPrinter(context, printerPortPath, printerBaudRate)
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            runCatching {
                GridLockController.instance().setOnTempListener(null)
                GridLockController.instance().setOnGridLocksListener(null)
                GridLockController.instance().setOnWeightingListener(null)
                GridLockController.instance().readWeightContinuous(false)
                GridLockController.instance().stop()
            }
            runCatching {
                PrintController.instance().setOnDataListener(null)
                PrintController.instance().close()
            }
            tempListener = null
            doorListener = null
            weightListener = null
            printerOpen = false
            printerState.value = PrinterStatus.UNKNOWN
            running.set(false)
            scope.coroutineContext.cancelChildren()
        }
    }

    fun stopBlocking() {
        runBlocking {
            stop()
        }
    }

    fun hasWeightUpdate(): Boolean = latestWeight.value != null

    fun observeWeight(): Flow<WeightResult> = weightUpdates.asSharedFlow()

    suspend fun latestWeight(): WeightResult {
        latestWeight.value?.let { return it }
        val read = withTimeoutOrNull(SINGLE_READ_TIMEOUT_MS) {
            runCatching {
                GridLockController.instance().readWeightG()
            }.getOrNull()
        }
        if (read != null) {
            val result = WeightResult(
                value = read,
                unit = WeightUnit.GRAM,
            )
            latestWeight.value = result
            weightUpdates.tryEmit(result)
            return result
        }
        return withTimeoutOrNull(SINGLE_READ_TIMEOUT_MS) {
            latestWeight.filterNotNull().first()
        } ?: WeightResult(
            value = 0.0,
            unit = WeightUnit.GRAM,
            stable = false,
        )
    }

    suspend fun latestTemperature(): TemperatureReading {
        latestTemperature.value?.let { return it }
        return withTimeoutOrNull(SINGLE_READ_TIMEOUT_MS) {
            latestTemperature.filterNotNull().first()
        } ?: TemperatureReading(celsius = Double.NaN)
    }

    fun latestDoorState(index: Int): DoorState = latestDoorStates.value[index] ?: DoorState.UNKNOWN

    suspend fun printAndAwaitCompletion(label: Label): StarPrintResult {
        if (!printerOpen) {
            return StarPrintResult(success = false, message = "Star 打印串口未打开。")
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                PrintController.instance().sendCommandLabel(label)
                val status = awaitPrinterStatus(timeoutMs = PRINT_TIMEOUT_MS)
                when (status) {
                    PrinterStatus.READY -> StarPrintResult(success = true)
                    PrinterStatus.PAPER_OUT -> StarPrintResult(success = false, message = "Star 打印失败：缺纸。")
                    PrinterStatus.OFFLINE -> StarPrintResult(success = false, message = "Star 打印失败：离线。")
                    PrinterStatus.BUSY -> StarPrintResult(success = false, message = "Star 打印失败：忙碌。")
                    PrinterStatus.UNKNOWN -> StarPrintResult(success = false, message = "Star 打印状态未知或等待超时。")
                }
            }.getOrElse { throwable ->
                StarPrintResult(
                    success = false,
                    message = throwable.message ?: "Star 打印命令发送失败。",
                )
            }
        }
    }

    suspend fun queryPrinterStatus(): PrinterStatus {
        if (!printerOpen) return PrinterStatus.OFFLINE
        return withContext(Dispatchers.IO) {
            runCatching {
                printerState.value = PrinterStatus.UNKNOWN
                val query = Label().apply {
                    paper()
                }
                PrintController.instance().sendCommandLabel(query)
                awaitPrinterStatus(timeoutMs = PRINTER_STATUS_TIMEOUT_MS)
            }.getOrDefault(PrinterStatus.UNKNOWN)
        }
    }

    private fun openPrinter(context: Context, printerPortPath: String, printerBaudRate: Int) {
        val controller = PrintController.instance()
        controller.setOnDataListener(
            object : PrintController.OnDataListener {
                override fun onDataReceived(bytes: ByteArray) {
                    printerState.value = parsePrinterStatus(bytes)
                }
            },
        )
        printerOpen = controller.open(context, printerPortPath, printerBaudRate)
    }

    private suspend fun awaitPrinterStatus(timeoutMs: Long): PrinterStatus {
        printerState.value.takeIf { it != PrinterStatus.UNKNOWN }?.let { return it }
        return withTimeoutOrNull(timeoutMs) {
            printerState.filterNotNull().first { it != PrinterStatus.UNKNOWN }
        } ?: PrinterStatus.UNKNOWN
    }

    private fun parsePrinterStatus(bytes: ByteArray): PrinterStatus {
        if (bytes.size >= 3 && (bytes[0].toInt() and 0xFF) == 0xFE) {
            val code = bytes[1].toInt() and 0xFF
            val value = bytes[2].toInt() and 0xFF
            return when (code) {
                0x23 -> when (value) {
                    0x1A -> PrinterStatus.PAPER_OUT
                    0x12 -> PrinterStatus.READY
                    else -> PrinterStatus.UNKNOWN
                }
                0x24 -> when (value) {
                    0x10 -> PrinterStatus.PAPER_OUT
                    0x11 -> PrinterStatus.READY
                    else -> PrinterStatus.UNKNOWN
                }
                0x25,
                0x26,
                0x2B,
                -> if (value == 0x11) PrinterStatus.OFFLINE else PrinterStatus.UNKNOWN
                0x27 -> if (value == 0x11) PrinterStatus.OFFLINE else PrinterStatus.UNKNOWN
                0x28 -> if (value == 0x11) PrinterStatus.READY else PrinterStatus.OFFLINE
                else -> PrinterStatus.UNKNOWN
            }
        }
        if (bytes.size >= 3 && (bytes[0].toInt() and 0xFF) == 0xFC) {
            val first = bytes[1].toInt() and 0xFF
            val second = bytes[2].toInt() and 0xFF
            return when {
                first == 0x4F && second == 0x4B -> PrinterStatus.READY
                first == 0x6E && second == 0x6F -> PrinterStatus.BUSY
                else -> PrinterStatus.UNKNOWN
            }
        }
        return PrinterStatus.UNKNOWN
    }

    companion object {
        private const val SINGLE_READ_TIMEOUT_MS = 1_000L
        private const val PRINT_TIMEOUT_MS = 10_000L
        private const val PRINTER_STATUS_TIMEOUT_MS = 1_000L
    }
}

private data class StarPrintResult(
    val success: Boolean,
    val message: String? = null,
)
