package com.holderzone.device.api.cabinet.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 柜门地址模型，用 0-based 大门序号和可选小格口序号定位柜门。
 */
data class DoorAddress(
    val primaryIndex: Int,
    val compartmentIndex: Int? = null,
) {
    init {
        require(primaryIndex >= 0) { "primaryIndex must be greater than or equal to 0." }
        require(compartmentIndex == null || compartmentIndex >= 0) {
            "compartmentIndex must be greater than or equal to 0."
        }
    }

    val isCompartment: Boolean
        get() = compartmentIndex != null

    companion object {
        fun primary(index: Int): DoorAddress = DoorAddress(primaryIndex = index)

        fun compartment(primaryIndex: Int, compartmentIndex: Int): DoorAddress {
            return DoorAddress(
                primaryIndex = primaryIndex,
                compartmentIndex = compartmentIndex,
            )
        }
    }
}
