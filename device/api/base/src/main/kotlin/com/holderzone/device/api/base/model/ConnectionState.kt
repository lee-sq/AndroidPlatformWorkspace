package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备连接生命周期状态枚举，描述探测、连接、降级、重连和失败等阶段。
 */
enum class ConnectionState {
    DISCONNECTED,
    SNIFFING,
    CONNECTING,
    CONNECTED,
    DEGRADED,
    RECONNECTING,
    FAILED,
}
