package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口通道缓存池，按端口路径保存当前已创建或已绑定的通道。
 */
class ChannelPool {
    private val channels = mutableMapOf<String, SerialChannel>()

    fun put(channel: SerialChannel) {
        channels[channel.portPath] = channel
    }

    fun get(portPath: String): SerialChannel? = channels[portPath]

    fun remove(portPath: String): SerialChannel? = channels.remove(portPath)

    fun clear() {
        channels.clear()
    }
}
