package com.holderzone.foundation.core.ui.widget

import android.graphics.Rect
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.core.view.doOnDetach

/**
 * 将普通 View 绑定到确认页数量键盘。
 */
object ConfirmQuantityKeypadHost {
    fun attach(
        target: View,
        palette: NumericKeypadPalette,
        listener: ConfirmQuantityKeypadView.Listener,
        onDismiss: () -> Unit = {},
        outsideDismissEnabled: Boolean = true,
        placementPreference: KeypadPlacementPreference = KeypadPlacementPreference.DEFAULT,
    ): ConfirmQuantityKeypadHandle {
        return ConfirmQuantityKeypadHandle(
            target = target,
            palette = palette,
            listener = listener,
            onDismiss = onDismiss,
            outsideDismissEnabled = outsideDismissEnabled,
            placementPreference = placementPreference,
        )
    }
}

enum class KeypadPlacementPreference {
    DEFAULT,
    LEFT,
}

class ConfirmQuantityKeypadHandle internal constructor(
    target: View,
    palette: NumericKeypadPalette,
    listener: ConfirmQuantityKeypadView.Listener,
    private val onDismiss: () -> Unit,
    private val outsideDismissEnabled: Boolean,
    private val placementPreference: KeypadPlacementPreference,
) {
    private var target: View? = target
    private val popup = ConfirmQuantityKeypadPopup(target.context, palette)
    private var unbound = false
    private var isDismissing = false
    private var backCallback: OnBackPressedCallback? = null

    init {
        popup.setListener(listener)
        popup.setOnDismissListener {
            isDismissing = false
            backCallback?.isEnabled = false
            if (!unbound) {
                onDismiss()
            }
        }
        target.doOnDetach { unbind() }
    }

    val isShowing: Boolean
        get() = popup.isShowing

    fun show(
        anchor: View,
        decimalEnabled: Boolean,
        spotlightRect: Rect? = null,
    ) {
        if (unbound) return
        popup.setDecimalEnabled(decimalEnabled)
        popup.show(
            anchor = anchor,
            spotlightRect = spotlightRect,
            outsideDismissEnabled = outsideDismissEnabled,
            placementPreference = placementPreference,
        )
        ensureBackCallback(anchor)
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        popup.applyPalette(palette)
    }

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        popup.dismiss()
        if (!popup.isShowing) {
            isDismissing = false
        }
        backCallback?.isEnabled = popup.isShowing
    }

    fun unbind() {
        if (unbound) return
        unbound = true
        backCallback?.remove()
        backCallback = null
        popup.setListener(null)
        popup.setOnDismissListener(null)
        popup.dismiss()
        target = null
    }

    private fun ensureBackCallback(anchor: View) {
        val callback = backCallback
        if (callback != null) {
            callback.isEnabled = popup.isShowing
            return
        }
        val owner = anchor.findViewTreeOnBackPressedDispatcherOwner() ?: return
        backCallback = object : OnBackPressedCallback(popup.isShowing) {
            override fun handleOnBackPressed() {
                dismiss()
            }
        }.also { owner.onBackPressedDispatcher.addCallback(it) }
    }
}
