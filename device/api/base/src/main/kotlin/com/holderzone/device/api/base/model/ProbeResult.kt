package com.holderzone.device.api.base.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 设备探测结果密封类，表达驱动匹配成功、不匹配或探测异常三类结果。
 */
sealed class ProbeResult {
    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 探测成功结果，携带匹配到的设备型号以及可选固件版本和序列号。
     */
    data class Matched(
        val deviceModel: String,
        val firmwareVersion: String? = null,
        val serialNumber: String? = null,
    ) : ProbeResult()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 探测不匹配结果，表示当前响应不属于该驱动支持的设备。
     */
    data object Mismatched : ProbeResult()

    /**
     * Auth：ligen26
     * CrateTime：2026/6/2 16:48
     * Description: 探测异常结果，保留失败原因和原始异常，避免一次端口异常中断整体扫描。
     */
    data class Error(
        val reason: String,
        val cause: Throwable? = null,
    ) : ProbeResult()
}
