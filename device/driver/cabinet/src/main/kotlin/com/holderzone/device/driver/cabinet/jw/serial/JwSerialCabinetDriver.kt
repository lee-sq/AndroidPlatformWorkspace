package com.holderzone.device.driver.cabinet.jw.serial

import android.content.Context
import android.content.SharedPreferences
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
import com.holderzone.device.api.base.strategy.IPollingProvider
import com.holderzone.device.api.base.strategy.PollingCommand
import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.cabinet.capability.IPrintable
import com.holderzone.device.api.cabinet.capability.ITemperatureCtrl
import com.holderzone.device.api.cabinet.model.DoorAddress
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.PrintContent
import com.holderzone.device.api.cabinet.model.PrintResult
import com.holderzone.device.api.cabinet.model.PrinterStatus
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.capability.ICalibratable
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.CalibrationResult
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.driver.cabinet.jw.serial.protocol.JwCabinetFrameParser
import com.holderzone.device.driver.cabinet.jw.serial.protocol.JwCabinetProtocol
import com.holderzone.device.driver.cabinet.print.SerialPrinterClient
import com.holderzone.device.driver.cabinet.print.TsplLabelRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:25
 * Description: 基于 JW 直接串口协议的留样柜 driver，聚合称重、门锁、打印和温控能力。
 */
class JwSerialCabinetDriver(
    context: Context? = null,
    doorCount: Int = JwCabinetProtocol.DEFAULT_DOOR_COUNT,
    private val printerPortPath: String = JwCabinetProtocol.DEFAULT_PRINTER_PORT,
    private val printerBaudRate: Int = JwCabinetProtocol.DEFAULT_PRINTER_BAUD_RATE,
    private val protocol: JwCabinetProtocol = JwCabinetProtocol,
    private val parser: JwCabinetFrameParser = JwCabinetFrameParser(
        doorCount = doorCount,
        protocol = protocol,
    ),
    private val calibrationStore: JwCalibrationStore = context?.applicationContext
        ?.let(::SharedPreferencesJwCalibrationStore)
        ?: NoopJwCalibrationStore,
) : IDeviceDriver,
    IPollingProvider {
    private val factory = JwSerialCabinetFactory(
        parser = parser,
        protocol = protocol,
        doorCount = doorCount,
        printerPortPath = printerPortPath,
        printerBaudRate = printerBaudRate,
        calibrationStore = calibrationStore,
    )

    override val descriptor: DriverDescriptor = DriverDescriptor(
        strategyId = STRATEGY_ID,
        vendorName = VENDOR_NAME,
        deviceModel = DEVICE_MODEL,
        deviceCategory = DeviceCategory.CABINET,
        communicationMode = CommunicationMode.PASSIVE_RESPONSE,
        supportedConfigs = listOf(SerialConfig(baudRate = JwCabinetProtocol.DEFAULT_BAUD_RATE)),
        preferredPortPaths = listOf(JwCabinetProtocol.DEFAULT_CABINET_PORT),
        capabilities = setOf(
            IWeighable::class,
            ICalibratable::class,
            ILockable::class,
            IPrintable::class,
            ITemperatureCtrl::class,
        ),
        priority = 10,
        probeTimeoutMs = 2_000L,
        probeSettleDelayMs = 500L,
    )

    override val probeStrategy: IProbeStrategy = JwCabinetProbeStrategy(protocol)

    override val heartbeatProvider: IHeartbeatProvider? = null

    override val pollingCommands: List<PollingCommand> = listOf(
        PollingCommand(
            payload = protocol.buildReadWeightCommand(),
            intervalMs = POLL_INTERVAL_MS,
        ),
        PollingCommand(
            payload = protocol.buildReadDoorCommand(doorCount),
            intervalMs = POLL_INTERVAL_MS,
        ),
    )

    override val frameParser: IFrameParser = JwCabinetRuntimeParser(
        delegate = parser,
        onLightCommand = factory::sendCommand,
    )

    override val deviceFactory: IDeviceFactory = factory

    companion object {
        const val STRATEGY_ID = "cabinet.jw.serial"
        const val VENDOR_NAME = "JW"
        const val DEVICE_MODEL = "JW Serial Cabinet"
        const val DEFAULT_LABEL_WIDTH_MM = 50
        const val DEFAULT_LABEL_HEIGHT_MM = 50
        private const val POLL_INTERVAL_MS = 240L
    }
}

private class JwCabinetProbeStrategy(
    private val protocol: JwCabinetProtocol,
) : IProbeStrategy {
    override fun buildProbeFrame(): ByteArray = protocol.buildReadWeightCommand()

    override fun validateResponse(response: ByteArray): ProbeResult {
        val frame = protocol.findFrame(response) ?: return ProbeResult.Mismatched
        val reading = protocol.parseFrame(
            frame = frame.payload,
            doorCount = JwCabinetProtocol.DEFAULT_DOOR_COUNT,
            slope = 1.0,
        ) ?: return ProbeResult.Mismatched
        return ProbeResult.Matched(
            deviceModel = JwSerialCabinetDriver.DEVICE_MODEL,
            firmwareVersion = reading::class.simpleName,
        )
    }
}

private class JwCabinetRuntimeParser(
    private val delegate: JwCabinetFrameParser,
    private val onLightCommand: (ByteArray) -> Unit,
) : IFrameParser {
    override fun extractFrame(raw: ByteArray): Frame? = delegate.extractFrame(raw)

    override fun parseFrame(frame: Frame): ParsedData? {
        val parsed = delegate.parseFrame(frame)
        delegate.consumePendingLightCommand()?.let(onLightCommand)
        return parsed
    }
}

