package com.holderzone.device.api.scale.capability

import com.holderzone.device.api.scale.model.CalibrationResult

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 17:30
 * Description: 称重标定能力接口，定义以标准重量修正后续称重结果的动作。
 */
interface ICalibratable {
    suspend fun calibrate(standardWeightGrams: Double): CalibrationResult
}
