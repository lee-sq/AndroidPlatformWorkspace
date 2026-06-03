package com.holderzone.device.driver.cabinet.y

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.CommunicationMode
import com.holderzone.device.api.base.model.DeviceCategory
import com.holderzone.device.api.base.model.DeviceInfo
import com.holderzone.device.api.base.model.DriverDescriptor
import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ParsedData
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.api.base.strategy.IDeviceFactory
import com.holderzone.device.api.base.strategy.IFrameParser
import com.holderzone.device.api.base.strategy.IHeartbeatProvider
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.cabinet.capability.ITemperatureCtrl
import com.holderzone.device.api.cabinet.model.DoorAddress
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.capability.IDisplayable
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.DisplayContent
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: HolderZone Cabinet-Y 柜体驱动，声明柜体、称重、门锁、温控和显示能力。
 */
class CabinetYDriver : IDeviceDriver {
    /** DriverDescriptor 是自动探测和能力聚合的入口配置。 */
    override val descriptor = DriverDescriptor(
        strategyId = STRATEGY_ID,
        vendorName = "HolderZone",
        deviceModel = "Cabinet-Y",
        deviceCategory = DeviceCategory.CABINET,
        communicationMode = CommunicationMode.ACTIVE_REPORT,
        supportedConfigs = listOf(SerialConfig(baudRate = 38_400)),
        capabilities = setOf(
            IWeighable::class,
            ILockable::class,
            ITemperatureCtrl::class,
            IDisplayable::class,
        ),
        priority = 20,
    )

    override val probeStrategy: IProbeStrategy = CabinetYProbeStrategy()
    override val heartbeatProvider: IHeartbeatProvider = CabinetYHeartbeatProvider()
    override val frameParser: IFrameParser = CabinetYFrameParser()
    override val deviceFactory: IDeviceFactory = CabinetYFactory()

    companion object {
        const val STRATEGY_ID = "cabinet.holderzone.cabinet-y"
    }
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Cabinet-Y 探测策略，发送识别帧并通过响应关键字判断设备型号。
 */
private class CabinetYProbeStrategy : IProbeStrategy {
    /** 发送 Cabinet-Y 识别命令，真实协议接入时应替换为厂商探测帧。 */
    override fun buildProbeFrame(): ByteArray = "CABINET_Y_PROBE".encodeToByteArray()

    /** 当前根据响应关键字识别设备，未包含关键字则继续尝试其他驱动。 */
    override fun validateResponse(response: ByteArray): ProbeResult {
        return if (response.decodeToString().contains("CABINET_Y")) {
            ProbeResult.Matched(deviceModel = "Cabinet-Y")
        } else {
            ProbeResult.Mismatched
        }
    }
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Cabinet-Y 心跳策略，定期发送心跳命令并检查 OK 响应。
 */
private class CabinetYHeartbeatProvider : IHeartbeatProvider {
    override val heartbeatIntervalMs: Long = 5_000L

    /** 主动上报型柜体的保活命令，后续可替换为真实厂商心跳帧。 */
    override fun buildHeartbeatCommand(): ByteArray = "CABINET_Y_HEARTBEAT".encodeToByteArray()

    /** 响应中包含 OK 即认为柜体链路正常。 */
    override fun parseHeartbeatResponse(data: ByteArray): Boolean {
        return data.decodeToString().contains("OK")
    }
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Cabinet-Y 帧解析器，占位处理柜体主动上报数据并输出帧大小信息。
 */
private class CabinetYFrameParser : IFrameParser {
    /** 占位实现把当前读到的字节整体视为一帧，真实协议接入时在这里处理半包和帧头。 */
    override fun extractFrame(raw: ByteArray): Frame? = Frame(payload = raw)

    /** 输出帧大小便于调试，真实协议可解析门锁、称重、温控等主动上报字段。 */
    override fun parseFrame(frame: Frame): ParsedData? = ParsedData(
        type = "cabinet-y-frame",
        fields = mapOf("size" to frame.payload.size),
    )
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Cabinet-Y 设备工厂，在通道初始化后创建柜体设备实例。
 */
private class CabinetYFactory : IDeviceFactory {
    /** Cabinet-Y 当前无额外初始化命令，通道打开即可创建设备。 */
    override suspend fun initialize(channel: SerialChannel): Boolean = true

    override fun createDevice(channel: SerialChannel): IDevice = CabinetYDevice(channel.portPath)
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: Cabinet-Y 设备实现，聚合称重、门锁、温控和显示能力的占位动作。
 */
private class CabinetYDevice(
    portPath: String,
) : IDevice, IWeighable, ILockable, ITemperatureCtrl, IDisplayable {
    /** deviceId 使用 strategyId + portPath，确保同型号多端口设备也能区分。 */
    override val info = DeviceInfo(
        deviceId = "${CabinetYDriver.STRATEGY_ID}:$portPath",
        strategyId = CabinetYDriver.STRATEGY_ID,
        vendorName = "HolderZone",
        deviceModel = "Cabinet-Y",
        deviceCategory = DeviceCategory.CABINET,
    )

    override suspend fun weigh(): WeightResult = WeightResult(
        value = 0.0,
        unit = WeightUnit.GRAM,
        stable = false,
    )

    override fun observeWeight(): Flow<WeightResult> = flowOf(
        WeightResult(
            value = 0.0,
            unit = WeightUnit.GRAM,
            stable = false,
        ),
    )

    override suspend fun tare() = Unit

    override suspend fun zero() = Unit

    override suspend fun unlock(address: DoorAddress): Boolean = true

    override suspend fun lock(address: DoorAddress): Boolean = true

    override suspend fun queryDoorState(address: DoorAddress): DoorState = DoorState.UNKNOWN

    override suspend fun setTemperature(celsius: Double): Boolean = true

    override suspend fun getTemperature(): TemperatureReading = TemperatureReading(celsius = 0.0)

    override suspend fun display(content: DisplayContent) = Unit

    override suspend fun clearDisplay() = Unit
}
