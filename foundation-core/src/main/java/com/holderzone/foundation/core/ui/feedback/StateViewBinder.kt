package com.holderzone.foundation.core.ui.feedback

import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.ViewStateBinding

object StateViewBinder {
    fun bind(
        binding: View,
        title: String,
        description: String = "",
        actionText: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_foundation_state_empty,
        colors: StateViewColors? = null,
        onActionClick: (() -> Unit)? = null,
    ) {
        bind(
            binding = ViewStateBinding.bind(binding),
            title = title,
            description = description,
            actionText = actionText,
            iconRes = iconRes,
            colors = colors,
            onActionClick = onActionClick,
        )
    }

    fun bind(
        binding: ViewStateBinding,
        title: String,
        description: String = "",
        actionText: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_foundation_state_empty,
        colors: StateViewColors? = null,
        onActionClick: (() -> Unit)? = null,
    ) {
        binding.stateIcon.setImageResource(iconRes)
        binding.stateTitle.text = title
        binding.stateDescription.text = description
        binding.stateDescription.isVisible = description.isNotBlank()
        binding.stateAction.text = actionText.orEmpty()
        binding.stateAction.isVisible = !actionText.isNullOrBlank()
        binding.stateAction.setOnClickListener(
            if (onActionClick == null) null else android.view.View.OnClickListener { onActionClick() },
        )
        colors?.let { applyColors(binding, it) }
    }

    fun applyColors(view: View, colors: StateViewColors) {
        applyColors(ViewStateBinding.bind(view), colors)
    }

    fun applyColors(binding: ViewStateBinding, colors: StateViewColors) {
        binding.stateTitle.setTextColor(colors.title)
        binding.stateDescription.setTextColor(colors.description)
        binding.stateAction.setTextColor(colors.actionText)
        binding.stateAction.setBackgroundResource(colors.actionBackground)
        binding.stateIcon.imageTintList = ColorStateList.valueOf(colors.icon)
    }

    fun setIconTint(view: View, @ColorInt color: Int) {
        ViewStateBinding.bind(view).stateIcon.imageTintList = ColorStateList.valueOf(color)
    }
}

data class StateViewColors(
    @param:ColorInt val title: Int,
    @param:ColorInt val description: Int,
    @param:ColorInt val icon: Int,
    @param:ColorInt val actionText: Int,
    @param:DrawableRes val actionBackground: Int,
)
