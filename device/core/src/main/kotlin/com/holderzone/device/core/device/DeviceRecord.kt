package com.holderzone.device.core.device

import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备注册记录，关联设备实例、驱动、聚合能力和当前连接状态。
 */
data class DeviceRecord(
    val device: IDevice,
    val driver: IDeviceDriver,
    val capabilities: Map<Class<*>, Any>,
    val state: ConnectionState,
)
