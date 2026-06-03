package com.holderzone.device.api.base.strategy

import com.holderzone.device.api.base.model.Frame
import com.holderzone.device.api.base.model.ParsedData

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: 协议帧解析接口，负责从连续字节流中切出完整帧并解析为结构化数据。
 */
interface IFrameParser {
    /** 从当前累计的原始字节中尝试切出一帧；数据不足或无合法帧时返回 null。 */
    fun extractFrame(raw: ByteArray): Frame?

    /** 解析一帧完整协议数据，并可在实现内部更新状态流或缓存最新业务读数。 */
    fun parseFrame(frame: Frame): ParsedData?
}
