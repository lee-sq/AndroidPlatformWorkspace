package com.holderzone.device.api.cabinet.capability

import com.holderzone.device.api.cabinet.model.PrintContent
import com.holderzone.device.api.cabinet.model.PrintResult
import com.holderzone.device.api.cabinet.model.PrinterStatus

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 打印能力接口，定义柜体内置打印机的打印和状态查询动作。
 */
interface IPrintable {
    suspend fun print(content: PrintContent): PrintResult

    suspend fun getPrinterStatus(): PrinterStatus
}
