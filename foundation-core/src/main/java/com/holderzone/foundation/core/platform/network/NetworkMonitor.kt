package com.holderzone.foundation.core.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _status = MutableStateFlow(currentStatus())
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        if (callback != null) return
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _status.value = currentStatus()
            }

            override fun onLost(network: Network) {
                _status.value = currentStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                _status.value = currentStatus()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(requireNotNull(callback))
    }

    fun stop() {
        callback?.let(connectivityManager::unregisterNetworkCallback)
        callback = null
    }

    fun currentStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork ?: return NetworkStatus.UNAVAILABLE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkStatus.UNAVAILABLE
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            NetworkStatus.AVAILABLE
        } else {
            NetworkStatus.UNAVAILABLE
        }
    }
}
