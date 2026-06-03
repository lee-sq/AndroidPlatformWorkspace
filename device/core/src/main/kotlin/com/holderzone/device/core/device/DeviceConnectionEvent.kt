package com.holderzone.device.core.device

import com.holderzone.device.api.base.model.ConnectionState

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备连接事件密封类，向上层广播绑定、状态变化、解绑和清空事件。
 */
sealed class DeviceConnectionEvent {
    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 设备绑定事件，表示设备已经完成初始化并写入注册表。
     */
    data class Bound(
        val record: DeviceRecord,
    ) : DeviceConnectionEvent()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 设备状态变化事件，携带旧状态、新状态和当前设备记录。
     */
    data class StateChanged(
        val deviceId: String,
        val previousState: ConnectionState?,
        val state: ConnectionState,
        val record: DeviceRecord?,
    ) : DeviceConnectionEvent()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 设备解绑事件，表示设备记录已从注册表移除。
     */
    data class Unbound(
        val deviceId: String,
        val record: DeviceRecord,
    ) : DeviceConnectionEvent()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 设备管理器清空事件，表示所有驱动、设备和状态缓存都已重置。
     */
    data object Cleared : DeviceConnectionEvent()
}
