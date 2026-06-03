package com.holderzone.device.core.channel

import android_serialport_api.SerialPort
import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.Parity
import com.holderzone.device.api.base.model.SerialConfig
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 基于 serialport native so 的真实串口通道，实现 8N1 串口打开、读写和关闭。
 */
class AndroidSerialChannel(
    override val portPath: String,
    override val config: SerialConfig,
    private val flags: Int = DEFAULT_FLAGS,
    private val readBufferSize: Int = DEFAULT_READ_BUFFER_SIZE,
) : SerialChannel {
    /** 保护 open/close 状态切换，避免并发打开或关闭导致 native 句柄错乱。 */
    private val stateMutex = Mutex()

    /** 串口写入串行化，防止多个命令帧交叉写入同一个 OutputStream。 */
    private val writeMutex = Mutex()

    @Volatile
    private var serialPort: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override val isOpen: Boolean
        get() = serialPort != null

    /** 校验配置后通过 native SerialPort 打开设备文件，并缓存输入输出流。 */
    override suspend fun open() {
        stateMutex.withLock {
            if (isOpen) return
            validateSupportedConfig()
            val port = withContext(Dispatchers.IO) {
                SerialPort(File(portPath), config.baudRate, flags)
            }
            serialPort = port
            inputStream = port.inputStream
            outputStream = port.outputStream
        }
    }

    /** 先清空 Kotlin 侧引用，再在 IO 线程关闭 native 串口句柄。 */
    override suspend fun close() {
        val port = stateMutex.withLock {
            val current = serialPort
            serialPort = null
            inputStream = null
            outputStream = null
            current
        } ?: return

        withContext(Dispatchers.IO) {
            port.close()
        }
    }

    /** 写入完整命令帧并 flush，确保探测、心跳或控制命令立即发往设备。 */
    override suspend fun write(data: ByteArray) {
        if (data.isEmpty()) return
        val stream = outputStream ?: error("Serial channel $portPath is not open.")
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                stream.write(data)
                stream.flush()
            }
        }
    }

    /** 在 IO 线程轮询 InputStream.available，超时返回空数组而不是阻塞调用方。 */
    override suspend fun read(timeoutMs: Long): ByteArray {
        val stream = inputStream ?: error("Serial channel $portPath is not open.")
        return withContext(Dispatchers.IO) {
            readAvailable(stream, timeoutMs)
        }
    }

    private fun validateSupportedConfig() {
        // 当前打包的 serialport-1.0.1 native so 只暴露 baudRate/flags，无法设置完整 termios。
        require(config.dataBits == 8 && config.stopBits == 1 && config.parity == Parity.NONE) {
            "serialport-1.0.1 native backend only supports 8N1, but got $config."
        }
    }

    private suspend fun readAvailable(stream: InputStream, timeoutMs: Long): ByteArray {
        val deadline = System.nanoTime() + timeoutMs.coerceAtLeast(0L) * NANOS_PER_MILLISECOND
        while (true) {
            val available = stream.available()
            if (available > 0) {
                val buffer = ByteArray(minOf(available, readBufferSize))
                val size = stream.read(buffer)
                return if (size > 0) buffer.copyOf(size) else ByteArray(0)
            }

            if (timeoutMs <= 0L || System.nanoTime() >= deadline) {
                return ByteArray(0)
            }
            delay(READ_POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val DEFAULT_FLAGS = 0
        private const val DEFAULT_READ_BUFFER_SIZE = 512
        private const val READ_POLL_INTERVAL_MS = 10L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
