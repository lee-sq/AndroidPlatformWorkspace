package com.holderzone.device.api.scale.capability

import com.holderzone.device.api.scale.model.WeightResult
import kotlinx.coroutines.flow.Flow

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 称重能力接口，定义单次称重、流式重量监听、去皮和置零动作。
 */
interface IWeighable {
    /** 主动读取一次重量；主动上报型设备通常返回最近一次稳定或最新上报值。 */
    suspend fun weigh(): WeightResult

    /** 观察设备连续上报的重量变化，适合实时称重 UI。 */
    fun observeWeight(): Flow<WeightResult>

    /** 去皮，把当前载荷作为皮重扣除。 */
    suspend fun tare()

    /** 置零，把当前秤面状态设为零点。 */
    suspend fun zero()
}
