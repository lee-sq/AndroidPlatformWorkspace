package com.holderzone.foundation.core.ui.widget

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
 * 确认页数量键盘 Popup 管理器。
 */
class ConfirmQuantityKeypadPopup(
    private val context: Context,
    initialPalette: NumericKeypadPalette,
) {
    private val keypadView = ConfirmQuantityKeypadView(context)
    private val overlayFrame = KeypadOverlayFrame(context, keypadView, initialPalette.overlayColor) { dismiss() }
    private val width = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_width)
    private val height = context.resources.getDimensionPixelSize(R.dimen.foundation_confirm_keypad_height)
    private val margin = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_anchor_margin)
    private val popupWindow = PopupWindow(
        overlayFrame,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        false,
    )
    private val activity = context.findKeypadActivity()

    init {
        keypadView.applyPalette(initialPalette)
        popupWindow.apply {
            isOutsideTouchable = false
            isClippingEnabled = true
            elevation = context.resources.getDimensionPixelSize(R.dimen.foundation_keypad_elevation).toFloat()
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    val isShowing: Boolean
        get() = popupWindow.isShowing

    fun setListener(listener: ConfirmQuantityKeypadView.Listener?) {
        keypadView.setListener(listener)
    }

    fun setOnDismissListener(listener: PopupWindow.OnDismissListener?) {
        popupWindow.setOnDismissListener(listener)
    }

    fun setDecimalEnabled(enabled: Boolean) {
        keypadView.setDecimalEnabled(enabled)
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        keypadView.applyPalette(palette)
        overlayFrame.setOverlayColor(palette.overlayColor)
    }

    fun show(
        anchor: View,
        spotlightRect: Rect? = null,
        outsideDismissEnabled: Boolean = true,
        placementPreference: KeypadPlacementPreference = KeypadPlacementPreference.DEFAULT,
    ) {
        if (!anchor.canHostConfirmKeypad()) return
        val rootView = anchor.rootView
        val position = calculatePosition(anchor, placementPreference)
        val rootPosition = rootView.locationOnScreen()
        overlayFrame.placeKeypad(
            x = position.x - rootPosition.x,
            y = position.y - rootPosition.y,
            width = width,
            height = height,
        )
        overlayFrame.setSpotlight(spotlightRect, SPOTLIGHT_RADIUS_DP)
        overlayFrame.setOutsideDismissEnabled(outsideDismissEnabled)
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

    private fun calculatePosition(
        anchor: View,
        placementPreference: KeypadPlacementPreference,
    ): KeypadPosition {
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

        val rawPosition = when (placementPreference) {
            KeypadPlacementPreference.LEFT -> calculateLeftPreferredPosition(anchorRect, visibleFrame)
            KeypadPlacementPreference.DEFAULT -> calculateDefaultPosition(anchorRect, visibleFrame)
        }
        return KeypadPosition(
            x = rawPosition.x.coerceInConfirmKeypad(minX, maxX),
            y = rawPosition.y.coerceInConfirmKeypad(minY, maxY),
        )
    }

    private fun calculateDefaultPosition(
        anchorRect: Rect,
        visibleFrame: Rect,
    ): KeypadPosition {
        return when {
            anchorRect.bottom + margin + height <= visibleFrame.bottom -> {
                KeypadPosition(anchorRect.left, anchorRect.bottom + margin)
            }

            anchorRect.top - margin - height >= visibleFrame.top -> {
                KeypadPosition(anchorRect.left, anchorRect.top - margin - height)
            }

            anchorRect.right + margin + width <= visibleFrame.right -> {
                KeypadPosition(anchorRect.right + margin, anchorRect.top)
            }

            else -> {
                KeypadPosition(anchorRect.left - margin - width, anchorRect.top)
            }
        }
    }

    private fun calculateLeftPreferredPosition(
        anchorRect: Rect,
        visibleFrame: Rect,
    ): KeypadPosition {
        return when {
            anchorRect.left - margin - width >= visibleFrame.left -> {
                KeypadPosition(anchorRect.left - margin - width, anchorRect.top)
            }

            anchorRect.right + margin + width <= visibleFrame.right -> {
                KeypadPosition(anchorRect.right + margin, anchorRect.top)
            }

            anchorRect.top - margin - height >= visibleFrame.top -> {
                KeypadPosition(anchorRect.left, anchorRect.top - margin - height)
            }

            else -> {
                KeypadPosition(anchorRect.left, anchorRect.bottom + margin)
            }
        }
    }

    private data class KeypadPosition(
        val x: Int,
        val y: Int,
    )

    private companion object {
        const val SPOTLIGHT_RADIUS_DP = 12f
    }
}

private fun View.canHostConfirmKeypad(): Boolean {
    return isAttachedToWindow && isEnabled && visibility == View.VISIBLE && width > 0 && height > 0
}

private fun Int.coerceInConfirmKeypad(
    minimumValue: Int,
    maximumValue: Int,
): Int {
    if (maximumValue < minimumValue) return minimumValue
    return coerceIn(minimumValue, maximumValue)
}

private data class KeypadScreenPosition(
    val x: Int,
    val y: Int,
)

private fun View.locationOnScreen(): KeypadScreenPosition {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return KeypadScreenPosition(location[0], location[1])
}

private tailrec fun Context.findKeypadActivity(): android.app.Activity? {
    return when (this) {
        is android.app.Activity -> this
        is ContextWrapper -> baseContext.findKeypadActivity()
        else -> null
    }
}
