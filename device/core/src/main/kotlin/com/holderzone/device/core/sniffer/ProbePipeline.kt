package com.holderzone.device.core.sniffer

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.model.CommunicationMode
import com.holderzone.device.api.base.model.ProbeResult
import com.holderzone.device.api.base.strategy.IDeviceDriver
import com.holderzone.device.api.base.strategy.ISelfManagedProbeStrategy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 探测流水线，负责打开通道、发送驱动探测帧、读取响应并转换为探测结果。
 */
class ProbePipeline {
    /** 打开候选通道、发送驱动探测帧并在观察窗口内累积响应，所有异常都会被包装成 ProbeResult.Error。 */
    suspend fun probe(driver: IDeviceDriver, channel: SerialChannel): ProbeResult {
        return try {
            if (!channel.isOpen) {
                channel.open()
            }
            val settleDelayMs = driver.descriptor.probeSettleDelayMs
            if (settleDelayMs > 0L) {
                delay(settleDelayMs)
            }
            val probeFrame = driver.probeStrategy.buildProbeFrame()
            channel.write(probeFrame)

            val timeoutMs = driver.descriptor.probeTimeoutMs
                ?: driver.descriptor.communicationMode.defaultProbeTimeoutMs()
            if (driver.descriptor.selfManagedConnection) {
                val selfManagedProbe = driver.probeStrategy as? ISelfManagedProbeStrategy
                return selfManagedProbe?.validateChannel(channel)
                    ?: driver.probeStrategy.validateResponse(ByteArray(0))
            }
            val attempts = probeAttempts(timeoutMs)
            val responseBuffer = mutableListOf<Byte>()

            repeat(attempts) {
                if (!currentCoroutineContext().isActive) return ProbeResult.Mismatched

                val data = channel.read(timeoutMs = PROBE_READ_SLICE_MS)
                if (data.isNotEmpty()) {
                    responseBuffer += data.toList()
                    when (val result = driver.probeStrategy.validateResponse(responseBuffer.toByteArray())) {
                        is ProbeResult.Matched,
                        is ProbeResult.Error,
                        -> return result
                        ProbeResult.Mismatched -> Unit
                    }
                } else {
                    delay(PROBE_IDLE_DELAY_MS)
                }
            }

            ProbeResult.Mismatched
        } catch (throwable: Throwable) {
            ProbeResult.Error(
                reason = throwable.message ?: "Probe failed.",
                cause = throwable,
            )
        }
    }

    private fun probeAttempts(timeoutMs: Long): Int {
        val timeout = timeoutMs.coerceAtLeast(PROBE_READ_SLICE_MS)
        return ((timeout + PROBE_READ_SLICE_MS - 1) / PROBE_READ_SLICE_MS).toInt()
    }

    private fun CommunicationMode.defaultProbeTimeoutMs(): Long {
        return when (this) {
            CommunicationMode.ACTIVE_REPORT -> DEFAULT_ACTIVE_REPORT_PROBE_TIMEOUT_MS
            CommunicationMode.PASSIVE_RESPONSE -> DEFAULT_PASSIVE_RESPONSE_PROBE_TIMEOUT_MS
        }
    }

    companion object {
        private const val DEFAULT_PASSIVE_RESPONSE_PROBE_TIMEOUT_MS = 500L
        private const val DEFAULT_ACTIVE_REPORT_PROBE_TIMEOUT_MS = 3_000L
        private const val PROBE_READ_SLICE_MS = 100L
        private const val PROBE_IDLE_DELAY_MS = 10L
    }
}
