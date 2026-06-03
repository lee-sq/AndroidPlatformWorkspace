package com.holderzone.device.driver.scale.jw.protocol

import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 Modbus 协议工具，封装命令构造、帧查找、CRC 校验和重量解析。
 */
object JWScale3568Protocol {
    /** 重量主动上报帧长度：帧头 3 字节 + 寄存器数据 + CRC。 */
    const val WEIGHT_FRAME_LENGTH: Int = 29
    /** 版本响应帧长度，用于探测阶段识别精卫 3568 主板。 */
    const val VERSION_FRAME_LENGTH: Int = 19
    /** 标定参数响应帧长度，当前只识别完整帧，不在业务层展开。 */
    const val CALIBRATION_FRAME_LENGTH: Int = 37

    private val weightFrameHeader = byteArrayOf(0x01, 0x03, 0x18)
    private val versionFrameHeader = byteArrayOf(0x01, 0x03, 0x0E)
    private val calibrationFrameHeader = byteArrayOf(0x01, 0x03, 0x20)

    val readParameterCommand: ByteArray = hexToBytes("01030100001045FA")
    val readVersionCommand: ByteArray = hexToBytes("01030200000705B0")

    /** 构建置零命令，对应 reset=1、peel=0。 */
    fun buildZeroCommand(): ByteArray {
        return buildScaleControlCommand(reset = true, peel = false)
    }

    /** 构建去皮命令，对应 reset=0、peel=1。 */
    fun buildTareCommand(): ByteArray {
        return buildScaleControlCommand(reset = false, peel = true)
    }

    fun findFrame(raw: ByteArray): ByteArray? = findFrameSlice(raw)?.frame

    /** 在混合字节流中寻找最靠前的合法帧，同时返回应该从缓冲区消费掉的范围。 */
    fun findFrameSlice(raw: ByteArray): FrameSlice? {
        val candidates = listOf(
            weightFrameHeader to WEIGHT_FRAME_LENGTH,
            versionFrameHeader to VERSION_FRAME_LENGTH,
            calibrationFrameHeader to CALIBRATION_FRAME_LENGTH,
        )
        return candidates
            .flatMap { (header, length) ->
                raw.findFrameCandidates(header, length)
            }
            .minByOrNull { it.start }
            ?.let { candidate ->
                FrameSlice(
                    frame = candidate.frame,
                    consumed = raw.copyOfRange(0, candidate.start + candidate.length),
                )
            }
    }

    /** 将精卫重量帧解析为业务重量和诊断字段，帧头、长度或 CRC 不合法时返回 null。 */
    fun parseWeight(frame: ByteArray): JWScaleWeightReading? {
        if (!frame.startsWith(weightFrameHeader)) return null
        if (frame.size != WEIGHT_FRAME_LENGTH) return null
        if (!isValidCrc(frame)) return null

        val adValue = frame.uint32(3)
        val zeroAdValue = frame.uint32(7)
        val batteryVoltageRaw = frame.uint16(11)
        val weightRaw = frame.uint16(13)
        val signRaw = frame.uint16(15)
        val temperature = frame.uint16(17) - 40
        val coefficient = frame.uint32(19)
        val peelRaw = frame.uint16(23)
        val holdState = frame.uByte(26)
        val sign = if (signRaw == 0) 1.0 else -1.0
        val grossWeight = oneDecimal(weightRaw / 100.0)
        val tareWeight = oneDecimal(peelRaw / 100.0)
        // 协议重量单位按 0.01kg 上报，业务侧统一截到一位小数。
        val realWeight = oneDecimal((weightRaw - peelRaw) * sign / 100.0)

        return JWScaleWeightReading(
            result = WeightResult(
                value = realWeight,
                unit = WeightUnit.KILOGRAM,
                stable = holdState != 0,
            ),
            adValue = adValue,
            zeroAdValue = zeroAdValue,
            batteryVoltage = batteryVoltageRaw / 100.0,
            temperatureCelsius = temperature,
            coefficient = coefficient,
            grossWeight = grossWeight,
            tareWeight = tareWeight,
            holdState = holdState,
            raw = frame,
        )
    }

