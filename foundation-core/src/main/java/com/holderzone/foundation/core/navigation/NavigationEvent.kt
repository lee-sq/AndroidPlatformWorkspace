package com.holderzone.foundation.core.navigation

import androidx.navigation.NavOptions

sealed interface NavigationEvent {
    data class NavigateTo(
        val route: String,
        val navOptions: NavOptions? = null,
    ) : NavigationEvent

    data class NavigateBack(
        val result: Map<String, Any>? = null,
    ) : NavigationEvent

    data class NavigateBackTo(
        val route: String,
        val inclusive: Boolean = false,
        val result: Map<String, Any> = emptyMap(),
    ) : NavigationEvent
}
