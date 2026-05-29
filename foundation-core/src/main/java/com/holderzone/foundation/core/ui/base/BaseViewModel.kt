package com.holderzone.foundation.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holderzone.foundation.core.navigation.AppNavigator
import com.holderzone.foundation.core.platform.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected fun launchOnMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) { block() }
    }

    protected open val log = AppLogger

    protected fun launchOnIo(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            runCatching { block() }.onFailure(onError)
        }
    }

    protected fun launchOnIO(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return launchOnIo(onError, block)
    }

    fun navigateBack() {
        viewModelScope.launch { AppNavigator.navigateBack() }
    }

    fun navigateBack(result: Map<String, Any>) {
        viewModelScope.launch { AppNavigator.navigateBack(result) }
    }

    fun navigateBackTo(
        route: String,
        inclusive: Boolean = false,
        result: Map<String, Any> = emptyMap(),
    ) {
        viewModelScope.launch { AppNavigator.navigateBackTo(route, inclusive, result) }
    }

    fun navigateTo(route: String) {
        viewModelScope.launch { AppNavigator.navigateTo(route) }
    }
}
