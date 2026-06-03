package com.holderzone.device.driver.scale.ly

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.device.IDevice
import com.holderzone.device.api.base.model.CommunicationMode
import com.holderzone.device.api.base.model.DeviceCategory
import com.holderzone.device.api.base.model.DeviceInfo
import com.holderzone.device.api.base.model.DriverDescriptor
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.api.base.strategy.IDeviceFactory
import com.holderzone.device.api.base.strategy.IFrameParser
import com.holderzone.device.api.base.strategy.IHeartbeatProvider
import com.holderzone.device.api.base.strategy.IProbeStrategy
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.driver.scale.constant.VendorName
import com.holderzone.device.driver.scale.ly.protocol.LyScaleFrameParser
import com.holderzone.device.driver.scale.ly.protocol.LyScaleProbeStrategy
import com.holderzone.device.driver.scale.ly.protocol.LyScaleProtocol

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 16:48
 * Description: 亮悦电子秤驱动，适配 9600 波特率主动上报 ASCII 称重协议。
 */
class LyScaleDriver(
    private val parser: LyScaleFrameParser = LyScaleFrameParser(),
    private val protocol: LyScaleProtocol = LyScaleProtocol,
) : IDeviceDriver {
    /** LY 称重默认 9600 波特率，常见端口沿用历史项目候选顺序。 */
    override val descriptor: DriverDescriptor = DriverDescriptor(
        strategyId = STRATEGY_ID,
        vendorName = VendorName.LY,
        deviceModel = DEVICE_MODEL,
        deviceCategory = DeviceCategory.SCALE,
        communicationMode = CommunicationMode.ACTIVE_REPORT,
        supportedConfigs = listOf(SerialConfig(baudRate = DEFAULT_BAUD_RATE)),
        preferredPortPaths = DEFAULT_PORT_PATHS,
        capabilities = setOf(IWeighable::class),
        priority = 20,
    )

    override val probeStrategy: IProbeStrategy = LyScaleProbeStrategy(protocol)

    override val heartbeatProvider: IHeartbeatProvider? = null

    override val frameParser: IFrameParser = parser

    override val deviceFactory: IDeviceFactory = LyScaleFactory(
        parser = parser,
        protocol = protocol,
    )

    companion object {
        const val STRATEGY_ID = "scale.liangyue.ascii"
        const val DEVICE_MODEL = "LY"
        const val DEFAULT_BAUD_RATE = 9_600

        val DEFAULT_PORT_PATHS: List<String> = listOf(
            "/dev/ttyS4",
            "/dev/ttyS0"
        )
    }
}

private class LyScaleFactory(
    private val parser: LyScaleFrameParser,
    private val protocol: LyScaleProtocol,
) : IDeviceFactory {
    /** LY 主动上报重量，绑定时不需要额外初始化命令。 */
    override suspend fun initialize(channel: SerialChannel): Boolean = true

    override fun createDevice(channel: SerialChannel): IDevice {
        return LyScaleDevice(
            portPath = channel.portPath,
            channel = channel,
            parser = parser,
            protocol = protocol,
        )
    }
}

private class LyScaleDevice(
    portPath: String,
    private val channel: SerialChannel,
    private val parser: LyScaleFrameParser,
    private val protocol: LyScaleProtocol,
) : IDevice, IWeighable {
    override val info: DeviceInfo = DeviceInfo(
        deviceId = "${LyScaleDriver.STRATEGY_ID}:$portPath",
        strategyId = LyScaleDriver.STRATEGY_ID,
        vendorName = VendorName.LY,
        deviceModel = LyScaleDriver.DEVICE_MODEL,
        deviceCategory = DeviceCategory.SCALE,
    )

    override suspend fun weigh() = parser.latestWeight()

    override fun observeWeight() = parser.observeWeight()

    override suspend fun tare() {
        channel.write(protocol.tareCommand)
    }

    override suspend fun zero() {
        channel.write(protocol.zeroCommand)
    }
}
