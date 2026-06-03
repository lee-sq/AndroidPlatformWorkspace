package com.holderzone.device.api.base.strategy

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 心跳策略接口，为需要主动保活的设备提供心跳命令和响应判定。
 */
interface IHeartbeatProvider {
    /** 两次心跳之间的建议间隔，运行时或调度器可按此频率发送保活命令。 */
    val heartbeatIntervalMs: Long

    /** 构建设备心跳命令；返回 null 表示该设备只依赖上报帧喂狗，不主动发送心跳。 */
    fun buildHeartbeatCommand(): ByteArray?

    /** 判断心跳响应是否有效，false 通常意味着设备链路异常或型号不匹配。 */
    fun parseHeartbeatResponse(data: ByteArray): Boolean
}
