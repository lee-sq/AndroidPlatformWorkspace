package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 19:05
 * Description: 厂商 SDK 自管理连接占位通道，只携带端口身份，不占用真实串口资源。
 */
class SelfManagedSerialChannel(
    override val portPath: String,
    override val config: SerialConfig,
) : SerialChannel {
    override var isOpen: Boolean = false
        private set

    override suspend fun open() {
        isOpen = true
    }

    override suspend fun close() {
        isOpen = false
    }

    override suspend fun write(data: ByteArray) = Unit

    override suspend fun read(timeoutMs: Long): ByteArray = ByteArray(0)
}
