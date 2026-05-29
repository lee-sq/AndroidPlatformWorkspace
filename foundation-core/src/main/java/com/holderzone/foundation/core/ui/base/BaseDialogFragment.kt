package com.holderzone.foundation.core.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.holderzone.foundation.core.R

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Dialog binding is only valid between onCreateView and onDestroyView."
        }

    protected open val widthDp: Int = 480
    protected open val heightDp: Int? = null
    protected open val gravity: Int = Gravity.CENTER
    protected open val dimAmount: Float = 0.56f
    protected open val cancelOnTouchOutside: Boolean = true
    protected open val windowAnimationStyle: Int? = null

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), R.style.ThemeOverlay_Foundation_Dialog) {
            override fun show() {
                window?.apply {
                    setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    )
                    ImmersiveMode.inheritActivitySystemUiVisibility(
                        this@BaseDialogFragment.activity,
                        this,
                    )
                }
                super.show()
                window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                applyImmersiveMode(activeDialogWindow = window)
            }

            override fun onWindowFocusChanged(hasFocus: Boolean) {
                super.onWindowFocusChanged(hasFocus)
                if (hasFocus) {
                    applyImmersiveMode(window)
                }
            }
        }.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(cancelOnTouchOutside)
        }
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBindingCreated(binding, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setGravity(gravity)
            windowAnimationStyle?.let { attributes.windowAnimations = it }
            attributes = attributes.apply {
                width = (widthDp * resources.displayMetrics.density).toInt()
                height = heightDp?.let { (it * resources.displayMetrics.density).toInt() }
                    ?: WindowManager.LayoutParams.WRAP_CONTENT
                dimAmount = this@BaseDialogFragment.dimAmount
            }
            applyImmersiveMode(this)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        applyImmersiveMode(activeDialogWindow = null)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) = Unit

    private fun applyImmersiveMode(activeDialogWindow: Window? = dialog?.window) {
        ImmersiveMode.applyToActivityAndWindow(activity, activeDialogWindow)
    }
}
