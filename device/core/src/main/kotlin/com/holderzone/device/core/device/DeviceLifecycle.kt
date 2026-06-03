package com.holderzone.device.core.device

import com.holderzone.device.api.base.model.ConnectionState

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备生命周期状态辅助类，集中表达绑定成功和故障后的默认状态迁移。
 */
class DeviceLifecycle {
    fun nextAfterBind(): ConnectionState = ConnectionState.CONNECTED

    fun nextAfterFailure(): ConnectionState = ConnectionState.DEGRADED
}
