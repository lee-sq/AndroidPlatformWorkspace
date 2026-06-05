package com.holderzone.foundation.core.ui.feedback

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.databinding.DialogConfirmBinding
import com.permissionx.guolindev.dialog.RationaleDialog

internal class FoundationPermissionDialog(
    context: Context,
    private val permissions: List<String>,
    private val title: String,
    private val message: String,
    private val positiveText: String,
    private val negativeText: String?,
) : RationaleDialog(context, R.style.ThemeOverlay_Foundation_Dialog) {
    private lateinit var binding: DialogConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindContent()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setGravity(Gravity.CENTER)
            attributes = attributes.apply {
                width = (context.resources.displayMetrics.widthPixels * WIDTH_RATIO).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
                dimAmount = DIM_AMOUNT
            }
        }
    }

    override fun getPositiveButton(): View = binding.confirmButton

    override fun getNegativeButton(): View? = negativeText?.let { binding.cancelButton }

    override fun getPermissionsToRequest(): List<String> = permissions

    private fun bindContent() {
        binding.dialogTitle.text = title
        binding.dialogMessage.text = message
        binding.confirmButton.text = positiveText
        if (negativeText == null) {
            binding.cancelButton.visibility = View.GONE
        } else {
            binding.cancelButton.visibility = View.VISIBLE
            binding.cancelButton.text = negativeText
        }
    }

    private companion object {
        private const val WIDTH_RATIO = 0.86f
        private const val DIM_AMOUNT = 0.56f
    }
}
