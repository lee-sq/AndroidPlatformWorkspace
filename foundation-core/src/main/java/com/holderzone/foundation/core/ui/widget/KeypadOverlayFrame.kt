package com.holderzone.foundation.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.holderzone.foundation.core.R
import kotlin.math.roundToInt

/**
 * 键盘拦截层，用来消费键盘外点击，并在需要时绘制聚光遮罩。
 */
internal class KeypadOverlayFrame(
    context: Context,
    private val keypadView: View,
    @param:ColorInt private var overlayColor: Int,
    private val onOutsideTouch: () -> Unit,
) : FrameLayout(context) {
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private var spotlightRect: RectF? = null
    private var spotlightRadius = 0f
    private var outsideDismissEnabled = true

    init {
        isClickable = true
        isFocusable = false
        setWillNotDraw(false)
        addView(
            keypadView,
            LayoutParams(
                keypadView.resources.getDimensionPixelSize(R.dimen.foundation_keypad_width),
                LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    fun placeKeypad(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val params = keypadView.layoutParams as LayoutParams
        params.width = width
        params.height = height
        params.leftMargin = x
        params.topMargin = y
        keypadView.layoutParams = params
    }

    fun setOverlayColor(@ColorInt color: Int) {
        overlayColor = color
        invalidate()
    }

    fun setSpotlight(
        rect: Rect?,
        radiusDp: Float,
    ) {
        spotlightRect = rect?.let { RectF(it) }
        spotlightRadius = radiusDp.dpToPx()
        invalidate()
    }

    fun setOutsideDismissEnabled(enabled: Boolean) {
        outsideDismissEnabled = enabled
    }

    override fun dispatchDraw(canvas: Canvas) {
        val slotRect = spotlightRect
        if (slotRect == null) {
            super.dispatchDraw(canvas)
            return
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawColor(overlayColor)
        canvas.drawRoundRect(slotRect, spotlightRadius, spotlightRadius, clearPaint)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(layer)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return ev.actionMasked == MotionEvent.ACTION_DOWN && !isInsideKeypad(ev.x, ev.y)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !isInsideKeypad(event.x, event.y)) {
            if (outsideDismissEnabled) {
                onOutsideTouch()
            }
        }
        return true
    }

    private fun isInsideKeypad(
        x: Float,
        y: Float,
    ): Boolean {
        return x >= keypadView.left &&
            x <= keypadView.right &&
            y >= keypadView.top &&
            y <= keypadView.bottom
    }

    private fun Float.dpToPx(): Float {
        return (this * resources.displayMetrics.density).roundToInt().toFloat()
    }
}
