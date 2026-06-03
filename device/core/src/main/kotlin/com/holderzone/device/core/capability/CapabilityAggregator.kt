package com.holderzone.device.core.capability

import com.holderzone.device.api.base.device.IDevice
import kotlin.reflect.KClass

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 能力聚合器，根据驱动声明的能力类型从设备实例上提取可查询能力。
 */
class CapabilityAggregator {
    /** 只收集设备实例真正实现的能力，避免 descriptor 声明和实现不一致时暴露错误能力。 */
    fun aggregate(device: IDevice, capabilityTypes: Set<KClass<*>>): Map<Class<*>, Any> {
        return capabilityTypes.mapNotNull { capabilityType ->
            val javaType = capabilityType.java
            if (javaType.isInstance(device)) {
                javaType to device
            } else {
                null
            }
        }.toMap()
    }
}
