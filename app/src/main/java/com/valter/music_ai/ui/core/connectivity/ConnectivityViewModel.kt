package com.valter.music_ai.ui.core.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectivityStatus {
    CONNECTED,    // Just reconnected — shows green banner for 3s then hides
    DISCONNECTED, // No internet — show grey banner indefinitely
    HIDDEN        // No banner visible
}


@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    private val observer: NetworkConnectivityObserver
) : ViewModel() {

    private val _status = MutableStateFlow(ConnectivityStatus.HIDDEN)
    val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    private var wasConnected: Boolean? = null
    private var secondsDelay: Long = 3000

    init {
        viewModelScope.launch {
            observer.isConnected.collect { connected ->
                if (!connected) {
                    // Lost connection — show grey banner immediately
                    _status.value = ConnectivityStatus.DISCONNECTED
                    wasConnected = false
                } else {
                    if (wasConnected == false) {
                        // Reconnected after being offline — show green banner for @secondsDelay
                        _status.value = ConnectivityStatus.CONNECTED
                        // Delay show banner
                        delay(secondsDelay)
                        _status.value = ConnectivityStatus.HIDDEN
                    } else if (wasConnected == null) {
                        // First emission: online from the start, show nothing
                        _status.value = ConnectivityStatus.HIDDEN
                    }
                    wasConnected = true
                }
            }
        }
    }

}
