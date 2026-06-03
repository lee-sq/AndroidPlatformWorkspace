package com.holderzone.device.api.cabinet.model

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 打印机状态枚举，描述可打印、忙碌、缺纸、离线和未知状态。
 */
enum class PrinterStatus {
    READY,
    BUSY,
    PAPER_OUT,
    OFFLINE,
    UNKNOWN,
}
