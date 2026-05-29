package com.holderzone.foundation.core.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.holderzone.foundation.core.databinding.DialogDateRangePickerBinding
import com.holderzone.foundation.core.ui.picker.PickerDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateRangePickerDialogFragment : PickerDialogFragment<DialogDateRangePickerBinding>() {
    private var leftCalendar: Calendar = Calendar.getInstance()
    private var rightCalendar: Calendar = Calendar.getInstance()
    private var startDate: CalendarDate? = null
    private var endDate: CalendarDate? = null
    private lateinit var palette: CalendarPalette
    private lateinit var leftAdapter: CalendarRangeAdapter
    private lateinit var rightAdapter: CalendarRangeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): DialogDateRangePickerBinding {
        return DialogDateRangePickerBinding.inflate(inflater, container, false)
    }

    override fun onBindingCreated(
        binding: DialogDateRangePickerBinding,
        savedInstanceState: Bundle?,
    ) {
        palette = CalendarThemeSpec.palette(requireContext())
        binding.titleText.text = pickerTitle()
        startDate = CalendarDate.fromString(requireArguments().getString(ARG_START_DATE))
        endDate = CalendarDate.fromString(requireArguments().getString(ARG_END_DATE))
        initCalendars()
        setupAdapters(binding)
        setupActions(binding)
        applyTheme(binding)
        updateCalendarDisplay(binding)
    }

    private fun initCalendars() {
        val start = startDate
        val end = endDate
        when {
            start != null && end != null && (start.year != end.year || start.month != end.month) -> {
                leftCalendar = start.toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1) }
                rightCalendar = end.toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1) }
            }

            start != null -> setupAroundSelectedMonth(start)

            else -> {
                leftCalendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                rightCalendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, 1)
                }
            }
        }
    }

    private fun setupAroundSelectedMonth(selectedDate: CalendarDate) {
        val selectedCalendar = selectedDate.toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val currentCalendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        if (selectedCalendar.after(currentCalendar)) {
            leftCalendar = selectedCalendar.cloneCalendar().apply { add(Calendar.MONTH, -1) }
            rightCalendar = selectedCalendar
        } else {
            leftCalendar = selectedCalendar
            rightCalendar = selectedCalendar.cloneCalendar().apply { add(Calendar.MONTH, 1) }
        }
    }

    private fun setupAdapters(binding: DialogDateRangePickerBinding) {
        leftAdapter = CalendarRangeAdapter(palette, ::onDateClicked)
        rightAdapter = CalendarRangeAdapter(palette, ::onDateClicked)
        binding.leftCalendarGrid.apply {
            layoutManager = GridLayoutManager(context, DAYS_IN_WEEK)
            adapter = leftAdapter
            itemAnimator = null
        }
        binding.rightCalendarGrid.apply {
            layoutManager = GridLayoutManager(context, DAYS_IN_WEEK)
            adapter = rightAdapter
            itemAnimator = null
        }
    }

    private fun setupActions(binding: DialogDateRangePickerBinding) {
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener {
            val start = startDate
            val end = endDate
            if (start == null || end == null) {
                binding.errorText.isVisible = true
                return@setOnClickListener
            }
            parentFragmentManager.setFragmentResult(
                resultRequestKey(),
                Bundle().apply {
                    putString(RESULT_START_DATE, start.formatDate())
                    putString(RESULT_END_DATE, end.formatDate())
                },
            )
            dismiss()
        }

        binding.leftPrevYearButton.setOnClickListener { shiftMonth(binding, leftCalendar, Calendar.YEAR, -1) }
        binding.leftPrevMonthButton.setOnClickListener { shiftMonth(binding, leftCalendar, Calendar.MONTH, -1) }
        binding.leftNextMonthButton.setOnClickListener { shiftMonth(binding, leftCalendar, Calendar.MONTH, 1) }
        binding.leftNextYearButton.setOnClickListener { shiftMonth(binding, leftCalendar, Calendar.YEAR, 1) }
        binding.rightPrevYearButton.setOnClickListener { shiftMonth(binding, rightCalendar, Calendar.YEAR, -1) }
        binding.rightPrevMonthButton.setOnClickListener { shiftMonth(binding, rightCalendar, Calendar.MONTH, -1) }
        binding.rightNextMonthButton.setOnClickListener { shiftMonth(binding, rightCalendar, Calendar.MONTH, 1) }
        binding.rightNextYearButton.setOnClickListener { shiftMonth(binding, rightCalendar, Calendar.YEAR, 1) }
    }

    private fun shiftMonth(
        binding: DialogDateRangePickerBinding,
        calendar: Calendar,
        field: Int,
        amount: Int,
    ) {
        calendar.add(field, amount)
        updateCalendarDisplay(binding)
    }

    private fun onDateClicked(date: CalendarDate) {
        if (!date.isEnabled || date.day <= 0) return
        val start = startDate
        val end = endDate
        when {
            start == null || end != null -> {
                startDate = date
                endDate = null
            }

            date.isBefore(start) -> {
                startDate = date
                endDate = null
            }

            else -> {
                endDate = date
            }
        }
        binding.errorText.isVisible = false
        updateConfirmState(binding)
        updateCalendarDisplay(binding)
    }

    private fun updateCalendarDisplay(binding: DialogDateRangePickerBinding) {
        val monthFormatter = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        binding.leftMonthText.text = monthFormatter.format(leftCalendar.time)
        binding.rightMonthText.text = monthFormatter.format(rightCalendar.time)
        leftAdapter.submitDates(generateCalendarDates(leftCalendar))
        rightAdapter.submitDates(generateCalendarDates(rightCalendar))
        updateConfirmState(binding)
    }

    private fun updateConfirmState(binding: DialogDateRangePickerBinding) {
        val enabled = startDate != null && endDate != null
        binding.confirmButton.isEnabled = enabled
        binding.confirmButton.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun generateCalendarDates(calendar: Calendar): List<CalendarDate> {
        val dates = mutableListOf<CalendarDate>()
        val today = CalendarDate.today()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val firstDayCalendar = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val firstDayOfWeek = when (firstDayCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> DAYS_IN_WEEK
            else -> firstDayCalendar.get(Calendar.DAY_OF_WEEK) - 1
        }
        val daysInMonth = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthCalendar = Calendar.getInstance().apply { set(year, month - 2, 1) }
        val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (index in firstDayOfWeek - 1 downTo 1) {
            val day = daysInPrevMonth - index + 1
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year
            dates.add(CalendarDate(prevYear, prevMonth, day, isCurrentMonth = false, isEnabled = false))
        }

        for (day in 1..daysInMonth) {
            val currentDate = CalendarDate(year, month, day)
            val dateState = calculateDateState(currentDate)
            dates.add(
                CalendarDate(
                    year = year,
                    month = month,
                    day = day,
                    isToday = currentDate.isSameDay(today),
                    isStartDate = dateState.isStartDate,
                    isEndDate = dateState.isEndDate,
                    isInRange = dateState.isInRange,
                    isRangeStart = dateState.isRangeStart,
                    isRangeEnd = dateState.isRangeEnd,
                    isRangeMiddle = dateState.isRangeMiddle,
                    isSingleSelected = dateState.isSingleSelected,
                ),
            )
        }

        val remainingCells = CALENDAR_CELL_COUNT - dates.size
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        for (day in 1..remainingCells) {
            dates.add(CalendarDate(nextYear, nextMonth, day, isCurrentMonth = false, isEnabled = false))
        }
        return dates
    }

    private fun calculateDateState(currentDate: CalendarDate): DateState {
        val start = startDate ?: return DateState()
        val end = endDate
        return when {
            end == null && currentDate.isSameDay(start) -> {
                DateState(isSingleSelected = true, isStartDate = true)
            }

            end == null -> DateState()

            currentDate.isSameDay(start) && currentDate.isSameDay(end) -> {
                DateState(isStartDate = true, isEndDate = true, isSingleSelected = true)
            }

            currentDate.isSameDay(start) -> DateState(isStartDate = true, isRangeStart = true)

            currentDate.isSameDay(end) -> DateState(isEndDate = true, isRangeEnd = true)

            currentDate.isAfter(start) && currentDate.isBefore(end) -> {
                DateState(isInRange = true, isRangeMiddle = true)
            }

            else -> DateState()
        }
    }

    private fun applyTheme(binding: DialogDateRangePickerBinding) {
        binding.pickerRoot.setBackgroundResource(palette.pickerBackground)
        binding.titleText.setTextColor(palette.textPrimary)
        binding.cancelButton.setTextColor(palette.brandPrimary)
        binding.confirmButton.setTextColor(palette.brandPrimary)
        binding.errorText.setTextColor(palette.textMuted)
        binding.divider.setBackgroundColor(palette.divider)
        binding.leftMonthText.setTextColor(palette.textPrimary)
        binding.rightMonthText.setTextColor(palette.textPrimary)
        binding.leftWeekdayRow.setBackgroundResource(palette.weekdayBackground)
        binding.rightWeekdayRow.setBackgroundResource(palette.weekdayBackground)
        binding.leftWeekdayRow.setTextColorForChildren(palette.textMuted)
        binding.rightWeekdayRow.setTextColorForChildren(palette.textMuted)
        listOf(
            binding.leftPrevYearButton,
            binding.leftPrevMonthButton,
            binding.leftNextMonthButton,
            binding.leftNextYearButton,
            binding.rightPrevYearButton,
            binding.rightPrevMonthButton,
            binding.rightNextMonthButton,
            binding.rightNextYearButton,
        ).forEach { it.setColorFilter(palette.textMuted) }
    }

    private fun pickerTitle(): String {
        return requireArguments().getString(ARG_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: getString(com.holderzone.foundation.core.R.string.foundation_date_range_title)
    }

    private fun resultRequestKey(): String {
        return requireArguments().getString(ARG_REQUEST_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: REQUEST_KEY
    }

    private fun ViewGroup.setTextColorForChildren(color: Int) {
        for (index in 0 until childCount) {
            (getChildAt(index) as? android.widget.TextView)?.setTextColor(color)
        }
    }

    private fun Calendar.cloneCalendar(): Calendar = (clone() as Calendar)

    private data class DateState(
        val isStartDate: Boolean = false,
        val isEndDate: Boolean = false,
        val isInRange: Boolean = false,
        val isRangeStart: Boolean = false,
        val isRangeEnd: Boolean = false,
        val isRangeMiddle: Boolean = false,
        val isSingleSelected: Boolean = false,
    )

    companion object {
        const val REQUEST_KEY = "foundation_date_range_picker_request"
        const val RESULT_START_DATE = "result_start_date"
        const val RESULT_END_DATE = "result_end_date"

        private const val ARG_REQUEST_KEY = "arg_request_key"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_START_DATE = "arg_start_date"
        private const val ARG_END_DATE = "arg_end_date"
        private const val DAYS_IN_WEEK = 7
        private const val CALENDAR_CELL_COUNT = 42
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.42f
        private const val TAG = "DateRangePickerDialogFragment"

        fun newInstance(
            startDate: String?,
            endDate: String?,
            title: String? = null,
            requestKey: String = REQUEST_KEY,
        ): DateRangePickerDialogFragment {
            return DateRangePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_START_DATE, startDate.orEmpty())
                    putString(ARG_END_DATE, endDate.orEmpty())
                    putString(ARG_TITLE, title.orEmpty())
                    putString(ARG_REQUEST_KEY, requestKey)
                }
            }
        }

        fun show(
            fragmentManager: FragmentManager,
            startDate: String?,
            endDate: String?,
            title: String? = null,
            requestKey: String = REQUEST_KEY,
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            newInstance(startDate, endDate, title, requestKey).show(fragmentManager, TAG)
        }
    }
}
