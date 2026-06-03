package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备通信模式枚举，用于区分主动上报设备和被动请求响应设备的运行策略。
 */
enum class CommunicationMode {
    ACTIVE_REPORT,
    PASSIVE_RESPONSE,
}
