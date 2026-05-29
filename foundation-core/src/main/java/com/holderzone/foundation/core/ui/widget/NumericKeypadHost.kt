package com.holderzone.foundation.core.ui.widget

import android.text.Editable
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.core.view.doOnDetach

/**
 * 将 EditText 绑定到悬浮数字键盘，并管理焦点、输入和生命周期。
 */
object NumericKeypadHost {

    fun attach(
        target: EditText,
        palette: NumericKeypadPalette,
    ): KeypadHandle {
        return KeypadHandle(target, palette)
    }
}

class KeypadHandle internal constructor(
    target: EditText,
    palette: NumericKeypadPalette,
) {
    private var target: EditText? = target
    private val popup = NumericKeypadPopup(target.context, palette)
    private val originalShowSoftInputOnFocus = target.showSoftInputOnFocus
    private val originalFocusChangeListener = target.onFocusChangeListener
    private var isDismissing = false
    private var unbound = false
    private var backCallback: OnBackPressedCallback? = null

    private val focusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
        originalFocusChangeListener?.onFocusChange(view, hasFocus)
        val editText = targetOrNull() ?: return@OnFocusChangeListener
        when {
            unbound -> Unit
            hasFocus && !isDismissing && editText.canShowKeypad() -> show()
            !hasFocus -> dismiss(clearFocus = false)
        }
    }

    private val keypadListener = object : NumericKeypadView.Listener {
        override fun onDigit(digit: Char) {
            targetOrNull()?.replaceSelection(digit.toString())
        }

        override fun onDelete() {
            targetOrNull()?.deleteSelectionOrPrevious()
        }
    }

    init {
        target.showSoftInputOnFocus = false
        target.onFocusChangeListener = focusChangeListener
        popup.setListener(keypadListener)
        popup.setOnDismissListener {
            if (!unbound) {
                dismiss(clearFocus = true)
            }
            isDismissing = false
        }
        target.doOnDetach {
            unbind()
        }
    }

    fun applyPalette(palette: NumericKeypadPalette) {
        popup.applyPalette(palette)
    }

    fun dismiss() {
        dismiss(clearFocus = true)
    }

    fun unbind() {
        if (unbound) return
        unbound = true
        target?.let { editText ->
            if (editText.onFocusChangeListener === focusChangeListener) {
                editText.onFocusChangeListener = originalFocusChangeListener
            }
            editText.showSoftInputOnFocus = originalShowSoftInputOnFocus
        }
        backCallback?.remove()
        backCallback = null
        popup.setListener(null)
        popup.setOnDismissListener(null)
        popup.dismiss()
        target = null
    }

    private fun show() {
        val editText = targetOrNull() ?: return
        if (!editText.canShowKeypad()) {
            dismiss(clearFocus = false)
            return
        }
        popup.show(editText)
        ensureBackCallback(editText)
    }

    private fun dismiss(clearFocus: Boolean) {
        if (isDismissing) return
        isDismissing = true
        popup.dismiss()
        val editText = target
        if (clearFocus && editText?.hasFocus() == true) {
            editText.clearFocus()
        }
        if (!popup.isShowing) {
            isDismissing = false
        }
        backCallback?.isEnabled = popup.isShowing
    }

    private fun targetOrNull(): EditText? = target?.takeUnless { unbound }

    private fun ensureBackCallback(editText: EditText) {
        val callback = backCallback
        if (callback != null) {
            callback.isEnabled = popup.isShowing
            return
        }
        val owner = editText.findViewTreeOnBackPressedDispatcherOwner() ?: return
        backCallback = object : OnBackPressedCallback(popup.isShowing) {
            override fun handleOnBackPressed() {
                dismiss(clearFocus = true)
            }
        }.also { owner.onBackPressedDispatcher.addCallback(it) }
    }
}

private fun EditText.canShowKeypad(): Boolean {
    return isAttachedToWindow && isEnabled && visibility == View.VISIBLE
}

private fun EditText.replaceSelection(text: String) {
    val editable = this.text ?: return
    val start = selectionStart.coerceAtLeast(0)
    val end = selectionEnd.coerceAtLeast(0)
    val replaceStart = minOf(start, end).coerceAtMost(editable.length)
    val replaceEnd = maxOf(start, end).coerceAtMost(editable.length)
    val oldLength = editable.length
    editable.replace(replaceStart, replaceEnd, text)
    val selection = if (replaceStart == replaceEnd && editable.length == oldLength) {
        replaceStart
    } else {
        replaceStart + text.length
    }
    setSelection(selection.coerceAtMost(editable.length))
}

private fun EditText.deleteSelectionOrPrevious() {
    val editable: Editable = text ?: return
    val start = selectionStart.coerceAtLeast(0)
    val end = selectionEnd.coerceAtLeast(0)
    val deleteStart = minOf(start, end).coerceAtMost(editable.length)
    val deleteEnd = maxOf(start, end).coerceAtMost(editable.length)
    when {
        deleteStart != deleteEnd -> {
            editable.delete(deleteStart, deleteEnd)
            setSelection(deleteStart.coerceAtMost(editable.length))
        }
        deleteStart > 0 -> {
            editable.delete(deleteStart - 1, deleteStart)
            setSelection((deleteStart - 1).coerceAtMost(editable.length))
        }
    }
}