    /** 校验 Modbus CRC16，协议帧末尾为低字节在前、高字节在后。 */
    fun isValidCrc(frame: ByteArray): Boolean {
        if (frame.size < 4) return false

        val expected = crc16Modbus(frame.copyOfRange(0, frame.size - 2))
        val expectedLow = (expected and 0xFF).toByte()
        val expectedHigh = ((expected ushr 8) and 0xFF).toByte()
        return frame[frame.lastIndex - 1] == expectedLow && frame[frame.lastIndex] == expectedHigh
    }

    /** 将十六进制命令字符串转换为字节数组，供固定协议命令声明使用。 */
    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string length must be even." }
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    /** 将原始字节转换为大写十六进制字符串，主要用于 ParsedData 调试字段。 */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    }

    /** 写多个保持寄存器的控制命令，最后拼接 Modbus 小端 CRC。 */
    private fun buildScaleControlCommand(reset: Boolean, peel: Boolean): ByteArray {
        val resetValue = if (reset) "0001" else "0000"
        val peelValue = if (peel) "0001" else "0000"
        val payload = hexToBytes(
            "01100000000204$resetValue$peelValue",
        )
        return payload + crcLittleEndian(payload)
    }

    /** 生成 Modbus CRC 的小端字节序，符合串口帧尾部格式。 */
    private fun crcLittleEndian(bytes: ByteArray): ByteArray {
        val crc = crc16Modbus(bytes)
        return byteArrayOf(
            (crc and 0xFF).toByte(),
            ((crc ushr 8) and 0xFF).toByte(),
        )
    }

    /** 标准 Modbus CRC16 算法，多数串口设备用它校验帧完整性。 */
    private fun crc16Modbus(bytes: ByteArray): Int {
        var crc = 0xFFFF
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    /** 在缓冲区里查找指定帧头和长度的所有合法候选帧。 */
    private fun ByteArray.findFrameCandidates(header: ByteArray, length: Int): List<FrameCandidate> {
        val candidates = mutableListOf<FrameCandidate>()
        var start = indexOf(header, fromIndex = 0)
        while (start >= 0 && size - start >= length) {
            val frame = copyOfRange(start, start + length)
            if (isValidCrc(frame)) {
                candidates += FrameCandidate(
                    start = start,
                    length = length,
                    frame = frame,
                )
            }
            start = indexOf(header, fromIndex = start + 1)
        }
        return candidates
    }

    private fun ByteArray.indexOf(target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || target.size > size) return -1
        for (index in fromIndex..size - target.size) {
            var matched = true
            for (targetIndex in target.indices) {
                if (this[index + targetIndex] != target[targetIndex]) {
                    matched = false
                    break
                }
            }
            if (matched) return index
        }
        return -1
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (prefix.size > size) return false
        return prefix.indices.all { index -> this[index] == prefix[index] }
    }

    private fun ByteArray.uByte(index: Int): Int = this[index].toInt() and 0xFF

    private fun ByteArray.uint16(index: Int): Int {
        return (uByte(index) shl 8) or uByte(index + 1)
    }

    private fun ByteArray.uint32(index: Int): Long {
        return (uByte(index).toLong() shl 24) or
            (uByte(index + 1).toLong() shl 16) or
            (uByte(index + 2).toLong() shl 8) or
            uByte(index + 3).toLong()
    }

    private fun oneDecimal(value: Double): Double {
        val scaled = value * 10.0
        return if (value >= 0) {
            floor(scaled) / 10.0
        } else {
            ceil(scaled) / 10.0
        }
    }

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 候选协议帧片段，记录在缓冲区中的起点、长度和完整帧字节。
     */
    private data class FrameCandidate(
        val start: Int,
        val length: Int,
        val frame: ByteArray,
    )

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 已识别协议帧片段，frame 是有效帧，consumed 是应从流缓冲区移除的字节。
     */
    data class FrameSlice(
        val frame: ByteArray,
        val consumed: ByteArray,
    )
}
