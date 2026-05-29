package com.holderzone.foundation.core.navigation

import androidx.navigation.NavController

fun NavController.handleNavigationEvent(event: NavigationEvent): Boolean {
    return when (event) {
        is NavigationEvent.NavigateTo -> {
            navigate(event.route, event.navOptions)
            true
        }

        is NavigationEvent.NavigateBack -> {
            val target = previousBackStackEntry
            val popped = popBackStack()
            val receiver = if (popped) currentBackStackEntry else target
            event.result?.forEach { (key, value) ->
                receiver?.savedStateHandle?.set(key, value)
            }
            popped
        }

        is NavigationEvent.NavigateBackTo -> {
            val popped = popBackStack(event.route, event.inclusive)
            event.result.forEach { (key, value) ->
                currentBackStackEntry?.savedStateHandle?.set(key, value)
            }
            popped
        }
    }
}
