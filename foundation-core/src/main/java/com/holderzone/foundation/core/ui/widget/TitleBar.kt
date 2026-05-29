package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.ViewTitleBarBinding

class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ViewTitleBarBinding.inflate(LayoutInflater.from(context), this)
    val actionContainer: FrameLayout
        get() = binding.titleActionContainer
    private var redirectExternalChildren = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isBaselineAligned = false
        setPadding(
            resources.getDimensionPixelSize(R.dimen.foundation_title_bar_padding_horizontal),
            resources.getDimensionPixelSize(R.dimen.foundation_title_bar_padding_vertical),
            resources.getDimensionPixelSize(R.dimen.foundation_title_bar_padding_horizontal),
            resources.getDimensionPixelSize(R.dimen.foundation_title_bar_padding_vertical),
        )
        parseAttributes(attrs, defStyleAttr)
        redirectExternalChildren = true
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (redirectExternalChildren && child != null) {
            actionContainer.addView(child, toActionLayoutParams(params))
        } else {
            super.addView(child, index, params)
        }
    }

    fun setTitle(title: CharSequence) {
        binding.titleTextView.text = title
    }

    fun setBackClickListener(listener: OnClickListener?) {
        binding.titleBackButton.setOnClickListener(listener)
    }

    fun setBackVisible(visible: Boolean) {
        binding.titleBackButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setBackBackgroundResource(@DrawableRes drawableRes: Int) {
        binding.titleBackButton.setBackgroundResource(drawableRes)
    }

    fun setBackIconTint(@ColorInt color: Int) {
        binding.titleBackButton.imageTintList = ColorStateList.valueOf(color)
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        binding.titleTextView.setTextColor(color)
    }

    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TitleBar, defStyleAttr, 0)
        try {
            setTitle(typedArray.getText(R.styleable.TitleBar_titleText) ?: "")
            setBackVisible(typedArray.getBoolean(R.styleable.TitleBar_backVisible, true))
            binding.titleBackButton.setImageResource(
                typedArray.getResourceId(R.styleable.TitleBar_backIcon, R.drawable.ic_foundation_back),
            )
            val backBackgroundRes = typedArray.getResourceId(R.styleable.TitleBar_backBackground, 0)
            if (backBackgroundRes != 0) {
                binding.titleBackButton.setBackgroundResource(backBackgroundRes)
            }
            val iconSize = typedArray.getDimensionPixelSize(
                R.styleable.TitleBar_backIconSize,
                resources.getDimensionPixelSize(R.dimen.foundation_title_bar_back_icon_size),
            )
            val iconPadding = (resources.getDimensionPixelSize(R.dimen.foundation_title_bar_back_size) - iconSize)
                .coerceAtLeast(0) / 2
            binding.titleBackButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        } finally {
            typedArray.recycle()
        }
        binding.titleBackButton.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.foundation_primary),
        )
    }

    private fun toActionLayoutParams(params: ViewGroup.LayoutParams?): FrameLayout.LayoutParams {
        return when (params) {
            is FrameLayout.LayoutParams -> params
            is ViewGroup.MarginLayoutParams -> FrameLayout.LayoutParams(params).apply {
                gravity = Gravity.CENTER
            }
            null -> FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
            else -> FrameLayout.LayoutParams(params).apply {
                gravity = Gravity.CENTER
            }
        }
    }
}
