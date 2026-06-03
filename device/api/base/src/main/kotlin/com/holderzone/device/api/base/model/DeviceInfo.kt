package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备身份信息模型，记录设备唯一标识、驱动策略、厂商、型号和可选固件信息。
 */
data class DeviceInfo(
    val deviceId: String,
    val strategyId: String,
    val vendorName: String,
    val deviceModel: String,
    val deviceCategory: DeviceCategory,
    val firmwareVersion: String? = null,
    val serialNumber: String? = null,
)
