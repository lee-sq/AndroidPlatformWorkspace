package com.holderzone.device.api.base.channel

import com.holderzone.device.api.base.model.SerialConfig

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口通信通道抽象，屏蔽真实串口、内存通道或其他后端的打开、读写和关闭差异。
 */
interface SerialChannel {
    /** 当前串口设备文件路径，例如 `/dev/ttyS7`，用于日志、注册和重连。 */
    val portPath: String

    /** 当前通道使用的串口参数，探测和正式绑定必须保持同一份配置。 */
    val config: SerialConfig

    /** 通道是否已经打开，探测流程会据此避免重复 open。 */
    val isOpen: Boolean

    /** 打开底层串口资源；失败时直接抛出异常，由探测或运行时转换成状态变化。 */
    suspend fun open()

    /** 关闭底层串口资源；调用方可重复调用，具体实现应尽量保证幂等。 */
    suspend fun close()

    /** 向串口写入完整命令帧，空数组由实现自行忽略或快速返回。 */
    suspend fun write(data: ByteArray)

    /** 在指定超时时间内读取当前可用字节；超时无数据时返回空数组。 */
    suspend fun read(timeoutMs: Long): ByteArray
}
