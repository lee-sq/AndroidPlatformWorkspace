package com.holderzone.device.driver.cabinet.jw.serial.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 17:30
 * Description: JW 留样柜串口协议工具，负责命令拼包、分帧、CRC 校验和业务字段解析。
 */
object JwCabinetProtocol {
    const val DEFAULT_DOOR_COUNT: Int = 8
    const val DEFAULT_BAUD_RATE: Int = 9_600
    const val DEFAULT_CABINET_PORT: String = "/dev/ttyS2"
    const val DEFAULT_PRINTER_PORT: String = "/dev/ttyS3"
    const val DEFAULT_PRINTER_BAUD_RATE: Int = 9_600

    private const val WEIGHT_STATE = 2
    private const val DOOR_LIGHT_STATE_1 = 16
    private const val DOOR_LIGHT_STATE_2 = 17
    private const val DOOR_LIGHT_STATE_3 = 18
    private const val LOCK_THRESHOLD = 17_000
    private const val OUT_DOOR_INDEX = 0

    val lightOnCommand: ByteArray = hexToBytes("02100005000102AAAA4C2A")
    val lightOffCommand: ByteArray = hexToBytes("021000050001020000B2F5")
    val zeroCommand: ByteArray = hexToBytes("02100002000102AAAA4D9D")

    fun buildReadWeightCommand(): ByteArray {
        return buildCommand(
            0x02,
            0x03,
            0x00,
            0x00,
            0x00,
            0x12,
        )
    }

    fun buildReadDoorCommand(doorCount: Int = DEFAULT_DOOR_COUNT): ByteArray {
        return buildCommand(
            0x10,
            0x03,
            0x00,
            0x00,
            0x00,
            doorCount + 1,
        )
    }

    fun buildSetTemperatureCommand(rawTemperature: Int): ByteArray {
        return buildCommand(
            0x02,
            0x10,
            0x00,
            0x00,
            0x00,
            0x01,
            0x02,
            rawTemperature ushr 8,
            rawTemperature and 0xFF,
        )
    }

    fun buildOpenDoorCommand(
        doorIndex: Int,
        doorCount: Int = DEFAULT_DOOR_COUNT,
        openDoorDelayMs: Int = 500,
    ): ByteArray {
        val payload = IntArray(7 + doorCount * 2)
        payload[0] = 16 + OUT_DOOR_INDEX
        payload[1] = 16
        payload[2] = 0
        payload[3] = 0
        payload[4] = 0
        payload[5] = doorCount
        payload[6] = doorCount * 2
        for (index in 0 until doorCount) {
            if (doorIndex == index) {
                payload[7 + 2 * index] = openDoorDelayMs ushr 8
                payload[8 + 2 * index] = openDoorDelayMs and 0xFF
            }
        }
        return buildCommand(*payload)
    }

    fun findFrame(raw: ByteArray): Frame? {
        if (raw.isEmpty()) return null

        var start = 0
        while (start < raw.size) {
            val address = raw[start].uByte()
            if (address !in setOf(1, 2, 6, 16, 17, 18, 19)) {
                start += 1
                continue
            }
            if (raw.size - start < 2) return null

            val command = raw[start + 1].uByte()
            if (command != 3 && command != 16) {
                start += 1
                continue
            }
            if (command == 16) {
                val writeResponseLength = 8
                if (raw.size - start < writeResponseLength) return null
                val candidate = raw.copyOfRange(start, start + writeResponseLength)
                if (isValidCrc(candidate)) {
                    return Frame(payload = candidate, raw = raw.copyOfRange(0, start + writeResponseLength))
                }
                start += 1
                continue
            }

            if (raw.size - start < 3) return null
            val payloadSize = raw[start + 2].uByte()
            val frameLength = payloadSize + 5
            if (raw.size - start < frameLength) {
                if (start == 0) return null
                start += 1
                continue
            }

            val candidate = raw.copyOfRange(start, start + frameLength)
            if (isValidCrc(candidate)) {
                return Frame(payload = candidate, raw = raw.copyOfRange(0, start + frameLength))
            }
            start += 1
        }
        return null
    }

