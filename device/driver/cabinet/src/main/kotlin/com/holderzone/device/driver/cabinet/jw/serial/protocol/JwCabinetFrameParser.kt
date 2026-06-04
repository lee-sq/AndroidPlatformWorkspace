package com.holderzone.device.driver.cabinet.jw.serial.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ParsedData
import com.holderzone.device.api.base.strategy.IFrameParser
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.model.CalibrationResult
import com.holderzone.device.api.scale.model.WeightResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:15
 * Description: JW 留样柜帧解析器，维护重量、门状态、温度和外门灯联动状态。
 */
class JwCabinetFrameParser(
    private val doorCount: Int = JwCabinetProtocol.DEFAULT_DOOR_COUNT,
    private val protocol: JwCabinetProtocol = JwCabinetProtocol,
    private val singleReadTimeoutMs: Long = DEFAULT_SINGLE_READ_TIMEOUT_MS,
) : IFrameParser {
    private val weightUpdates = MutableSharedFlow<WeightResult>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val temperatureUpdates = MutableSharedFlow<TemperatureReading>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val doorUpdates = MutableSharedFlow<Map<Int, DoorState>>(
        replay = 1,
        extraBufferCapacity = 16,
    )

    private val latestWeight = MutableStateFlow<WeightResult?>(null)
    private val latestTemperature = MutableStateFlow<TemperatureReading?>(null)
    private val latestDoorStates = MutableStateFlow<Map<Int, DoorState>>(emptyMap())

    var slope: Double = 1.0
        private set

    var lastRawWeightGrams: Double = 0.0
        private set

    var lastZeroOffsetGrams: Double = 0.0
        private set

    var pendingLightCommand: ByteArray? = null
        private set

    override fun extractFrame(raw: ByteArray): Frame? = protocol.findFrame(raw)

    override fun parseFrame(frame: Frame): ParsedData? {
        val reading = protocol.parseFrame(frame.payload, doorCount, slope) ?: return ParsedData(
            type = "jw-cabinet-frame",
            fields = mapOf(
                "raw" to protocol.bytesToHex(frame.payload),
                "recognized" to false,
            ),
        )

        return when (reading) {
            is JwCabinetReading.Weight -> parseWeight(reading, frame.payload)
            is JwCabinetReading.DoorTemperature -> parseDoorTemperature(reading, frame.payload)
        }
    }

    fun observeWeight(): Flow<WeightResult> = weightUpdates.asSharedFlow()

    fun observeTemperature(): Flow<TemperatureReading> = temperatureUpdates.asSharedFlow()

    fun observeDoorStates(): Flow<Map<Int, DoorState>> = doorUpdates.asSharedFlow()

    suspend fun latestWeight(): WeightResult {
        latestWeight.value?.let { return it }
        return withTimeout(singleReadTimeoutMs) {
            latestWeight.filterNotNull().first()
        }
    }

    suspend fun latestTemperature(): TemperatureReading {
        latestTemperature.value?.let { return it }
        return withTimeout(singleReadTimeoutMs) {
            latestTemperature.filterNotNull().first()
        }
    }

    fun latestDoorState(index: Int): DoorState = latestDoorStates.value[index] ?: DoorState.UNKNOWN

    fun restoreCalibration(slope: Double) {
        this.slope = slope.takeIf { it.isFinite() && it > 0.0 } ?: DEFAULT_SLOPE
        latestWeight.value?.let { current ->
            val calibrated = current.copy(value = lastRawWeightGrams * this.slope)
            latestWeight.value = calibrated
            weightUpdates.tryEmit(calibrated)
        }
    }

    fun applyCalibration(standardWeightGrams: Double): CalibrationResult {
        if (standardWeightGrams <= 0.0 || lastRawWeightGrams <= 0.0) {
            return CalibrationResult(
                success = false,
                rawWeightGrams = lastRawWeightGrams,
                zeroOffsetGrams = lastZeroOffsetGrams,
                standardWeightGrams = standardWeightGrams,
            )
        }
        val calibratedSlope = standardWeightGrams / lastRawWeightGrams
        if (!calibratedSlope.isFinite() || calibratedSlope <= 0.0) {
            return CalibrationResult(
                success = false,
                rawWeightGrams = lastRawWeightGrams,
                zeroOffsetGrams = lastZeroOffsetGrams,
                standardWeightGrams = standardWeightGrams,
            )
        }
        slope = calibratedSlope
        latestWeight.value?.let { current ->
            val calibrated = current.copy(value = lastRawWeightGrams * slope)
            latestWeight.value = calibrated
            weightUpdates.tryEmit(calibrated)
        }
        return CalibrationResult(
            success = true,
            slope = slope,
            rawWeightGrams = lastRawWeightGrams,
            zeroOffsetGrams = lastZeroOffsetGrams,
            standardWeightGrams = standardWeightGrams,
        )
    }

    fun consumePendingLightCommand(): ByteArray? {
        val command = pendingLightCommand
        pendingLightCommand = null
        return command
    }

    private fun parseWeight(
        reading: JwCabinetReading.Weight,
        raw: ByteArray,
    ): ParsedData {
        lastRawWeightGrams = reading.rawGrams
        lastZeroOffsetGrams = reading.zeroOffsetGrams
        latestWeight.value = reading.result
        weightUpdates.tryEmit(reading.result)
        pendingLightCommand = when {
            !reading.outsideDoorOpen && reading.outsideLightOpen -> protocol.lightOffCommand
            reading.outsideDoorOpen && !reading.outsideLightOpen -> protocol.lightOnCommand
            else -> null
        }
        return ParsedData(
            type = "jw-cabinet-weight",
            fields = mapOf(
                "value" to reading.result.value,
                "unit" to reading.result.unit,
                "grams" to reading.result.grams,
                "stable" to reading.result.stable,
                "rawGrams" to reading.rawGrams,
                "zeroOffsetGrams" to reading.zeroOffsetGrams,
                "slope" to slope,
                "outsideDoorOpen" to reading.outsideDoorOpen,
                "outsideLightOpen" to reading.outsideLightOpen,
                "raw" to protocol.bytesToHex(raw),
            ),
        )
    }

    private fun parseDoorTemperature(
        reading: JwCabinetReading.DoorTemperature,
        raw: ByteArray,
    ): ParsedData {
        latestDoorStates.value = reading.doorStates
        latestTemperature.value = reading.temperature
        doorUpdates.tryEmit(reading.doorStates)
        temperatureUpdates.tryEmit(reading.temperature)
        return ParsedData(
            type = "jw-cabinet-door-temperature",
            fields = mapOf(
                "doorStates" to reading.doorStates,
                "temperatureCelsius" to reading.temperature.celsius,
                "raw" to protocol.bytesToHex(raw),
            ),
        )
    }

    companion object {
        const val DEFAULT_SINGLE_READ_TIMEOUT_MS: Long = 1_000L
        const val DEFAULT_SLOPE: Double = 1.0
    }
}
