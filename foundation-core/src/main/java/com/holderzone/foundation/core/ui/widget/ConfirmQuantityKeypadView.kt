package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.holderzone.foundation.core.R

/**
 * 带完成按钮的数量输入键盘，适合需要显式提交输入的场景。
 */
class ConfirmQuantityKeypadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onDigit(digit: Char)
        fun onDecimal()
        fun onDelete()
        fun onDone()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var deleteRepeatRunnable: Runnable? = null
    private var listener: Listener? = null
    private val digitKeys: List<TextView>
    private val decimalKey: TextView
    private val deleteKey: TextView
    private val doneKey: TextView

    init {
        orientation = VERTICAL
        isFocusable = false
        setPadding(
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
        )
        LayoutInflater.from(context).inflate(R.layout.view_confirm_numeric_keypad, this, true)
        digitKeys = listOf(
            findViewById(R.id.confirmNumericKeypadKey0),
            findViewById(R.id.confirmNumericKeypadKey1),
            findViewById(R.id.confirmNumericKeypadKey2),
            findViewById(R.id.confirmNumericKeypadKey3),
            findViewById(R.id.confirmNumericKeypadKey4),
            findViewById(R.id.confirmNumericKeypadKey5),
            findViewById(R.id.confirmNumericKeypadKey6),
            findViewById(R.id.confirmNumericKeypadKey7),
            findViewById(R.id.confirmNumericKeypadKey8),
            findViewById(R.id.confirmNumericKeypadKey9),
        )
        decimalKey = findViewById(R.id.confirmNumericKeypadDecimal)
        deleteKey = findViewById(R.id.confirmNumericKeypadDelete)
        doneKey = findViewById(R.id.confirmNumericKeypadDone)
        bindKeyEvents()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setDecimalEnabled(enabled: Boolean) {
        decimalKey.isEnabled = enabled
        decimalKey.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        setBackgroundResource(palette.surfaceBackground)
        (digitKeys + decimalKey + deleteKey).forEach { key ->
            key.setBackgroundResource(palette.keyBackground)
            key.setTextColor(palette.keyTextColor)
        }
        deleteKey.setTextColor(palette.deleteTextColor)
        applyDonePalette(palette)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun onDetachedFromWindow() {
        stopDeleteRepeat()
        super.onDetachedFromWindow()
    }

    private fun bindKeyEvents() {
        digitKeys.forEach { key ->
            key.setOnClickListener {
                key.text?.firstOrNull()?.let { digit -> listener?.onDigit(digit) }
            }
            key.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        key.isPressed = true
                        key.text?.firstOrNull()?.let { digit -> listener?.onDigit(digit) }
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_OUTSIDE,
                    -> {
                        key.isPressed = false
                        true
                    }

                    else -> true
                }
            }
        }
        decimalKey.setOnClickListener {
            if (decimalKey.isVisible && decimalKey.isEnabled) {
                listener?.onDecimal()
            }
        }
        deleteKey.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    listener?.onDelete()
                    startDeleteRepeat()
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE,
                -> {
                    stopDeleteRepeat()
                    view.isPressed = false
                    true
                }

                else -> true
            }
        }
        doneKey.setOnClickListener { listener?.onDone() }
    }

    private fun startDeleteRepeat() {
        stopDeleteRepeat()
        val repeatRunnable = object : Runnable {
            override fun run() {
                listener?.onDelete()
                mainHandler.postDelayed(this, DELETE_REPEAT_INTERVAL_MS)
            }
        }
        deleteRepeatRunnable = repeatRunnable
        mainHandler.postDelayed(repeatRunnable, DELETE_REPEAT_START_DELAY_MS)
    }

    private fun stopDeleteRepeat() {
        deleteRepeatRunnable?.let(mainHandler::removeCallbacks)
        deleteRepeatRunnable = null
    }

    private fun applyDonePalette(palette: NumericKeypadPalette) {
        doneKey.setBackgroundResource(palette.keyBackground)
        doneKey.setTextColor(palette.keyTextColor)
        doneKey.setText(R.string.foundation_keypad_done)
        palette.doneText?.let { doneText ->
            doneKey.text = doneText
        }
        palette.doneBackground?.let { doneBackground ->
            doneKey.background = createDoneBackground(doneBackground)
        }
        palette.doneTextColor?.let { doneTextColor ->
            doneKey.setTextColor(doneTextColor)
        }
    }

    private fun createDoneBackground(color: Int): RippleDrawable {
        val radius = resources.getDimension(R.dimen.foundation_radius_md)
        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }
        return RippleDrawable(ColorStateList.valueOf(color.withPressedAlpha()), content, null)
    }

    private fun Int.withPressedAlpha(): Int {
        return Color.argb(
            (Color.alpha(this) * PRESSED_ALPHA).toInt(),
            Color.red(this),
            Color.green(this),
            Color.blue(this),
        )
    }

    private companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.35f
        private const val PRESSED_ALPHA = 0.72f
        private const val DELETE_REPEAT_START_DELAY_MS = 400L
        private const val DELETE_REPEAT_INTERVAL_MS = 80L
    }
}
