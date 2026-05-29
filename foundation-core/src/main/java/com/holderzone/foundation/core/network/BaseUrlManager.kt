package com.holderzone.foundation.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BaseUrlManager(initialBaseUrl: String? = null) {
    private val _currentBaseUrl = MutableStateFlow(initialBaseUrl?.normalizeBaseUrl())
    val currentBaseUrl: StateFlow<String?> = _currentBaseUrl.asStateFlow()

    fun setBaseUrl(baseUrl: String) {
        _currentBaseUrl.value = baseUrl.normalizeBaseUrl()
    }

    fun clearBaseUrl() {
        _currentBaseUrl.value = null
    }

    fun getCurrentBaseUrl(): String? = _currentBaseUrl.value

    fun hasBaseUrl(): Boolean = _currentBaseUrl.value != null
}

internal fun String.normalizeBaseUrl(): String {
    val trimmed = trim()
    require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        "BaseUrl must start with http:// or https://: $this"
    }
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}
