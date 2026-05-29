package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.holderzone.foundation.core.R

/**
 * 通用 4x3 悬浮数字键盘。
 */
class NumericKeypadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onDigit(digit: Char)
        fun onDelete()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var deleteRepeatRunnable: Runnable? = null
    private var listener: Listener? = null
    private val digitKeys: List<TextView>
    private val deleteKey: TextView

    init {
        orientation = VERTICAL
        isFocusable = false
        setPadding(
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
            resources.getDimensionPixelSize(R.dimen.foundation_keypad_padding),
        )
        LayoutInflater.from(context).inflate(R.layout.view_numeric_keypad, this, true)
        digitKeys = listOf(
            findViewById(R.id.foundationKeypadKey0),
            findViewById(R.id.foundationKeypadKey1),
            findViewById(R.id.foundationKeypadKey2),
            findViewById(R.id.foundationKeypadKey3),
            findViewById(R.id.foundationKeypadKey4),
            findViewById(R.id.foundationKeypadKey5),
            findViewById(R.id.foundationKeypadKey6),
            findViewById(R.id.foundationKeypadKey7),
            findViewById(R.id.foundationKeypadKey8),
            findViewById(R.id.foundationKeypadKey9),
        )
        deleteKey = findViewById(R.id.foundationKeypadDelete)
        bindKeyEvents()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        setBackgroundResource(palette.surfaceBackground)
        digitKeys.forEach { key ->
            key.setBackgroundResource(palette.keyBackground)
            key.setTextColor(palette.keyTextColor)
        }
        deleteKey.setBackgroundResource(palette.keyBackground)
        deleteKey.setTextColor(palette.deleteTextColor)
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
                key.text?.firstOrNull()?.let { digit ->
                    listener?.onDigit(digit)
                }
            }
            key.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        key.isPressed = true
                        key.text?.firstOrNull()?.let { digit ->
                            listener?.onDigit(digit)
                        }
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
        deleteKey.setOnClickListener {
            listener?.onDelete()
        }
        deleteKey.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    listener?.onDelete()
                    startDeleteRepeat()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopDeleteRepeat()
                    view.isPressed = false
                    true
                }
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

    private companion object {
        private const val DELETE_REPEAT_START_DELAY_MS = 400L
        private const val DELETE_REPEAT_INTERVAL_MS = 80L
    }
}
