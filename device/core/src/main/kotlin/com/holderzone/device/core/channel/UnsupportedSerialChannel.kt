package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 不可用串口通道占位实现，用于未安装真实串口后端时给出明确失败。
 */
class UnsupportedSerialChannel(
    override val portPath: String,
    override val config: SerialConfig,
) : SerialChannel {
    override val isOpen: Boolean = false

    override suspend fun open() {
        error("No real serial backend is installed for $portPath.")
    }

    override suspend fun close() = Unit

    override suspend fun write(data: ByteArray) {
        error("No real serial backend is installed for $portPath.")
    }

    override suspend fun read(timeoutMs: Long): ByteArray {
        error("No real serial backend is installed for $portPath.")
    }
}
