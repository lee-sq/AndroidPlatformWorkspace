package com.holderzone.device.api.base.strategy

import com.holderzone.device.api.base.model.ProbeResult

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备探测策略接口，负责生成探测帧并判断串口响应是否匹配当前驱动。
 */
interface IProbeStrategy {
    /** 构建设备识别探测帧，AutoSniffer 会在每个候选端口和串口配置上发送它。 */
    fun buildProbeFrame(): ByteArray

    /** 校验探测响应并输出匹配、不匹配或错误结果，决定扫描是否绑定该驱动。 */
    fun validateResponse(response: ByteArray): ProbeResult
}
