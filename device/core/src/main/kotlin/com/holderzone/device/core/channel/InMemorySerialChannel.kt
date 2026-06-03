package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 内存串口通道，用队列模拟读缓冲并记录写出的字节帧。
 */
class InMemorySerialChannel(
    override val portPath: String,
    override val config: SerialConfig,
    private val readBuffer: ArrayDeque<ByteArray> = ArrayDeque(),
) : SerialChannel {
    private val mutex = Mutex()
    private val writtenFrames = mutableListOf<ByteArray>()

    override var isOpen: Boolean = false
        private set

    val writes: List<ByteArray>
        get() = writtenFrames.toList()

    override suspend fun open() {
        mutex.withLock {
            isOpen = true
        }
    }

    override suspend fun close() {
        mutex.withLock {
            isOpen = false
        }
    }

    override suspend fun write(data: ByteArray) {
        mutex.withLock {
            check(isOpen) { "Serial channel $portPath is not open." }
            writtenFrames += data.copyOf()
        }
    }

    override suspend fun read(timeoutMs: Long): ByteArray {
        return mutex.withLock {
            check(isOpen) { "Serial channel $portPath is not open." }
            readBuffer.removeFirstOrNull() ?: ByteArray(0)
        }
    }

    suspend fun enqueueRead(data: ByteArray) {
        mutex.withLock {
            readBuffer.addLast(data.copyOf())
        }
    }
}
