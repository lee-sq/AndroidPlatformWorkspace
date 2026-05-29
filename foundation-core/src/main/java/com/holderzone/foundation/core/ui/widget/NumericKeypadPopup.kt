package com.holderzone.foundation.core.ui.widget

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.ui.base.ImmersiveMode

/**
 * 悬浮数字键盘 PopupWindow 管理器。
 */
class NumericKeypadPopup(
    private val context: Context,
    initialPalette: NumericKeypadPalette,
) {
    private val keypadView = NumericKeypadView(context)
    private val overlayFrame = KeypadOverlayFrame(context, keypadView, initialPalette.overlayColor) { dismiss() }
    private val width = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_width)
    private val height = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_height)
    private val margin = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_anchor_margin)
    private val popupWindow = PopupWindow(
        overlayFrame,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        false,
    )
    private val activity = context.findActivity()

    init {
        keypadView.applyPalette(initialPalette)
        PopupWindowCompatConfigurator.configure(popupWindow, context)
    }

    val isShowing: Boolean
        get() = popupWindow.isShowing

    fun setListener(listener: NumericKeypadView.Listener?) {
        keypadView.setListener(listener)
    }

    fun setOnDismissListener(listener: PopupWindow.OnDismissListener?) {
        popupWindow.setOnDismissListener(listener)
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        keypadView.applyPalette(palette)
        overlayFrame.setOverlayColor(palette.overlayColor)
    }

    fun show(anchor: View) {
        if (!anchor.canHostKeypad()) return
        val rootView = anchor.rootView
        val position = calculatePosition(anchor)
        val rootPosition = rootView.locationOnScreen()
        overlayFrame.placeKeypad(
            x = position.x - rootPosition.x,
            y = position.y - rootPosition.y,
            width = width,
            height = height,
        )
        popupWindow.width = rootView.width
        popupWindow.height = rootView.height
        ImmersiveMode.inheritActivitySystemUiVisibility(activity, overlayFrame)
        if (popupWindow.isShowing) {
            popupWindow.update(
                rootPosition.x,
                rootPosition.y,
                rootView.width,
                rootView.height,
            )
        } else {
            popupWindow.showAtLocation(rootView, Gravity.NO_GRAVITY, rootPosition.x, rootPosition.y)
            applyPopupImmersiveMode()
            overlayFrame.post { applyPopupImmersiveMode() }
        }
    }

    fun dismiss() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    private fun applyPopupImmersiveMode() {
        ImmersiveMode.applyToActivityAndView(activity, overlayFrame.rootView ?: overlayFrame)
    }

    private fun calculatePosition(anchor: View): Position {
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorRect = Rect(
            anchorLocation[0],
            anchorLocation[1],
            anchorLocation[0] + anchor.width,
            anchorLocation[1] + anchor.height,
        )
        val visibleFrame = Rect()
        anchor.getWindowVisibleDisplayFrame(visibleFrame)
        if (visibleFrame.isEmpty) {
            visibleFrame.set(0, 0, anchor.rootView.width, anchor.rootView.height)
        }
        val minX = visibleFrame.left + margin
        val minY = visibleFrame.top + margin
        val maxX = visibleFrame.right - margin - width
        val maxY = visibleFrame.bottom - margin - height

        val rawPosition = when {
            anchorRect.right + margin + width <= visibleFrame.right -> {
                Position(anchorRect.right + margin, anchorRect.top)
            }
            anchorRect.left - margin - width >= visibleFrame.left -> {
                Position(anchorRect.left - margin - width, anchorRect.top)
            }
            anchorRect.top - margin - height >= visibleFrame.top -> {
                Position(anchorRect.left, anchorRect.top - margin - height)
            }
            else -> {
                Position(anchorRect.left, anchorRect.bottom + margin)
            }
        }
        return Position(
            x = rawPosition.x.coerceInSafe(minX, maxX),
            y = rawPosition.y.coerceInSafe(minY, maxY),
        )
    }

    private data class Position(
        val x: Int,
        val y: Int,
    )
}

private object PopupWindowCompatConfigurator {
    fun configure(
        popupWindow: PopupWindow,
        context: Context,
    ) {
        popupWindow.apply {
            isOutsideTouchable = false
            isClippingEnabled = true
            elevation = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_elevation).toFloat()
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }
}

private data class ScreenPosition(
    val x: Int,
    val y: Int,
)

private fun View.locationOnScreen(): ScreenPosition {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return ScreenPosition(location[0], location[1])
}

private fun View.canHostKeypad(): Boolean {
    return isAttachedToWindow && isEnabled && visibility == View.VISIBLE && width > 0 && height > 0
}

private fun Int.coerceInSafe(
    minimumValue: Int,
    maximumValue: Int,
): Int {
    if (maximumValue < minimumValue) return minimumValue
    return coerceIn(minimumValue, maximumValue)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
