package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.ViewInputFieldBinding

class InputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ViewInputFieldBinding.inflate(LayoutInflater.from(context), this)
    private var passwordVisible = false
    private var passwordMode = false
    private var passwordVisibleIcon = R.drawable.ic_foundation_eye_open
    private var passwordHiddenIcon = R.drawable.ic_foundation_eye_closed

    init {
        orientation = VERTICAL
        parseAttributes(attrs, defStyleAttr)
        setupInteractions()
    }

    fun setLabel(label: CharSequence) {
        binding.inputLabel.text = label
        val hasLabel = label.isNotBlank()
        binding.inputLabel.isVisible = hasLabel
        binding.inputContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = if (hasLabel) resources.getDimensionPixelSize(R.dimen.foundation_space_sm) else 0
        }
    }

    fun setHint(hint: CharSequence) {
        binding.inputEditText.hint = hint
    }

    fun setText(text: CharSequence) {
        binding.inputEditText.setText(text)
        binding.inputEditText.setSelection(binding.inputEditText.text?.length ?: 0)
    }

    fun text(): String = binding.inputEditText.text?.toString().orEmpty()

    fun setMaxLength(maxLength: Int) {
        binding.inputEditText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    fun setDigitsOnly(maxLength: Int) {
        binding.inputEditText.inputType = InputType.TYPE_CLASS_NUMBER
        binding.inputEditText.filters = arrayOf(DigitsOnlyInputFilter(), InputFilter.LengthFilter(maxLength))
    }

    fun setPasswordMode(enabled: Boolean, maxLength: Int = DEFAULT_MAX_LENGTH) {
        passwordMode = enabled
        binding.inputPasswordToggleButton.isVisible = enabled
        binding.inputEditText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        if (enabled) {
            passwordVisible = false
            applyPasswordInputType()
            updatePasswordToggleIcon()
        }
    }

    fun applyPalette(
        @DrawableRes backgroundRes: Int,
        @ColorInt labelColor: Int,
        @ColorInt textColor: Int,
        @ColorInt hintColor: Int,
        @ColorInt iconColor: Int,
    ) {
        binding.inputContainer.setBackgroundResource(backgroundRes)
        applyContainerPadding()
        binding.inputLabel.setTextColor(labelColor)
        binding.inputEditText.setTextColor(textColor)
        binding.inputEditText.setHintTextColor(hintColor)
        val tint = ColorStateList.valueOf(iconColor)
        binding.inputLeadingIcon.imageTintList = tint
        binding.inputClearButton.imageTintList = tint
        binding.inputPasswordToggleButton.imageTintList = tint
    }

    fun editText(): EditText = binding.inputEditText

    fun setLeadingIcon(@DrawableRes iconRes: Int) {
        binding.inputLeadingIcon.setImageResource(iconRes)
        binding.inputLeadingIcon.isVisible = true
    }

    fun clearLeadingIcon() {
        binding.inputLeadingIcon.setImageDrawable(null)
        binding.inputLeadingIcon.isVisible = false
    }

    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InputField, defStyleAttr, 0)
        try {
            setLabel(typedArray.getText(R.styleable.InputField_inputLabel) ?: "")
            setHint(typedArray.getText(R.styleable.InputField_inputHint) ?: "")
            val inputBackgroundRes = typedArray.getResourceId(R.styleable.InputField_inputBackground, 0)
            if (inputBackgroundRes != 0) {
                binding.inputContainer.setBackgroundResource(inputBackgroundRes)
            }
            val leadingIconRes = typedArray.getResourceId(R.styleable.InputField_inputLeadingIcon, 0)
            if (leadingIconRes != 0) setLeadingIcon(leadingIconRes) else clearLeadingIcon()
            binding.inputClearButton.setImageResource(
                typedArray.getResourceId(R.styleable.InputField_inputClearIcon, R.drawable.ic_foundation_clear),
            )
            passwordHiddenIcon = typedArray.getResourceId(
                R.styleable.InputField_inputPasswordHiddenIcon,
                R.drawable.ic_foundation_eye_closed,
            )
            passwordVisibleIcon = typedArray.getResourceId(
                R.styleable.InputField_inputPasswordVisibleIcon,
                R.drawable.ic_foundation_eye_open,
            )
            binding.inputPasswordToggleButton.setImageResource(passwordHiddenIcon)
            applyIconSize(
                typedArray.getDimensionPixelSize(
                    R.styleable.InputField_inputIconSize,
                    resources.getDimensionPixelSize(R.dimen.foundation_input_icon_size),
                ),
            )
            applyInputHeight(
                typedArray.getDimensionPixelSize(
                    R.styleable.InputField_inputHeight,
                    resources.getDimensionPixelSize(R.dimen.foundation_input_height),
                ),
            )
            val maxLength = typedArray.getInt(R.styleable.InputField_inputMaxLength, DEFAULT_MAX_LENGTH)
            when (typedArray.getInt(R.styleable.InputField_inputMode, INPUT_MODE_TEXT)) {
                INPUT_MODE_DIGITS -> setDigitsOnly(maxLength)
                INPUT_MODE_PASSWORD -> setPasswordMode(enabled = true, maxLength = maxLength)
                else -> setMaxLength(maxLength)
            }
        } finally {
            typedArray.recycle()
        }

        applyContainerPadding()
        val iconColor = ContextCompat.getColor(context, R.color.foundation_text_muted)
        val tint = ColorStateList.valueOf(iconColor)
        binding.inputLeadingIcon.imageTintList = tint
        binding.inputClearButton.imageTintList = tint
        binding.inputPasswordToggleButton.imageTintList = tint
    }

    private fun setupInteractions() {
        binding.inputEditText.doAfterTextChanged {
            binding.inputClearButton.isVisible = !it.isNullOrEmpty()
        }
        binding.inputClearButton.setOnClickListener {
            binding.inputEditText.text?.clear()
        }
        binding.inputPasswordToggleButton.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun applyIconSize(iconSize: Int) {
        val touchSize = resources.getDimensionPixelSize(R.dimen.foundation_input_icon_touch_size)
        val iconPadding = (touchSize - iconSize).coerceAtLeast(0) / 2
        binding.inputLeadingIcon.updateLayoutParams<ViewGroup.LayoutParams> {
            width = iconSize
            height = iconSize
        }
        binding.inputClearButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        binding.inputPasswordToggleButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
    }

    private fun applyInputHeight(height: Int) {
        binding.inputContainer.updateLayoutParams<ViewGroup.LayoutParams> {
            this.height = height
        }
    }

    private fun applyContainerPadding() {
        binding.inputContainer.setPadding(
            resources.getDimensionPixelSize(R.dimen.foundation_input_padding_horizontal),
            binding.inputContainer.paddingTop,
            resources.getDimensionPixelSize(R.dimen.foundation_input_icon_padding_end),
            binding.inputContainer.paddingBottom,
        )
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        applyPasswordInputType()
        updatePasswordToggleIcon()
        binding.inputEditText.setSelection(binding.inputEditText.text?.length ?: 0)
    }

    private fun updatePasswordToggleIcon() {
        val icon = if (passwordVisible) passwordVisibleIcon else passwordHiddenIcon
        val description = if (passwordVisible) R.string.foundation_hide_password else R.string.foundation_show_password
        binding.inputPasswordToggleButton.setImageResource(icon)
        binding.inputPasswordToggleButton.contentDescription = context.getString(description)
    }

    private fun applyPasswordInputType() {
        if (!passwordMode) return
        binding.inputEditText.inputType = if (passwordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    private class DigitsOnlyInputFilter : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: android.text.Spanned,
            dstart: Int,
            dend: Int,
        ): CharSequence? {
            for (index in start until end) {
                if (source[index] !in '0'..'9') {
                    return source.subSequence(start, end).filter { it in '0'..'9' }
                }
            }
            return null
        }
    }

    private companion object {
        private const val DEFAULT_MAX_LENGTH = 256
        private const val INPUT_MODE_TEXT = 0
        private const val INPUT_MODE_DIGITS = 1
        private const val INPUT_MODE_PASSWORD = 2
    }
}
