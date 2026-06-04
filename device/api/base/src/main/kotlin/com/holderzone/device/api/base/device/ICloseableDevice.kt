package com.holderzone.device.api.base.device

/**
 * Auth：ligen26
 * CrateTime：2026/6/3 18:10
 * Description: 可选设备关闭钩子，用于释放 driver 内部持有的轮询任务、附属串口或厂商 SDK 资源。
 */
interface ICloseableDevice : IDevice {
    suspend fun close()
}
