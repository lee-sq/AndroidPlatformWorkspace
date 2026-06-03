package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 串口参数模型，描述波特率、数据位、停止位和校验位。
 */
data class SerialConfig(
    val baudRate: Int,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Parity = Parity.NONE,
)
