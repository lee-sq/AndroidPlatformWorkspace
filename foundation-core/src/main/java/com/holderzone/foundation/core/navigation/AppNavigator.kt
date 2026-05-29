package com.holderzone.foundation.core.navigation

import androidx.navigation.NavOptions
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppNavigator {
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    suspend fun navigateTo(route: String, navOptions: NavOptions? = null) {
        _navigationEvents.emit(NavigationEvent.NavigateTo(route, navOptions))
    }

    suspend fun navigateBack() {
        _navigationEvents.emit(NavigationEvent.NavigateBack())
    }

    suspend fun navigateBack(result: Map<String, Any>) {
        _navigationEvents.emit(NavigationEvent.NavigateBack(result))
    }

    suspend fun navigateBackTo(
        route: String,
        inclusive: Boolean = false,
        result: Map<String, Any> = emptyMap(),
    ) {
        _navigationEvents.emit(NavigationEvent.NavigateBackTo(route, inclusive, result))
    }
}
