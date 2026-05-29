package com.holderzone.foundation.core.ui.calendar

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isCurrentMonth: Boolean = true,
    val isToday: Boolean = false,
    val isStartDate: Boolean = false,
    val isEndDate: Boolean = false,
    val isInRange: Boolean = false,
    val isRangeStart: Boolean = false,
    val isRangeEnd: Boolean = false,
    val isRangeMiddle: Boolean = false,
    val isSingleSelected: Boolean = false,
    val isEnabled: Boolean = true,
) {
    fun toCalendar(): Calendar {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun compareTo(other: CalendarDate): Int {
        return when {
            year != other.year -> year.compareTo(other.year)
            month != other.month -> month.compareTo(other.month)
            else -> day.compareTo(other.day)
        }
    }

    fun isSameDay(other: CalendarDate): Boolean {
        return year == other.year && month == other.month && day == other.day
    }

    fun isBefore(other: CalendarDate): Boolean = compareTo(other) < 0

    fun isAfter(other: CalendarDate): Boolean = compareTo(other) > 0

    fun formatDate(pattern: String = DATE_PATTERN): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(toCalendar().time)
    }

    companion object {
        const val DATE_PATTERN = "yyyy-MM-dd"

        fun fromCalendar(calendar: Calendar): CalendarDate {
            return CalendarDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
            )
        }

        fun today(): CalendarDate = fromCalendar(Calendar.getInstance()).copy(isToday = true)

        fun fromString(dateString: String?): CalendarDate? {
            if (dateString.isNullOrBlank()) return null
            return try {
                val date = SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).parse(dateString)
                    ?: return null
                Calendar.getInstance().apply { time = date }.let(::fromCalendar)
            } catch (_: ParseException) {
                null
            }
        }
    }
}
