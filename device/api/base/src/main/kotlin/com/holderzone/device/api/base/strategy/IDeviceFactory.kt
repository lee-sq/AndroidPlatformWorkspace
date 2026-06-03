package com.holderzone.device.api.base.strategy

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备实例工厂接口，负责在通道打开后完成初始化并创建实际设备对象。
 */
interface IDeviceFactory {
    /** 在通道打开后执行设备初始化命令；返回 false 表示该设备不能进入绑定流程。 */
    suspend fun initialize(channel: SerialChannel): Boolean

    /** 基于已初始化的通道创建设备实例，实例应实现 descriptor 中声明的能力接口。 */
    fun createDevice(channel: SerialChannel): IDevice
}
