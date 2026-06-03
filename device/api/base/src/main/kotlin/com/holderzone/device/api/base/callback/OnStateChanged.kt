package com.holderzone.device.api.base.callback

import com.holderzone.device.api.base.model.ConnectionState

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备连接状态变化回调，通知上层业务指定设备的状态迁移。
 */
fun interface OnStateChanged {
    fun onStateChanged(deviceId: String, state: ConnectionState)
}
