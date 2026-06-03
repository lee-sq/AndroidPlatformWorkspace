package com.holderzone.device.core.strategy

import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 驱动策略注册表，按 strategyId 管理驱动并按优先级输出探测顺序。
 */
class StrategyRegistry {
    private val drivers = linkedMapOf<String, IDeviceDriver>()

    /** 注册或覆盖同 strategyId 的驱动。 */
    fun register(driver: IDeviceDriver) {
        drivers[driver.descriptor.strategyId] = driver
    }

    /** 从后续自动探测候选集中移除指定驱动。 */
    fun unregister(strategyId: String) {
        drivers.remove(strategyId)
    }

    fun find(strategyId: String): IDeviceDriver? = drivers[strategyId]

    /** 返回稳定排序后的驱动列表，priority 越小越先被探测。 */
    fun all(): List<IDeviceDriver> {
        return drivers.values.sortedWith(
            compareBy<IDeviceDriver> { it.descriptor.priority }
                .thenBy { it.descriptor.strategyId },
        )
    }

    fun clear() {
        drivers.clear()
    }
}
