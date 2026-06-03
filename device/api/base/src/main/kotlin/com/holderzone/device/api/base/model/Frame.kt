package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 协议帧模型，payload 表示业务有效载荷，raw 表示从缓冲区消费掉的完整原始字节。
 */
data class Frame(
    val payload: ByteArray,
    val raw: ByteArray = payload,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Frame) return false
        if (!payload.contentEquals(other.payload)) return false
        if (!raw.contentEquals(other.raw)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + raw.contentHashCode()
        return result
    }
}
