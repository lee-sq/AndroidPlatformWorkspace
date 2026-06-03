package com.holderzone.device.driver.scale.ly.protocol

import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 16:48
 * Description: 亮悦电子秤 ASCII 协议工具，负责分帧、解析重量和构造控制命令。
 */
object LyScaleProtocol {
    const val FRAME_LENGTH: Int = 26
    const val MAX_BUFFER_SIZE: Int = 2_048

    private const val START_FLAG: Byte = '='.code.toByte()
    private val stableFlags = setOf("2", "3", "6", "7")

    val tareCommand: ByteArray = byteArrayOf(0x54)
    val zeroCommand: ByteArray = byteArrayOf(0x5A)

    fun findFrameSlice(raw: ByteArray): FrameSlice? {
        var scanIndex = 0
        while (scanIndex < raw.size) {
            if (raw[scanIndex] == START_FLAG && raw.size - scanIndex >= FRAME_LENGTH) {
                val frame = raw.copyOfRange(scanIndex, scanIndex + FRAME_LENGTH)
                if (parseFrame(frame) != null) {
                    return FrameSlice(
                        frame = frame,
                        consumed = raw.copyOfRange(0, scanIndex + FRAME_LENGTH),
                    )
                }
            }
            scanIndex += 1
        }
        return null
    }

    fun parseFrame(frame: ByteArray): LyScaleReading? {
        if (frame.size != FRAME_LENGTH) return null
        if (frame.first() != START_FLAG) return null

        val rawFrame = frame.toString(StandardCharsets.UTF_8)
        return runCatching {
            val tareWeightKg = rawFrame.substring(9, 16).trim().toDouble()
            val netWeightKg = rawFrame.substring(17, 24).trim().toDouble()
            val flag = rawFrame.substring(25, 26)
            LyScaleReading(
                result = WeightResult(
                    value = netWeightKg,
                    unit = WeightUnit.KILOGRAM,
                    stable = flag in stableFlags,
                ),
                tareWeight = tareWeightKg,
                netMode = tareWeightKg > 0,
                zero = netWeightKg == 0.0,
                flag = flag,
                rawFrame = rawFrame,
            )
        }.getOrNull()
    }

    fun bytesToDisplayString(bytes: ByteArray): String {
        return bytes.toString(StandardCharsets.UTF_8)
    }

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    }

    fun buildFrameForTest(
        tareWeightKg: Double,
        netWeightKg: Double,
        flag: String,
    ): ByteArray {
        require(flag.length == 1) { "LY flag must be one character." }
        return "=${" ".repeat(8)}${field(tareWeightKg)} ${field(netWeightKg)} $flag"
            .also { require(it.length == FRAME_LENGTH) }
            .toByteArray(StandardCharsets.UTF_8)
    }

    private fun field(value: Double): String {
        return String.format(Locale.US, "%7.2f", value)
    }

    data class FrameSlice(
        val frame: ByteArray,
        val consumed: ByteArray,
    )
}
