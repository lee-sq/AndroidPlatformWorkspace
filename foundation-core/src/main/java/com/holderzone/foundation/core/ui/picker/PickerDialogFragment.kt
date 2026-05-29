package com.holderzone.foundation.core.ui.picker

import android.view.Gravity
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.ui.base.BaseDialogFragment

abstract class PickerDialogFragment<VB : ViewBinding> : BaseDialogFragment<VB>() {
    override val widthDp: Int
        get() = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()

    override val gravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    override val dimAmount: Float = 0.46f
    override val windowAnimationStyle: Int = R.style.Animation_Foundation_Picker

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
