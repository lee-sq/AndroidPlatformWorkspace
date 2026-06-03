package com.holderzone.device.driver.scale.jw.protocol

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ParsedData
import com.holderzone.device.api.base.strategy.IFrameParser
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
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 帧解析器，从 Modbus 字节流中提取重量帧并发布最新称重结果。
 */
class JWScale3568FrameParser(
    private val protocol: JWScale3568Protocol = JWScale3568Protocol,
    private val singleReadTimeoutMs: Long = DEFAULT_SINGLE_READ_TIMEOUT_MS,
) : IFrameParser {
    /** replay=1 保留最后一次重量，晚订阅的 UI 也能立即拿到最近读数。 */
    private val weightUpdates = MutableSharedFlow<WeightResult>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val latestWeight = MutableStateFlow<WeightResult?>(null)

    /** 从累计字节中查找一段合法 Modbus 帧，raw 使用 consumed 表示需要从流缓冲区移除的范围。 */
    override fun extractFrame(raw: ByteArray): Frame? {
        val slice = protocol.findFrameSlice(raw) ?: return null
        return Frame(payload = slice.frame, raw = slice.consumed)
    }

    /** 解析重量帧并更新最新重量缓存；非重量帧仍以 ParsedData 返回，便于调试。 */
    override fun parseFrame(frame: Frame): ParsedData? {
        val reading = protocol.parseWeight(frame.payload) ?: return ParsedData(
            type = "jw-scale-3568-frame",
            fields = mapOf(
                "raw" to protocol.bytesToHex(frame.payload),
                "recognized" to false,
            ),
        )

        latestWeight.value = reading.result
        weightUpdates.tryEmit(reading.result)
        return ParsedData(
            type = "jw-scale-3568-weight",
            fields = mapOf(
                "value" to reading.result.value,
                "unit" to reading.result.unit,
                "grams" to reading.result.grams,
                "stable" to reading.result.stable,
                "adValue" to reading.adValue,
                "zeroAdValue" to reading.zeroAdValue,
                "batteryVoltage" to reading.batteryVoltage,
                "temperatureCelsius" to reading.temperatureCelsius,
                "coefficient" to reading.coefficient,
                "grossWeight" to reading.grossWeight,
                "tareWeight" to reading.tareWeight,
                "holdState" to reading.holdState,
                "raw" to protocol.bytesToHex(reading.raw),
            ),
        )
    }

    /** 观察电子秤主动上报的重量变化。 */
    fun observeWeight(): Flow<WeightResult> = weightUpdates.asSharedFlow()

    /** 获取最近重量；如果还没有任何上报，则等待一段时间后由 withTimeout 抛出超时。 */
    suspend fun latestWeight(): WeightResult {
        latestWeight.value?.let { return it }
        return withTimeout(singleReadTimeoutMs) {
            latestWeight.filterNotNull().first()
        }
    }

    companion object {
        const val DEFAULT_SINGLE_READ_TIMEOUT_MS: Long = 1_000L
    }
}
