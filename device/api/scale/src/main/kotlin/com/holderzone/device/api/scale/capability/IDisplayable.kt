package com.holderzone.device.api.scale.capability

import com.holderzone.device.api.scale.model.DisplayContent

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 显示能力接口，定义向设备屏幕写入内容和清屏的动作。
 */
interface IDisplayable {
    suspend fun display(content: DisplayContent)

    suspend fun clearDisplay()
}
