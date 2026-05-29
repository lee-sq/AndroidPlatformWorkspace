package com.holderzone.foundation.core.ui.datetime

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.ItemDatetimePickerBinding
import com.holderzone.foundation.core.databinding.ViewDatetimeWheelBinding

class DateTimeWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = ViewDatetimeWheelBinding.inflate(LayoutInflater.from(context), this, true)
    private val layoutManager = LinearLayoutManager(context)
    private val snapHelper = LinearSnapHelper()
    private val adapter = WheelAdapter()
    private val itemHeight = resources.getDimensionPixelSize(R.dimen.foundation_datetime_picker_item_height)
    private var items: List<String> = emptyList()
    private var selectedIndex = 0
    private var suppressSelectionChange = false
    private var onSelectionChanged: ((Int) -> Unit)? = null

    init {
        binding.wheelList.layoutManager = layoutManager
        binding.wheelList.adapter = adapter
        binding.wheelList.itemAnimator = null
        snapHelper.attachToRecyclerView(binding.wheelList)
        binding.wheelList.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int,
                ) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateSelectedFromSnap()
                    }
                }
            },
        )
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        val verticalPadding = ((h - itemHeight) / 2).coerceAtLeast(0)
        binding.wheelList.updatePadding(top = verticalPadding, bottom = verticalPadding)
        scrollToIndex(selectedIndex, smooth = false)
    }

    fun submitItems(
        nextItems: List<String>,
        selectedIndex: Int,
    ) {
        items = nextItems
        this.selectedIndex = selectedIndex.coerceInItems()
        adapter.submitItems(nextItems)
        scrollToIndex(this.selectedIndex, smooth = false)
    }

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChanged = listener
    }

    fun applyPalette(
        textColor: Int,
        selectedTextColor: Int,
    ) {
        adapter.applyPalette(textColor, selectedTextColor)
    }

    private fun updateSelectedFromSnap() {
        val snapView = snapHelper.findSnapView(layoutManager) ?: return
        val index = layoutManager.getPosition(snapView).coerceInItems()
        if (index == selectedIndex) return
        selectedIndex = index
        adapter.setSelectedIndex(index)
        if (!suppressSelectionChange) {
            onSelectionChanged?.invoke(index)
        }
    }

    private fun scrollToIndex(
        index: Int,
        smooth: Boolean,
    ) {
        if (items.isEmpty()) return
        suppressSelectionChange = true
        if (smooth) {
            binding.wheelList.smoothScrollToPosition(index)
        } else {
            layoutManager.scrollToPositionWithOffset(index, 0)
            binding.wheelList.post {
                adapter.setSelectedIndex(index)
                suppressSelectionChange = false
            }
            return
        }
        binding.wheelList.post {
            adapter.setSelectedIndex(index)
            suppressSelectionChange = false
        }
    }

    private fun Int.coerceInItems(): Int {
        return coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    private inner class WheelAdapter : RecyclerView.Adapter<WheelAdapter.WheelViewHolder>() {
        private var values: List<String> = emptyList()
        private var normalTextColor = 0
        private var selectedTextColor = 0
        private var selectedAdapterIndex = 0

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): WheelViewHolder {
            return WheelViewHolder(
                ItemDatetimePickerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ).dateTimePickerItemText,
            )
        }

        override fun onBindViewHolder(
            holder: WheelViewHolder,
            position: Int,
        ) {
            holder.bind(values[position], position == selectedAdapterIndex)
        }

        override fun getItemCount(): Int = values.size

        fun submitItems(nextValues: List<String>) {
            values = nextValues
            selectedAdapterIndex = selectedIndex.coerceInItems()
            notifyDataSetChanged()
        }

        fun applyPalette(
            textColor: Int,
            selectedTextColor: Int,
        ) {
            normalTextColor = textColor
            this.selectedTextColor = selectedTextColor
            notifyDataSetChanged()
        }

        fun setSelectedIndex(index: Int) {
            val oldIndex = selectedAdapterIndex
            selectedAdapterIndex = index.coerceInItems()
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedAdapterIndex)
        }

        inner class WheelViewHolder(
            private val textView: TextView,
        ) : RecyclerView.ViewHolder(textView) {
            fun bind(
                value: String,
                selected: Boolean,
            ) {
                textView.text = value
                textView.setTextColor(if (selected) selectedTextColor else normalTextColor)
                textView.alpha = if (selected) 1f else 0.58f
            }
        }
    }
}
