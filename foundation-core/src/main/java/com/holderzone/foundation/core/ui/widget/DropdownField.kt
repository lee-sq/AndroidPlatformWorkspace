package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.ViewDropdownItemBinding
import com.holderzone.foundation.core.ui.base.ImmersiveMode
import com.holderzone.foundation.core.ui.loading.FadingCircleLoading

class DropdownField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var popupWindow: PopupWindow? = null
    private var options: List<Option> = emptyList()
    private var selectedValue: String? = null
    private var onOptionSelected: ((Option) -> Unit)? = null
    private var popupBackgroundColor: Int = ContextCompat.getColor(context, R.color.foundation_surface)
    private var popupStrokeColor: Int = ContextCompat.getColor(context, R.color.foundation_border)
    private var dividerColor: Int = ContextCompat.getColor(context, R.color.foundation_border)
    private var textNormalColor: Int = ContextCompat.getColor(context, R.color.foundation_text_primary)
    private var textSelectedColor: Int = ContextCompat.getColor(context, R.color.foundation_primary)
    private var placeholderText: CharSequence = ""
    private var compactMode = false
    private var popupMatchParentWidth = false
    private var popupVerticalGapPx: Int? = null
    private var popupUseRootAnchor = false
    private var state: State = State.CONTENT
    private var loadingText: CharSequence = ""
    private var emptyText: CharSequence = ""
    private var errorText: CharSequence = ""
    private var onDropdownOpen: (() -> Unit)? = null
    private var onStateAction: (() -> Unit)? = null

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        isClickable = true
        isFocusable = true
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_foundation_dropdown, 0)
        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.foundation_space_sm)
        setOnClickListener { showDropdown() }
        applyTouchTarget()
    }

    fun setOptions(options: List<Option>) {
        this.options = options
        updateDisplayedText()
    }

    fun setSelectedValue(value: String?) {
        selectedValue = value
        updateDisplayedText()
    }

    fun setPlaceholder(value: CharSequence) {
        placeholderText = value
        updateDisplayedText()
    }

    fun setOnOptionSelectedListener(listener: (Option) -> Unit) {
        onOptionSelected = listener
    }

    fun setOnDropdownOpenListener(listener: (() -> Unit)?) {
        onDropdownOpen = listener
    }

    fun setOnStateActionListener(listener: (() -> Unit)?) {
        onStateAction = listener
    }

    fun setState(
        state: State,
        loadingText: CharSequence = this.loadingText,
        emptyText: CharSequence = this.emptyText,
        errorText: CharSequence = this.errorText,
    ) {
        this.state = state
        this.loadingText = loadingText
        this.emptyText = emptyText
        this.errorText = errorText
        if (popupWindow?.isShowing == true) {
            showDropdown(notifyOpen = false)
        }
    }

    fun setCompactMode(enabled: Boolean) {
        compactMode = enabled
        applyTouchTarget()
    }

    fun setPopupMatchParentWidth(enabled: Boolean) {
        popupMatchParentWidth = enabled
    }

    fun setPopupVerticalGapPx(gapPx: Int) {
        popupVerticalGapPx = gapPx.coerceAtLeast(0)
    }

    fun setPopupUseRootAnchor(enabled: Boolean) {
        popupUseRootAnchor = enabled
    }

    fun applyPalette(
        @DrawableRes backgroundRes: Int,
        @ColorInt textColor: Int,
        @ColorInt selectedTextColor: Int,
        @ColorInt popupBackgroundColor: Int,
        @ColorInt popupStrokeColor: Int,
        @ColorInt dividerColor: Int,
        @ColorInt arrowTintColor: Int,
    ) {
        textNormalColor = textColor
        textSelectedColor = selectedTextColor
        this.popupBackgroundColor = popupBackgroundColor
        this.popupStrokeColor = popupStrokeColor
        this.dividerColor = dividerColor
        if (backgroundRes == 0) background = null else setBackgroundResource(backgroundRes)
        setTextColor(textColor)
        tintArrow(arrowTintColor)
    }

    private fun showDropdown(notifyOpen: Boolean = true) {
        if (notifyOpen) onDropdownOpen?.invoke()
        if (options.isEmpty() && state == State.CONTENT) return
        popupWindow?.dismiss()
        val activity = context.findActivity()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            when {
                state == State.LOADING -> addView(createLoadingView())
                state == State.ERROR -> addView(createStateTextView(errorText, clickable = true))
                options.isEmpty() -> addView(createStateTextView(emptyText, clickable = false))
                else -> options.forEachIndexed { index, option ->
                    addView(createItemView(option, showDivider = index < options.lastIndex))
                }
            }
        }
        val popupContent = ScrollView(context).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = true
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
            background = createPopupBackground()
            clipToOutline = true
            addView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        ImmersiveMode.inheritActivitySystemUiVisibility(activity, popupContent)
        popupWindow = PopupWindow(
            popupContent,
            dropdownWidth(),
            dropdownHeight(),
            false,
        ).apply {
            isOutsideTouchable = true
            elevation = resources.getDimensionPixelSize(R.dimen.foundation_space_sm).toFloat()
            animationStyle = R.style.Animation_Foundation_Dropdown
            setBackgroundDrawable(createPopupBackground())
            setOnDismissListener {
                ImmersiveMode.applyToActivityAndView(activity, activeView = null)
            }
            showAnchoredPopup()
            isFocusable = true
            update()
            applyPopupImmersiveMode(activity, popupContent)
            popupContent.post {
                applyPopupImmersiveMode(activity, popupContent)
            }
        }
    }

    private fun applyPopupImmersiveMode(
        activity: android.app.Activity?,
        popupContent: View,
    ) {
        ImmersiveMode.applyToActivityAndView(activity, popupContent.rootView ?: popupContent)
    }

    private fun dropdownHeight(): Int {
        val itemHeight = resources.getDimensionPixelSize(R.dimen.foundation_dropdown_item_height)
        val maxHeight = resources.getDimensionPixelSize(R.dimen.foundation_dropdown_max_height)
        val count = if (state != State.CONTENT || options.isEmpty()) 1 else options.size
        return (itemHeight * count).coerceAtMost(maxHeight)
    }

    private fun dropdownWidth(): Int {
        return if (popupMatchParentWidth) {
            (parent as? View)?.width?.takeIf { it > 0 } ?: width
        } else {
            width
        }
    }

    private fun dropdownXOffset(): Int {
        return if (popupMatchParentWidth) -left else 0
    }

    private fun dropdownYOffset(): Int {
        val gap = popupVerticalGapPx ?: resources.getDimensionPixelSize(R.dimen.foundation_space_xs)
        return if (popupMatchParentWidth) {
            val parentHeight = (parent as? View)?.height ?: 0
            val parentBottomPadding = (parentHeight - bottom).coerceAtLeast(0)
            parentBottomPadding + gap
        } else {
            gap
        }
    }

    private fun PopupWindow.showAnchoredPopup() {
        if (!popupUseRootAnchor || Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            showAsDropDown(
                this@DropdownField,
                dropdownXOffset(),
                dropdownYOffset(),
            )
            return
        }
        showAtLocation(
            rootView,
            Gravity.NO_GRAVITY,
            dropdownRootX(),
            dropdownRootY(),
        )
    }

    private fun dropdownRootX(): Int {
        val anchorLocation = IntArray(2)
        getLocationInWindow(anchorLocation)
        return anchorLocation[0] + dropdownXOffset()
    }

    private fun dropdownRootY(): Int {
        val anchorLocation = IntArray(2)
        getLocationInWindow(anchorLocation)
        return anchorLocation[1] + height + dropdownYOffset()
    }

    private fun createItemView(option: Option, showDivider: Boolean): View {
        val binding = ViewDropdownItemBinding.inflate(LayoutInflater.from(context))
        val selected = option.value == selectedValue
        binding.dropdownItemText.text = option.label
        binding.dropdownItemText.gravity = Gravity.CENTER
        binding.dropdownItemText.setTextColor(if (selected) textSelectedColor else textNormalColor)
        binding.dropdownDivider.setBackgroundColor(dividerColor)
        binding.dropdownDivider.isVisible = showDivider
        binding.root.setOnClickListener {
            selectedValue = option.value
            updateDisplayedText()
            popupWindow?.dismiss()
            onOptionSelected?.invoke(option)
        }
        return binding.root
    }

    private fun createStateTextView(text: CharSequence, clickable: Boolean): View {
        return TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            includeFontPadding = false
            minHeight = resources.getDimensionPixelSize(R.dimen.foundation_dropdown_item_height)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.foundation_space_lg),
                0,
                resources.getDimensionPixelSize(R.dimen.foundation_space_lg),
                0,
            )
            setTextColor(if (clickable) textSelectedColor else textNormalColor)
            textSize = 15f
            if (clickable) {
                setOnClickListener {
                    popupWindow?.dismiss()
                    onStateAction?.invoke()
                }
            }
        }
    }

    private fun createLoadingView(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = resources.getDimensionPixelSize(R.dimen.foundation_dropdown_item_height)
            val spinnerSize = resources.getDimensionPixelSize(R.dimen.foundation_dropdown_loading_size)
            addView(
                FadingCircleLoading(context),
                LinearLayout.LayoutParams(spinnerSize, spinnerSize),
            )
            addView(
                TextView(context).apply {
                    text = loadingText
                    setTextColor(textNormalColor)
                    textSize = 15f
                    includeFontPadding = false
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.foundation_space_sm)
                },
            )
        }
    }

    private fun createPopupBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.foundation_radius_lg)
            setColor(popupBackgroundColor)
            setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1), popupStrokeColor)
        }
    }

    private fun updateDisplayedText() {
        val selected = options.firstOrNull { it.value == selectedValue }
        text = selected?.label ?: placeholderText
    }

    private fun tintArrow(@ColorInt color: Int) {
        compoundDrawablesRelative.filterNotNull().forEach { drawable ->
            drawable.mutate().setTintList(ColorStateList.valueOf(color))
        }
    }

    private fun applyTouchTarget() {
        minHeight = resources.getDimensionPixelSize(
            if (compactMode) {
                R.dimen.foundation_dropdown_compact_min_touch_target
            } else {
                R.dimen.foundation_dropdown_min_touch_target
            },
        )
    }

    data class Option(
        val value: String,
        val label: String,
    )

    enum class State {
        LOADING,
        EMPTY,
        ERROR,
        CONTENT,
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? {
    return when (this) {
        is android.app.Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
