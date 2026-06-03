package com.holderzone.device.core.strategy

import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 策略匹配器，基于已有响应在驱动列表中寻找第一个探测成功的驱动。
 */
class StrategyMatcher {
    fun match(drivers: List<IDeviceDriver>, response: ByteArray): IDeviceDriver? {
        return drivers.firstOrNull { driver ->
            driver.probeStrategy.validateResponse(response) is ProbeResult.Matched
        }
    }
}
