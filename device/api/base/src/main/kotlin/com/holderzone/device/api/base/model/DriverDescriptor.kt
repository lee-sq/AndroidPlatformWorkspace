package com.holderzone.device.api.base.model

import kotlin.reflect.KClass

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 驱动描述元数据，声明驱动匹配优先级、串口配置、通信模式和设备能力。
 */
data class DriverDescriptor(
    /** 驱动策略唯一标识，用于注册、绑定和 deviceId 生成。 */
    val strategyId: String,
    /** 厂商名称，展示和日志使用。 */
    val vendorName: String,
    /** 设备型号，探测成功后用于描述当前硬件。 */
    val deviceModel: String,
    /** 设备领域分类。 */
    val deviceCategory: DeviceCategory,
    /** 通信模式，决定运行时是否需要看门狗监听主动上报。 */
    val communicationMode: CommunicationMode,
    /** 驱动支持的串口配置列表，自动探测会逐个尝试。 */
    val supportedConfigs: List<SerialConfig>,
    /** 优先探测的端口路径，用于加速固定串口设备识别。 */
    val preferredPortPaths: List<String> = emptyList(),
    /** 设备绑定后应该暴露给业务查询的能力接口集合。 */
    val capabilities: Set<KClass<*>>,
    /** 探测优先级，数值越小越先尝试。 */
    val priority: Int = 100,
)
