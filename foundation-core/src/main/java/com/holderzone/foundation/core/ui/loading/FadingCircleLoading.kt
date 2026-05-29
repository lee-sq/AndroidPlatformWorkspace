package com.holderzone.foundation.core.ui.loading

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.ui.loading.spinkit.style.FadingCircle

class FadingCircleLoading @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val drawable = FadingCircle()

    init {
        drawable.callback = this
        drawable.setColor(context.getColor(R.color.foundation_primary))
    }

    fun setColor(@ColorInt color: Int) {
        drawable.setColor(color)
        invalidate()
    }

    fun start() {
        if (!drawable.isRunning) {
            drawable.start()
        }
    }

    fun stop() {
        if (drawable.isRunning) {
            drawable.stop()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(
        changedView: View,
        visibility: Int,
    ) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimation()
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawable.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawable.draw(canvas)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who === drawable || super.verifyDrawable(who)
    }

    private fun updateAnimation() {
        if (isAttachedToWindow && isShown) {
            start()
        } else {
            stop()
        }
    }
}
