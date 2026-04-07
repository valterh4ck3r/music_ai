package com.valter.music_ai.ui.features.song

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.valter.music_ai.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongUiState(
    val song: Song? = null,
    val isPlaying: Boolean = true,
    val progressMs: Long = 0L,
    val totalMs: Long = 234000L // fallback to ~3:54 if null
)

@HiltViewModel
class SongViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongUiState())
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        val songBase64: String? = savedStateHandle["songBase64"]
        songBase64?.let { base64 ->
            try {
                // Decode from URL_SAFE Base64
                val json = String(Base64.decode(base64, Base64.URL_SAFE or Base64.NO_WRAP))
                val song = Gson().fromJson(json, Song::class.java)
                _uiState.update { 
                    it.copy(
                        song = song,
                        totalMs = song.trackTimeMillis ?: 234000L
                    )
                }
                startProgress()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value
        _uiState.update { it.copy(isPlaying = !current.isPlaying) }
        if (!current.isPlaying) {
            startProgress()
        } else {
            progressJob?.cancel()
        }
    }
    
    fun seekTo(progressMs: Long) {
        _uiState.update { it.copy(progressMs = progressMs) }
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_uiState.value.isPlaying && _uiState.value.progressMs < _uiState.value.totalMs) {
                delay(1000)
                _uiState.update { it.copy(progressMs = it.progressMs + 1000) }
            }
        }
    }
}
