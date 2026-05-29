package com.holderzone.foundation.core.ui.widget

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.holderzone.foundation.core.R

data class NumericKeypadPalette(
    @param:DrawableRes val surfaceBackground: Int,
    @param:DrawableRes val keyBackground: Int,
    @param:ColorInt val keyTextColor: Int,
    @param:ColorInt val deleteTextColor: Int,
    @param:ColorInt val overlayColor: Int,
)

object NumericKeypadThemeSpec {
    fun palette(context: Context): NumericKeypadPalette {
        return NumericKeypadPalette(
            surfaceBackground = R.drawable.bg_foundation_keypad_surface,
            keyBackground = R.drawable.bg_foundation_keypad_key,
            keyTextColor = ContextCompat.getColor(context, R.color.foundation_text_primary),
            deleteTextColor = ContextCompat.getColor(context, R.color.foundation_text_muted),
            overlayColor = ContextCompat.getColor(context, R.color.foundation_overlay_dim),
        )
    }
}
