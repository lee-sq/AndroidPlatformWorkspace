package com.holderzone.foundation.core.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.holderzone.foundation.core.databinding.ItemCalendarDateBinding

class CalendarRangeAdapter(
    private var palette: CalendarPalette,
    private val onDateClick: (CalendarDate) -> Unit,
) : RecyclerView.Adapter<CalendarRangeAdapter.DateViewHolder>() {
    private var dates: List<CalendarDate> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemCalendarDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position], palette)
    }

    override fun getItemCount(): Int = dates.size

    fun submitDates(newDates: List<CalendarDate>) {
        dates = newDates
        notifyDataSetChanged()
    }

    fun applyPalette(newPalette: CalendarPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    inner class DateViewHolder(
        private val binding: ItemCalendarDateBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: CalendarDate, palette: CalendarPalette) {
            binding.dateText.text = if (date.day > 0) date.day.toString() else ""
            binding.root.isEnabled = date.isEnabled && date.day > 0
            binding.root.setOnClickListener(
                if (date.isEnabled && date.day > 0) {
                    View.OnClickListener { onDateClick(date) }
                } else {
                    null
                },
            )
            applyDateStyle(date, palette)
        }

        private fun applyDateStyle(date: CalendarDate, palette: CalendarPalette) {
            binding.rangeBackground.setBackgroundResource(android.R.color.transparent)
            binding.rangeBackgroundStart.setBackgroundResource(android.R.color.transparent)
            binding.rangeBackgroundEnd.setBackgroundResource(android.R.color.transparent)
            binding.dateText.setBackgroundResource(android.R.color.transparent)
            binding.rangeBackground.visibility = View.GONE
            binding.rangeBackgroundStart.visibility = View.GONE
            binding.rangeBackgroundEnd.visibility = View.GONE
            binding.dateText.isSelected = false

            when {
                !date.isEnabled || date.day <= 0 -> {
                    binding.dateText.setTextColor(palette.textDisabled)
                }

                date.isSingleSelected -> {
                    binding.dateText.setTextColor(palette.selectedText)
                    binding.dateText.setBackgroundResource(palette.selectedDayBackground)
                    binding.dateText.isSelected = true
                }

                date.isRangeStart -> {
                    binding.dateText.setTextColor(palette.selectedText)
                    binding.dateText.setBackgroundResource(palette.selectedDayBackground)
                    binding.dateText.isSelected = true
                    binding.rangeBackgroundStart.setBackgroundResource(palette.rangeStartBackground)
                    binding.rangeBackgroundStart.visibility = View.VISIBLE
                }

                date.isRangeEnd -> {
                    binding.dateText.setTextColor(palette.selectedText)
                    binding.dateText.setBackgroundResource(palette.selectedDayBackground)
                    binding.dateText.isSelected = true
                    binding.rangeBackgroundEnd.setBackgroundResource(palette.rangeEndBackground)
                    binding.rangeBackgroundEnd.visibility = View.VISIBLE
                }

                date.isRangeMiddle -> {
                    binding.dateText.setTextColor(palette.textPrimary)
                    binding.rangeBackground.setBackgroundResource(palette.rangeMiddleBackground)
                    binding.rangeBackground.visibility = View.VISIBLE
                }

                date.isToday -> {
                    binding.dateText.setTextColor(palette.brandPrimary)
                }

                !date.isCurrentMonth -> {
                    binding.dateText.setTextColor(palette.textDisabled)
                }

                else -> {
                    binding.dateText.setTextColor(palette.textPrimary)
                }
            }
        }
    }
}
