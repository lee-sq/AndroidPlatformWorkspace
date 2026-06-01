package com.holderzone.foundation.core.platform.tts

import java.util.Locale

data class TtsOptions(
    val speechRate: Float = 0.9f,
    val pitch: Float = 0.9f,
    val locale: Locale = Locale.CHINESE,
)
