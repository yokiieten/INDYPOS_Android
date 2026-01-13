package com.indybrain.indypos_Android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global network connectivity monitor.
 *
 * Exposes current online/offline state via [isOnline] so the UI layer
 * can disable actions or show an overlay when there is no internet.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnection())
    val isOnline: StateFlow<Boolean> = _isOnline

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            // When current active network is lost, mark as offline.
            // This is simple but works well for our use-case.
            _isOnline.value = false
        }
    }

    init {
        // Register default callback to monitor connectivity changes.
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun checkInitialConnection(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            activeNetwork != null
        } catch (_: Exception) {
            false
        }
    }
}