    fun parseFrame(frame: ByteArray, doorCount: Int, slope: Double): JwCabinetReading? {
        if (frame.size < 4 || !isValidCrc(frame)) return null
        return when (frame.first().uByte()) {
            WEIGHT_STATE -> parseWeightFrame(frame, slope)
            DOOR_LIGHT_STATE_1,
            DOOR_LIGHT_STATE_2,
            DOOR_LIGHT_STATE_3,
            -> parseDoorTemperatureFrame(frame, doorCount)
            else -> null
        }
    }

    fun parseWeightFrame(frame: ByteArray, slope: Double): JwCabinetReading.Weight? {
        if (!isValidCrc(frame)) return null
        val hexData = bytesToHex(frame)
        if (hexData.length < 34) return null

        val zeroWeightGrams = kilogramsToGrams(hexData.substring(18, 22).hexToInt().toDouble())
        val realWeightGrams = kilogramsToGrams(hexData.substring(26, 30).hexToInt().toDouble())
        val rawWeightGrams = realWeightGrams - zeroWeightGrams
        val isOuterLightOpen = hexData.substring(22, 26).hexToInt() != 0
        val outsideDoorOpen = hexData.substring(30, 34).hexToInt() != 0
        return JwCabinetReading.Weight(
            result = WeightResult(
                value = rawWeightGrams * slope,
                unit = WeightUnit.GRAM,
            ),
            rawGrams = rawWeightGrams,
            zeroOffsetGrams = zeroWeightGrams,
            outsideDoorOpen = outsideDoorOpen,
            outsideLightOpen = isOuterLightOpen,
        )
    }

    fun parseDoorTemperatureFrame(frame: ByteArray, doorCount: Int): JwCabinetReading.DoorTemperature? {
        if (!isValidCrc(frame)) return null
        val hexData = bytesToHex(frame)
        if (hexData.length < 10) return null

        val temperature = hexData.substring(6, 10).hexToInt() - 40
        val doorStates = linkedMapOf<Int, DoorState>()
        for (index in 0..doorCount) {
            val startIndex = (5 + index * 2) * 2
            val endIndex = (7 + index * 2) * 2
            if (endIndex > hexData.length) break

            val rawDoorValue = hexData.substring(startIndex, endIndex).hexToInt()
            if (index > 0) {
                val address = index - 1
                doorStates[address] = if (rawDoorValue > LOCK_THRESHOLD) DoorState.OPEN else DoorState.CLOSED
            }
        }
        return JwCabinetReading.DoorTemperature(
            doorStates = doorStates,
            temperature = TemperatureReading(celsius = temperature.toDouble()),
        )
    }

