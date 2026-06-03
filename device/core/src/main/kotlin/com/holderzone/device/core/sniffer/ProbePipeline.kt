package com.holderzone.device.core.sniffer

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IDeviceDriver

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 探测流水线，负责打开通道、发送驱动探测帧、读取响应并转换为探测结果。
 */
class ProbePipeline {
    /** 打开候选通道、发送驱动探测帧并读取响应，所有异常都会被包装成 ProbeResult.Error。 */
    suspend fun probe(driver: IDeviceDriver, channel: SerialChannel, timeoutMs: Long = 500L): ProbeResult {
        return try {
            if (!channel.isOpen) {
                channel.open()
            }
            channel.write(driver.probeStrategy.buildProbeFrame())
            // 探测阶段只读一次当前响应，是否足够完整由各驱动的 validateResponse 判断。
            val response = channel.read(timeoutMs)
            driver.probeStrategy.validateResponse(response)
        } catch (throwable: Throwable) {
            ProbeResult.Error(
                reason = throwable.message ?: "Probe failed.",
                cause = throwable,
            )
        }
    }
}
