package com.holderzone.foundation.core.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.platform.storage.StringResHelper
import com.holderzone.foundation.core.ui.feedback.Duration
import com.holderzone.foundation.core.ui.feedback.Toaster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

abstract class BaseNetworkViewModel(
    protected val savedStateHandle: SavedStateHandle? = null,
) : BaseViewModel() {
    open fun executeRequest() = Unit

    fun <T> Flow<T>.loading(tips: String? = null): Flow<T> {
        return onStart { Toaster.showLoading(tips) }
            .onCompletion { Toaster.dismissLoading() }
    }

    fun showSuccessToast(
        message: String = StringResHelper.getString(R.string.foundation_toast_success),
        duration: Duration = Duration.SHORT,
    ) {
        viewModelScope.launch { Toaster.showSuccess(message, duration) }
    }

    fun showErrorToast(
        message: String = StringResHelper.getString(R.string.foundation_toast_error),
        duration: Duration = Duration.SHORT,
    ) {
        viewModelScope.launch { Toaster.showError(message, duration) }
    }

    fun showInfoToast(
        message: String = StringResHelper.getString(R.string.foundation_toast_info),
        duration: Duration = Duration.SHORT,
    ) {
        viewModelScope.launch { Toaster.showInfo(message, duration) }
    }

    fun getStringRes(@StringRes resId: Int): String = StringResHelper.getString(resId)

    fun getStringRes(@StringRes resId: Int, vararg formatArgs: Any): String =
        StringResHelper.getString(resId, *formatArgs)
}