private class JwSerialCabinetFactory(
    private val parser: JwCabinetFrameParser,
    private val protocol: JwCabinetProtocol,
    private val doorCount: Int,
    private val printerPortPath: String,
    private val printerBaudRate: Int,
    private val calibrationStore: JwCalibrationStore,
) : IDeviceFactory {
    private var device: JwSerialCabinetDevice? = null

    override suspend fun initialize(channel: SerialChannel): Boolean {
        parser.restoreCalibration(
            calibrationStore.readSlope(channel.portPath) ?: JwCabinetFrameParser.DEFAULT_SLOPE,
        )
        channel.write(protocol.buildReadWeightCommand())
        return true
    }

    override fun createDevice(channel: SerialChannel): IDevice {
        return JwSerialCabinetDevice(
            portPath = channel.portPath,
            channel = channel,
            parser = parser,
            protocol = protocol,
            doorCount = doorCount,
            calibrationStore = calibrationStore,
            printer = SerialPrinterClient(
                portPath = printerPortPath,
                baudRate = printerBaudRate,
            ),
        ).also { current ->
            device = current
        }
    }

    fun sendCommand(command: ByteArray) {
        device?.sendCommand(command)
    }
}

private class JwSerialCabinetDevice(
    portPath: String,
    private val channel: SerialChannel,
    private val parser: JwCabinetFrameParser,
    private val protocol: JwCabinetProtocol,
    private val doorCount: Int,
    private val calibrationStore: JwCalibrationStore,
    private val printer: SerialPrinterClient,
) : ICloseableDevice,
    IWeighable,
    ICalibratable,
    ILockable,
    IPrintable,
    ITemperatureCtrl {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val info: DeviceInfo = DeviceInfo(
        deviceId = "${JwSerialCabinetDriver.STRATEGY_ID}:$portPath",
        strategyId = JwSerialCabinetDriver.STRATEGY_ID,
        vendorName = JwSerialCabinetDriver.VENDOR_NAME,
        deviceModel = JwSerialCabinetDriver.DEVICE_MODEL,
        deviceCategory = DeviceCategory.CABINET,
    )

    fun sendCommand(command: ByteArray) {
        scope.launch {
            runCatching {
                channel.write(command)
            }
        }
    }

    override suspend fun close() {
        printer.close()
        scope.coroutineContext.cancelChildren()
    }

    override suspend fun weigh(): WeightResult = runCatching {
        parser.latestWeight()
    }.getOrElse {
        WeightResult(
            value = 0.0,
            unit = WeightUnit.GRAM,
            stable = false,
        )
    }

    override fun observeWeight(): Flow<WeightResult> = parser.observeWeight()

    override suspend fun tare() {
        zero()
    }

    override suspend fun zero() {
        channel.write(protocol.zeroCommand)
    }

    override suspend fun calibrate(standardWeightGrams: Double): CalibrationResult {
        val result = parser.applyCalibration(standardWeightGrams)
        val slope = result.slope
        if (result.success && slope != null) {
            calibrationStore.saveSlope(channel.portPath, slope)
        }
        return result
    }

    override suspend fun unlock(address: DoorAddress): Boolean {
        if (address.isCompartment || address.primaryIndex >= doorCount) return false
        channel.write(protocol.buildOpenDoorCommand(address.primaryIndex, doorCount))
        return true
    }

    override suspend fun lock(address: DoorAddress): Boolean = false

    override suspend fun queryDoorState(address: DoorAddress): DoorState {
        if (address.isCompartment || address.primaryIndex >= doorCount) return DoorState.UNKNOWN
        return parser.latestDoorState(address.primaryIndex)
    }

    override suspend fun print(content: PrintContent): PrintResult {
        val payload = TsplLabelRenderer.render(
            content = content,
            defaultWidthMm = JwSerialCabinetDriver.DEFAULT_LABEL_WIDTH_MM,
            defaultHeightMm = JwSerialCabinetDriver.DEFAULT_LABEL_HEIGHT_MM,
        )
        val success = printer.print(payload)
        return PrintResult(
            success = success,
            message = if (success) null else "JW 串口打印命令发送失败。",
        )
    }

    override suspend fun getPrinterStatus(): PrinterStatus = printer.status()

    override suspend fun setTemperature(celsius: Double): Boolean {
        val rawTemperature = celsius.toInt()
        channel.write(protocol.buildSetTemperatureCommand(rawTemperature))
        return true
    }

    override suspend fun getTemperature(): TemperatureReading = runCatching {
        parser.latestTemperature()
    }.getOrElse {
        TemperatureReading(celsius = Double.NaN)
    }

}

interface JwCalibrationStore {
    fun readSlope(portPath: String): Double?

    fun saveSlope(portPath: String, slope: Double)
}

object NoopJwCalibrationStore : JwCalibrationStore {
    override fun readSlope(portPath: String): Double? = null

    override fun saveSlope(portPath: String, slope: Double) = Unit
}

class SharedPreferencesJwCalibrationStore(
    context: Context,
) : JwCalibrationStore {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readSlope(portPath: String): Double? {
        val raw = preferences.getString(slopeKey(portPath), null) ?: return null
        return raw.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    }

    override fun saveSlope(portPath: String, slope: Double) {
        if (!slope.isFinite() || slope <= 0.0) return
        preferences.edit()
            .putString(slopeKey(portPath), slope.toString())
            .apply()
    }

    private fun slopeKey(portPath: String): String {
        return "jw_calibration_slope_${portPath.replace('/', '_')}"
    }

    companion object {
        private const val PREFERENCES_NAME = "holderzone_device_jw_calibration"
    }
}
