package com.valter.music_ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    init {
        viewModelScope.launch {
            // Delay handles the 2 seconds splash screen
            delay(1000)
            _showSplash.value = false
        }
    }
}