    fun isValidCrc(frame: ByteArray): Boolean {
        if (frame.size < 4) return false
        val expected = crc16(frame.copyOfRange(0, frame.size - 2))
        val high = ((expected ushr 8) and 0xFF).toByte()
        val low = (expected and 0xFF).toByte()
        return frame[frame.lastIndex - 1] == high && frame[frame.lastIndex] == low
    }

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    }

    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string length must be even." }
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun buildCommand(vararg values: Int): ByteArray {
        val payload = ByteArray(values.size) { index -> (values[index] and 0xFF).toByte() }
        val crc = crc16(payload)
        return payload + byteArrayOf(
            ((crc ushr 8) and 0xFF).toByte(),
            (crc and 0xFF).toByte(),
        )
    }

    private fun crc16(data: ByteArray): Int {
       var high = 0xFF
        var low = 0xFF
        for (byte in data) {
            val index = high xor (byte.toInt() and 0xFF)
            high = low xor CRC_HIGH[index]
            low = CRC_LOW[index]
        }
        return ((high and 0xFF) shl 8) or (low and 0xFF)
    }

    private fun kilogramsToGrams(value: Double): Double = value * 1_000.0

    private fun Byte.uByte(): Int = toInt() and 0xFF

    private fun String.hexToInt(): Int = toInt(16)

    private val CRC_HIGH = intArrayOf(
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
    )

    private val CRC_LOW = intArrayOf(
        0x00, 0xC0, 0xC1, 0x01, 0xC3, 0x03, 0x02, 0xC2, 0xC6, 0x06, 0x07, 0xC7, 0x05, 0xC5, 0xC4, 0x04,
        0xCC, 0x0C, 0x0D, 0xCD, 0x0F, 0xCF, 0xCE, 0x0E, 0x0A, 0xCA, 0xCB, 0x0B, 0xC9, 0x09, 0x08, 0xC8,
        0xD8, 0x18, 0x19, 0xD9, 0x1B, 0xDB, 0xDA, 0x1A, 0x1E, 0xDE, 0xDF, 0x1F, 0xDD, 0x1D, 0x1C, 0xDC,
        0x14, 0xD4, 0xD5, 0x15, 0xD7, 0x17, 0x16, 0xD6, 0xD2, 0x12, 0x13, 0xD3, 0x11, 0xD1, 0xD0, 0x10,
        0xF0, 0x30, 0x31, 0xF1, 0x33, 0xF3, 0xF2, 0x32, 0x36, 0xF6, 0xF7, 0x37, 0xF5, 0x35, 0x34, 0xF4,
        0x3C, 0xFC, 0xFD, 0x3D, 0xFF, 0x3F, 0x3E, 0xFE, 0xFA, 0x3A, 0x3B, 0xFB, 0x39, 0xF9, 0xF8, 0x38,
        0x28, 0xE8, 0xE9, 0x29, 0xEB, 0x2B, 0x2A, 0xEA, 0xEE, 0x2E, 0x2F, 0xEF, 0x2D, 0xED, 0xEC, 0x2C,
        0xE4, 0x24, 0x25, 0xE5, 0x27, 0xE7, 0xE6, 0x26, 0x22, 0xE2, 0xE3, 0x23, 0xE1, 0x21, 0x20, 0xE0,
        0xA0, 0x60, 0x61, 0xA1, 0x63, 0xA3, 0xA2, 0x62, 0x66, 0xA6, 0xA7, 0x67, 0xA5, 0x65, 0x64, 0xA4,
        0x6C, 0xAC, 0xAD, 0x6D, 0xAF, 0x6F, 0x6E, 0xAE, 0xAA, 0x6A, 0x6B, 0xAB, 0x69, 0xA9, 0xA8, 0x68,
        0x78, 0xB8, 0xB9, 0x79, 0xBB, 0x7B, 0x7A, 0xBA, 0xBE, 0x7E, 0x7F, 0xBF, 0x7D, 0xBD, 0xBC, 0x7C,
        0xB4, 0x74, 0x75, 0xB5, 0x77, 0xB7, 0xB6, 0x76, 0x72, 0xB2, 0xB3, 0x73, 0xB1, 0x71, 0x70, 0xB0,
        0x50, 0x90, 0x91, 0x51, 0x93, 0x53, 0x52, 0x92, 0x96, 0x56, 0x57, 0x97, 0x55, 0x95, 0x94, 0x54,
        0x9C, 0x5C, 0x5D, 0x9D, 0x5F, 0x9F, 0x9E, 0x5E, 0x5A, 0x9A, 0x9B, 0x5B, 0x99, 0x59, 0x58, 0x98,
        0x88, 0x48, 0x49, 0x89, 0x4B, 0x8B, 0x8A, 0x4A, 0x4E, 0x8E, 0x8F, 0x4F, 0x8D, 0x4D, 0x4C, 0x8C,
        0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82, 0x42, 0x43, 0x83, 0x41, 0x81, 0x80, 0x40,
    )
}

sealed class JwCabinetReading {
    data class Weight(
        val result: WeightResult,
        val rawGrams: Double,
        val zeroOffsetGrams: Double,
        val outsideDoorOpen: Boolean,
        val outsideLightOpen: Boolean,
    ) : JwCabinetReading()

    data class DoorTemperature(
        val doorStates: Map<Int, DoorState>,
        val temperature: TemperatureReading,
    ) : JwCabinetReading()
}
