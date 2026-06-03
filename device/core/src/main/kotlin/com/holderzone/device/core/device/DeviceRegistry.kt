package com.holderzone.device.core.device

import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备注册表，按 deviceId 保存已绑定设备及其驱动、能力和状态。
 */
class DeviceRegistry {
    private val records = linkedMapOf<String, DeviceRecord>()

    fun bind(
        device: IDevice,
        driver: IDeviceDriver,
        capabilities: Map<Class<*>, Any>,
        state: ConnectionState = ConnectionState.CONNECTED,
    ) {
        records[device.info.deviceId] = DeviceRecord(
            device = device,
            driver = driver,
            capabilities = capabilities,
            state = state,
        )
    }

    fun updateState(deviceId: String, state: ConnectionState) {
        val record = records[deviceId] ?: return
        records[deviceId] = record.copy(state = state)
    }

    fun find(deviceId: String): DeviceRecord? = records[deviceId]

    fun all(): List<DeviceRecord> = records.values.toList()

    fun unbind(deviceId: String): DeviceRecord? = records.remove(deviceId)

    fun clear() {
        records.clear()
    }
}
