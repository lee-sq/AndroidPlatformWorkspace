package com.holderzone.device.api.base.strategy

import com.holderzone.device.api.base.model.DriverDescriptor

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备驱动聚合接口，把描述信息、探测、心跳、帧解析和设备工厂组成一个可注册策略。
 */
interface IDeviceDriver {
    /** 驱动静态描述，决定探测顺序、可用串口配置、设备类别和能力集合。 */
    val descriptor: DriverDescriptor

    /** 探测策略，AutoSniffer 会用它生成探测帧并校验响应是否属于该设备。 */
    val probeStrategy: IProbeStrategy

    /** 可选心跳策略；没有心跳命令的主动上报设备可以返回 null。 */
    val heartbeatProvider: IHeartbeatProvider?

    /** 协议帧解析器，运行时读循环依赖它从连续字节流中切出并解析设备帧。 */
    val frameParser: IFrameParser

    /** 设备工厂，负责通道初始化和创建最终暴露给业务层的 IDevice 实例。 */
    val deviceFactory: IDeviceFactory
}
