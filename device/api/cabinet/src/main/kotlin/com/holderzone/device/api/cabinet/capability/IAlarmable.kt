package com.holderzone.device.api.cabinet.capability

import com.holderzone.device.api.cabinet.model.AlarmState

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 告警能力接口，定义柜体类设备查询和确认告警状态的能力。
 */
interface IAlarmable {
    suspend fun getAlarmState(): AlarmState

    suspend fun acknowledgeAlarm(): Boolean
}
