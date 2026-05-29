package com.holderzone.foundation.core.ui.feedback

sealed interface ToastEvent {
    data class ShowToast(
        val message: String,
        val type: ToastType = ToastType.INFO,
        val duration: Duration = Duration.SHORT,
    ) : ToastEvent

    data class ShowLoading(val tips: String? = null) : ToastEvent

    data object DismissLoading : ToastEvent
}
