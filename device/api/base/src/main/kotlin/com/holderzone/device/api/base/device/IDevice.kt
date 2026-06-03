package com.holderzone.device.api.base.device

import com.holderzone.device.api.base.model.DeviceInfo

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 所有设备实例的最小公共契约，提供设备身份信息供注册表和业务查询。
 */
interface IDevice {
    /** 设备身份信息，是设备注册、能力查询、状态流和事件广播的共同索引来源。 */
    val info: DeviceInfo
}
