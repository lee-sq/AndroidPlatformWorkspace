package com.holderzone.device.core.channel

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.strategy.IFrameParser
import java.io.ByteArrayOutputStream

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 字节流分帧器，缓存串口连续字节并通过驱动帧解析器处理半包、粘包和多帧。
 */
class FrameStreamReader(
    private val frameParser: IFrameParser,
    private val maxBufferSize: Int = DEFAULT_MAX_BUFFER_SIZE,
) {
    private val buffer = ByteArrayOutputStream()

    /** 追加新读到的字节，并尽可能从累计缓冲区中切出所有完整帧。 */
    fun append(bytes: ByteArray): List<Frame> {
        if (bytes.isEmpty()) return emptyList()

        buffer.write(bytes)
        trimIfNeeded()

        val frames = mutableListOf<Frame>()
        var extracted = frameParser.extractFrame(buffer.toByteArray())
        while (extracted != null) {
            frames += extracted
            val raw = extracted.raw
            val current = buffer.toByteArray()
            // raw 表示 parser 已经消费的完整原始片段，剩余字节保留给下一次拼包。
            val next = if (raw.isNotEmpty() && current.size >= raw.size) {
                current.copyOfRange(raw.size, current.size)
            } else {
                ByteArray(0)
            }
            buffer.reset()
            buffer.write(next)
            extracted = frameParser.extractFrame(buffer.toByteArray())
        }

        return frames
    }

    /** 清空累计缓冲区，通常在通道关闭或重新绑定时使用。 */
    fun clear() {
        buffer.reset()
    }

    private fun trimIfNeeded() {
        if (buffer.size() <= maxBufferSize) return

        // 避免异常数据长期无法成帧导致内存增长，仅保留尾部数据等待后续拼包。
        val bytes = buffer.toByteArray()
        val tail = bytes.copyOfRange(bytes.size - maxBufferSize, bytes.size)
        buffer.reset()
        buffer.write(tail)
    }

    companion object {
        const val DEFAULT_MAX_BUFFER_SIZE: Int = 8 * 1024
    }
}
