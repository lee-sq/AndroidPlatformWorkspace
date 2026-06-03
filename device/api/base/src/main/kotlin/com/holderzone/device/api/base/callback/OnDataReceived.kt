package com.holderzone.device.api.base.callback

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备原始数据回调，给上层业务透出某个设备收到的字节数据。
 */
fun interface OnDataReceived {
    fun onDataReceived(deviceId: String, data: ByteArray)
}
