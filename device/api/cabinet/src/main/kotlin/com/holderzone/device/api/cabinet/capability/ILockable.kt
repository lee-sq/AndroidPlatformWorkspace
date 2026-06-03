package com.holderzone.device.api.cabinet.capability

import com.holderzone.device.api.cabinet.model.DoorAddress
import com.holderzone.device.api.cabinet.model.DoorState

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 门锁能力接口，定义柜门开锁、上锁和门状态查询动作。
 */
interface ILockable {
    /** 打开指定柜门或格口的锁。 */
    suspend fun unlock(address: DoorAddress): Boolean

    /** 锁定指定柜门或格口。 */
    suspend fun lock(address: DoorAddress): Boolean

    /** 查询指定柜门或格口的当前开关/锁定状态。 */
    suspend fun queryDoorState(address: DoorAddress): DoorState
}
