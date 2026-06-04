package com.holderzone.device.core.channel

import com.holderzone.device.api.base.channel.SerialChannel
import com.holderzone.device.api.base.strategy.PollingCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Auth：ligen26
 * CrateTime：2026/6/4 11:12
 * Description: 问答式设备轮询写循环，由 core 统一调度 driver 声明的轮询命令。
 */
class SerialPollingLoop(
    private val channel: SerialChannel,
    private val commands: List<PollingCommand>,
    private val scope: CoroutineScope,
    private val onError: suspend (Throwable) -> Unit = {},
) {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    fun start() {
        if (isRunning || commands.isEmpty()) return
        job = scope.launch {
            while (isActive && channel.isOpen) {
                for (command in commands) {
                    if (!isActive || !channel.isOpen) return@launch
                    try {
                        channel.write(command.payload)
                        delay(command.intervalMs)
                    } catch (throwable: Throwable) {
                        onError(throwable)
                        return@launch
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
