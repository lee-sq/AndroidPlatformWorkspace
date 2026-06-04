package com.holderzone.device.driver.cabinet.print

import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.cabinet.model.PrinterStatus
import com.holderzone.device.core.channel.AndroidSerialChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:20
 * Description: 附属打印串口客户端，供柜机 driver 在不暴露业务绑定细节的情况下发送打印命令。
 */
class SerialPrinterClient(
    private val portPath: String,
    private val baudRate: Int,
    private val channelFactory: (String, SerialConfig) -> AndroidSerialChannel = { path, config ->
        AndroidSerialChannel(portPath = path, config = config)
    },
) {
    private val mutex = Mutex()
    private var channel: AndroidSerialChannel? = null

    suspend fun print(payload: ByteArray): Boolean {
        return runCatching {
            mutex.withLock {
                val opened = ensureChannel()
                opened.write(payload)
            }
            true
        }.getOrDefault(false)
    }

    suspend fun status(): PrinterStatus {
        return if (mutex.withLock { channel?.isOpen == true }) {
            PrinterStatus.READY
        } else {
            PrinterStatus.UNKNOWN
        }
    }

    suspend fun close() {
        mutex.withLock {
            channel?.close()
            channel = null
        }
    }

    private suspend fun ensureChannel(): AndroidSerialChannel {
        val current = channel
        if (current?.isOpen == true) return current
        val next = channelFactory(portPath, SerialConfig(baudRate = baudRate))
        next.open()
        channel = next
        return next
    }
}
