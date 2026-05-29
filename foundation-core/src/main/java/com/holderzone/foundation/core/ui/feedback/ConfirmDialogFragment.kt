package com.holderzone.foundation.core.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.holderzone.foundation.core.databinding.DialogConfirmBinding
import com.holderzone.foundation.core.ui.base.BaseDialogFragment

class ConfirmDialogFragment : BaseDialogFragment<DialogConfirmBinding>() {
    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): DialogConfirmBinding {
        return DialogConfirmBinding.inflate(inflater, container, false)
    }

    override fun onBindingCreated(binding: DialogConfirmBinding, savedInstanceState: Bundle?) {
        binding.dialogTitle.text = requireArguments().getString(ARG_TITLE).orEmpty()
        binding.dialogMessage.text = requireArguments().getString(ARG_MESSAGE).orEmpty()
        binding.cancelButton.text = requireArguments().getString(ARG_CANCEL_TEXT).orEmpty()
        binding.confirmButton.text = requireArguments().getString(ARG_CONFIRM_TEXT).orEmpty()
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                requireArguments().getString(ARG_REQUEST_KEY).orEmpty(),
                Bundle().apply {
                    putBoolean(RESULT_CONFIRMED, true)
                    putString(RESULT_TARGET_ID, requireArguments().getString(ARG_TARGET_ID).orEmpty())
                },
            )
            dismiss()
        }
    }

    companion object {
        const val RESULT_CONFIRMED = "result_confirmed"
        const val RESULT_TARGET_ID = "result_target_id"

        private const val ARG_REQUEST_KEY = "arg_request_key"
        private const val ARG_TARGET_ID = "arg_target_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_CANCEL_TEXT = "arg_cancel_text"
        private const val ARG_CONFIRM_TEXT = "arg_confirm_text"

        fun newInstance(
            requestKey: String,
            targetId: String = "",
            title: String,
            message: String,
            cancelText: String,
            confirmText: String,
        ): ConfirmDialogFragment {
            return ConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REQUEST_KEY, requestKey)
                    putString(ARG_TARGET_ID, targetId)
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_CANCEL_TEXT, cancelText)
                    putString(ARG_CONFIRM_TEXT, confirmText)
                }
            }
        }
    }
}
