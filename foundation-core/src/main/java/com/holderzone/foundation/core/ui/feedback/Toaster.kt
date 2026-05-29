package com.holderzone.foundation.core.ui.feedback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object Toaster {
    private val _events = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun showLoading(tips: String? = null) {
        _events.emit(ToastEvent.ShowLoading(tips))
    }

    suspend fun dismissLoading() {
        _events.emit(ToastEvent.DismissLoading)
    }

    suspend fun showSuccess(message: String, duration: Duration = Duration.SHORT) {
        _events.emit(ToastEvent.ShowToast(message, ToastType.SUCCESS, duration))
    }

    suspend fun showError(message: String, duration: Duration = Duration.SHORT) {
        _events.emit(ToastEvent.ShowToast(message, ToastType.ERROR, duration))
    }

    suspend fun showInfo(message: String, duration: Duration = Duration.SHORT) {
        _events.emit(ToastEvent.ShowToast(message, ToastType.INFO, duration))
    }
}
