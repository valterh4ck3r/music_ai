package com.valter.music_ai.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * SOLID - Interface Segregation / Dependency Inversion
 * Abstracts the platform implementation away from consumers
 */
interface NetworkConnectivityObserver {
    val isConnected: Flow<Boolean>
}
