package com.holderzone.foundation.core.ui.datetime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.DialogDateTimePickerBinding
import com.holderzone.foundation.core.ui.calendar.CalendarThemeSpec
import com.holderzone.foundation.core.ui.picker.PickerDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateTimePickerDialogFragment : PickerDialogFragment<DialogDateTimePickerBinding>() {
    private lateinit var selectedTime: Calendar
    private lateinit var maxTime: Calendar
    private val formatter = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): DialogDateTimePickerBinding {
        return DialogDateTimePickerBinding.inflate(inflater, container, false)
    }

    override fun onBindingCreated(
        binding: DialogDateTimePickerBinding,
        savedInstanceState: Bundle?,
    ) {
        maxTime = Calendar.getInstance().clearSeconds()
        selectedTime = parseInitialTime().coerceNotAfter(maxTime)
        binding.titleText.text = pickerTitle()
        setupActions(binding)
        setupWheels(binding)
        applyTheme(binding)
        renderWheels(binding)
    }

    private fun setupActions(binding: DialogDateTimePickerBinding) {
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                resultRequestKey(),
                Bundle().apply {
                    putString(RESULT_DATE_TIME, formatter.format(selectedTime.time))
                },
            )
            dismiss()
        }
    }

    private fun setupWheels(binding: DialogDateTimePickerBinding) {
        binding.yearWheel.setOnSelectionChangedListener { index ->
            updateYear(startYear() + index)
            normalizeSelectedTime()
            renderWheels(binding)
        }
        binding.monthWheel.setOnSelectionChangedListener { index ->
            updateMonth(index)
            normalizeSelectedTime()
            renderWheels(binding)
        }
        binding.dayWheel.setOnSelectionChangedListener { index ->
            selectedTime.set(Calendar.DAY_OF_MONTH, index + 1)
            normalizeSelectedTime()
            renderWheels(binding)
        }
        binding.hourWheel.setOnSelectionChangedListener { index ->
            selectedTime.set(Calendar.HOUR_OF_DAY, index)
            normalizeSelectedTime()
            renderWheels(binding)
        }
        binding.minuteWheel.setOnSelectionChangedListener { index ->
            selectedTime.set(Calendar.MINUTE, index)
            normalizeSelectedTime()
            renderWheels(binding)
        }
    }

    private fun renderWheels(binding: DialogDateTimePickerBinding) {
        val yearValues = (startYear()..maxTime.get(Calendar.YEAR)).map { "${it}年" }
        binding.yearWheel.submitItems(
            yearValues,
            selectedTime.get(Calendar.YEAR) - startYear(),
        )

        val months = 1..maxMonth()
        binding.monthWheel.submitItems(
            months.map { "${it}月" },
            selectedTime.get(Calendar.MONTH).coerceAtMost(months.last - 1),
        )

        val days = 1..maxDay()
        binding.dayWheel.submitItems(
            days.map { "${it}日" },
            selectedTime.get(Calendar.DAY_OF_MONTH).coerceAtMost(days.last) - 1,
        )

        val hours = 0..maxHour()
        binding.hourWheel.submitItems(
            hours.map { "${it}时" },
            selectedTime.get(Calendar.HOUR_OF_DAY).coerceAtMost(hours.last),
        )

        val minutes = 0..maxMinute()
        binding.minuteWheel.submitItems(
            minutes.map { "${it}分" },
            selectedTime.get(Calendar.MINUTE).coerceAtMost(minutes.last),
        )
    }

    private fun normalizeSelectedTime() {
        if (selectedTime.after(maxTime)) {
            selectedTime = maxTime.cloneCalendar()
        }
    }

    private fun updateYear(year: Int) {
        val originalDay = selectedTime.get(Calendar.DAY_OF_MONTH)
        selectedTime.set(Calendar.DAY_OF_MONTH, 1)
        selectedTime.set(Calendar.YEAR, year)
        selectedTime.set(Calendar.DAY_OF_MONTH, originalDay.coerceAtMost(selectedTime.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }

    private fun updateMonth(monthIndex: Int) {
        val originalDay = selectedTime.get(Calendar.DAY_OF_MONTH)
        selectedTime.set(Calendar.DAY_OF_MONTH, 1)
        selectedTime.set(Calendar.MONTH, monthIndex)
        selectedTime.set(Calendar.DAY_OF_MONTH, originalDay.coerceAtMost(selectedTime.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }

    private fun maxMonth(): Int {
        return if (selectedTime.get(Calendar.YEAR) == maxTime.get(Calendar.YEAR)) {
            maxTime.get(Calendar.MONTH) + 1
        } else {
            MONTHS_IN_YEAR
        }
    }

    private fun maxDay(): Int {
        val daysInMonth = selectedTime.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (
            selectedTime.get(Calendar.YEAR) == maxTime.get(Calendar.YEAR) &&
            selectedTime.get(Calendar.MONTH) == maxTime.get(Calendar.MONTH)
        ) {
            maxTime.get(Calendar.DAY_OF_MONTH).coerceAtMost(daysInMonth)
        } else {
            daysInMonth
        }
    }

    private fun maxHour(): Int {
        return if (isSameDate(selectedTime, maxTime)) {
            maxTime.get(Calendar.HOUR_OF_DAY)
        } else {
            HOURS_IN_DAY - 1
        }
    }

    private fun maxMinute(): Int {
        return if (
            isSameDate(selectedTime, maxTime) &&
            selectedTime.get(Calendar.HOUR_OF_DAY) == maxTime.get(Calendar.HOUR_OF_DAY)
        ) {
            maxTime.get(Calendar.MINUTE)
        } else {
            MINUTES_IN_HOUR - 1
        }
    }

    private fun applyTheme(binding: DialogDateTimePickerBinding) {
        val palette = CalendarThemeSpec.palette(requireContext())
        binding.pickerRoot.setBackgroundResource(palette.pickerBackground)
        binding.titleText.setTextColor(palette.textPrimary)
        binding.cancelButton.setTextColor(palette.brandPrimary)
        binding.confirmButton.setTextColor(palette.brandPrimary)
        binding.divider.setBackgroundColor(palette.divider)
        binding.selectionBackground.setBackgroundResource(palette.dateTimeSelectionBackground)
        listOf(
            binding.yearWheel,
            binding.monthWheel,
            binding.dayWheel,
            binding.hourWheel,
            binding.minuteWheel,
        ).forEach { wheel ->
            wheel.applyPalette(
                textColor = palette.textMuted,
                selectedTextColor = palette.textPrimary,
            )
        }
    }

    private fun parseInitialTime(): Calendar {
        val initialText = requireArguments().getString(ARG_DATE_TIME).orEmpty()
        val parsedDate = runCatching { formatter.parse(initialText) }.getOrNull()
        return Calendar.getInstance().apply {
            time = parsedDate ?: time
        }.clearSeconds()
    }

    private fun Calendar.clearSeconds(): Calendar {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        return this
    }

    private fun pickerTitle(): String {
        return requireArguments().getString(ARG_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.foundation_date_time_title)
    }

    private fun resultRequestKey(): String {
        return requireArguments().getString(ARG_REQUEST_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: REQUEST_KEY
    }

    private fun Calendar.coerceNotAfter(max: Calendar): Calendar {
        return if (after(max)) max.cloneCalendar() else this
    }

    private fun Calendar.cloneCalendar(): Calendar = (clone() as Calendar)

    private fun startYear(): Int {
        return maxTime.get(Calendar.YEAR) - YEAR_RANGE_OFFSET
    }

    private fun isSameDate(
        first: Calendar,
        second: Calendar,
    ): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        const val REQUEST_KEY = "foundation_date_time_picker_request"
        const val RESULT_DATE_TIME = "result_date_time"

        private const val ARG_DATE_TIME = "arg_date_time"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_REQUEST_KEY = "arg_request_key"
        private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
        private const val YEAR_RANGE_OFFSET = 10
        private const val MONTHS_IN_YEAR = 12
        private const val HOURS_IN_DAY = 24
        private const val MINUTES_IN_HOUR = 60
        private const val TAG = "DateTimePickerDialogFragment"

        fun newInstance(
            dateTime: String?,
            title: String? = null,
            requestKey: String = REQUEST_KEY,
        ): DateTimePickerDialogFragment {
            return DateTimePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATE_TIME, dateTime.orEmpty())
                    putString(ARG_TITLE, title.orEmpty())
                    putString(ARG_REQUEST_KEY, requestKey)
                }
            }
        }

        fun show(
            fragmentManager: FragmentManager,
            dateTime: String?,
            title: String? = null,
            requestKey: String = REQUEST_KEY,
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            newInstance(dateTime, title, requestKey).show(fragmentManager, TAG)
        }
    }
}
