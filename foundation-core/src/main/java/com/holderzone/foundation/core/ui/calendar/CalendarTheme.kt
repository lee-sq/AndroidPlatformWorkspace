package com.holderzone.foundation.core.ui.calendar

import android.content.Context
import androidx.core.content.ContextCompat
import com.holderzone.foundation.core.R

object CalendarThemeSpec {
    fun palette(context: Context): CalendarPalette {
        return CalendarPalette(
            pickerBackground = R.drawable.bg_foundation_picker_sheet,
            weekdayBackground = R.drawable.bg_foundation_calendar_weekday,
            selectedDayBackground = R.drawable.bg_foundation_calendar_day_selected,
            rangeStartBackground = R.drawable.bg_foundation_calendar_range_start,
            rangeMiddleBackground = R.drawable.bg_foundation_calendar_range_middle,
            rangeEndBackground = R.drawable.bg_foundation_calendar_range_end,
            dateTimeSelectionBackground = R.drawable.bg_foundation_datetime_picker_selection,
            textPrimary = ContextCompat.getColor(context, R.color.foundation_text_primary),
            textMuted = ContextCompat.getColor(context, R.color.foundation_text_muted),
            textDisabled = ContextCompat.getColor(context, R.color.foundation_text_disabled),
            brandPrimary = ContextCompat.getColor(context, R.color.foundation_primary),
            divider = ContextCompat.getColor(context, R.color.foundation_border),
            selectedText = ContextCompat.getColor(context, R.color.foundation_white),
        )
    }
}

data class CalendarPalette(
    val pickerBackground: Int = R.drawable.bg_foundation_picker_sheet,
    val weekdayBackground: Int = R.drawable.bg_foundation_calendar_weekday,
    val selectedDayBackground: Int = R.drawable.bg_foundation_calendar_day_selected,
    val rangeStartBackground: Int = R.drawable.bg_foundation_calendar_range_start,
    val rangeMiddleBackground: Int = R.drawable.bg_foundation_calendar_range_middle,
    val rangeEndBackground: Int = R.drawable.bg_foundation_calendar_range_end,
    val dateTimeSelectionBackground: Int = R.drawable.bg_foundation_datetime_picker_selection,
    val textPrimary: Int = 0,
    val textMuted: Int = 0,
    val textDisabled: Int = 0,
    val brandPrimary: Int = 0,
    val divider: Int = 0,
    val selectedText: Int = 0,
)
