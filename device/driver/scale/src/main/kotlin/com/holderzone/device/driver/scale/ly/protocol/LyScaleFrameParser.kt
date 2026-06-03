package com.holderzone.device.driver.scale.ly.protocol

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
 * CrateTime：2026/6/3 16:48
 * Description: 亮悦电子秤帧解析器，从主动上报 ASCII 字节流中切出 26 字节重量帧。
 */
class LyScaleFrameParser(
    private val protocol: LyScaleProtocol = LyScaleProtocol,
    private val singleReadTimeoutMs: Long = DEFAULT_SINGLE_READ_TIMEOUT_MS,
) : IFrameParser {
    private val weightUpdates = MutableSharedFlow<WeightResult>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val latestWeight = MutableStateFlow<WeightResult?>(null)

    override fun extractFrame(raw: ByteArray): Frame? {
        val slice = protocol.findFrameSlice(raw) ?: return null
        return Frame(payload = slice.frame, raw = slice.consumed)
    }

    override fun parseFrame(frame: Frame): ParsedData? {
        val reading = protocol.parseFrame(frame.payload) ?: return ParsedData(
            type = "ly-scale-frame",
            fields = mapOf(
                "raw" to protocol.bytesToHex(frame.payload),
                "recognized" to false,
            ),
        )

        latestWeight.value = reading.result
        weightUpdates.tryEmit(reading.result)
        return ParsedData(
            type = "ly-scale-weight",
            fields = mapOf(
                "value" to reading.result.value,
                "unit" to reading.result.unit,
                "grams" to reading.result.grams,
                "stable" to reading.result.stable,
                "tareWeight" to reading.tareWeight,
                "netMode" to reading.netMode,
                "zero" to reading.zero,
                "flag" to reading.flag,
                "rawFrame" to reading.rawFrame,
            ),
        )
    }

    fun observeWeight(): Flow<WeightResult> = weightUpdates.asSharedFlow()

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
