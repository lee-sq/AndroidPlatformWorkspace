package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口后台读循环，持续从已打开通道读取字节并把异常交给运行时处理。
 */
class SerialReadLoop(
    private val channel: SerialChannel,
    private val scope: CoroutineScope,
    private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    private val onBytes: suspend (ByteArray) -> Unit,
    private val onError: suspend (Throwable) -> Unit = {},
) {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    /** 启动后台读取协程，持续读取通道字节并转交给上层分帧逻辑。 */
    fun start() {
        if (isRunning) return
        job = scope.launch {
            while (isActive && channel.isOpen) {
                try {
                    val data = channel.read(readTimeoutMs)
                    if (data.isNotEmpty()) {
                        onBytes(data)
                    } else {
                        // 没有数据时短暂让出协程，避免空读导致 CPU 忙等。
                        delay(IDLE_DELAY_MS)
                    }
                } catch (throwable: Throwable) {
                    // 读循环异常交由 DeviceRuntime 统一降级和重连。
                    onError(throwable)
                    break
                }
            }
        }
    }

    /** 停止后台读取协程；不会主动关闭 SerialChannel，通道由会话统一释放。 */
    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEFAULT_READ_TIMEOUT_MS: Long = 500L
        const val IDLE_DELAY_MS: Long = 50L
    }
}
