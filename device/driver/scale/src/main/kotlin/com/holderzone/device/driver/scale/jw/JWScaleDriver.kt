package com.holderzone.device.driver.scale.jw

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
import com.holderzone.device.driver.scale.jw.protocol.JWScale3568FrameParser
import com.holderzone.device.driver.scale.jw.protocol.JWScale3568ProbeStrategy
import com.holderzone.device.driver.scale.jw.protocol.JWScale3568Protocol

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 主板电子秤驱动，声明主动上报模式、默认串口和称重能力。
 */
class JWScaleDriver(
    private val parser: JWScale3568FrameParser = JWScale3568FrameParser(),
    private val protocol: JWScale3568Protocol = JWScale3568Protocol,
) : IDeviceDriver {
    /** 精卫 3568 默认使用 /dev/ttyS7 和 19200 波特率，优先级高于通用驱动。 */
    override val descriptor: DriverDescriptor = DriverDescriptor(
        strategyId = STRATEGY_ID,
        vendorName = VendorName.JW,
        deviceModel = DEVICE_MODEL,
        deviceCategory = DeviceCategory.SCALE,
        communicationMode = CommunicationMode.ACTIVE_REPORT,
        supportedConfigs = listOf(SerialConfig(baudRate = DEFAULT_BAUD_RATE)),
        preferredPortPaths = DEFAULT_PORT_PATH,
        capabilities = setOf(IWeighable::class),
        priority = 10,
    )

    override val probeStrategy: IProbeStrategy = JWScale3568ProbeStrategy(protocol)

    override val heartbeatProvider: IHeartbeatProvider? = null

    override val frameParser: IFrameParser = parser

    override val deviceFactory: IDeviceFactory = JWScale3568Factory(
        parser = parser,
        protocol = protocol,
    )

    companion object {
        const val STRATEGY_ID = "scale.jingwei"
        const val DEVICE_MODEL = "JW"
        val DEFAULT_PORT_PATH = listOf("/dev/ttyS7", "/dev/ttyS2")
        const val DEFAULT_BAUD_RATE = 19_200
    }
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 设备工厂，绑定前读取参数并创建称重设备实例。
 */
private class JWScale3568Factory(
    private val parser: JWScale3568FrameParser,
    private val protocol: JWScale3568Protocol,
) : IDeviceFactory {
    /** 绑定前读取一次参数，让设备进入可持续上报的正常工作状态。 */
    override suspend fun initialize(channel: SerialChannel): Boolean {
        channel.write(protocol.readParameterCommand)
        return true
    }

    /** 创建设备实例时复用同一个 parser，使读循环解析结果能被称重能力读取。 */
    override fun createDevice(channel: SerialChannel): IDevice {
        return JWScale3568Device(
            portPath = channel.portPath,
            channel = channel,
            parser = parser,
            protocol = protocol,
        )
    }
}

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 精卫 3568 电子秤设备实现，提供实时称重、去皮和置零动作。
 */
private class JWScale3568Device(
    portPath: String,
    private val channel: SerialChannel,
    private val parser: JWScale3568FrameParser,
    private val protocol: JWScale3568Protocol,
) : IDevice, IWeighable {
    /** deviceId 使用 strategyId + portPath，支持同型号电子秤接入不同串口。 */
    override val info: DeviceInfo = DeviceInfo(
        deviceId = "${JWScaleDriver.STRATEGY_ID}:$portPath",
        strategyId = JWScaleDriver.STRATEGY_ID,
        vendorName = VendorName.JW,
        deviceModel = JWScaleDriver.DEVICE_MODEL,
        deviceCategory = DeviceCategory.SCALE,
    )

    /** 返回最近一次主动上报重量；若尚未收到帧，会等待 singleReadTimeoutMs。 */
    override suspend fun weigh() = parser.latestWeight()

    /** 暴露实时重量流，业务 UI 可 collect 以展示电子秤主动上报结果。 */
    override fun observeWeight() = parser.observeWeight()

    /** 发送去皮命令，设备后续上报重量会扣除当前皮重。 */
    override suspend fun tare() {
        channel.write(protocol.buildTareCommand())
    }

    /** 发送置零命令，要求秤体把当前重量作为零点。 */
    override suspend fun zero() {
        channel.write(protocol.buildZeroCommand())
    }
}
