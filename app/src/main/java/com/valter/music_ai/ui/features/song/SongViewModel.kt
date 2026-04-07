package com.valter.music_ai.ui.features.song

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongUiState(
    val song: Song? = null,
    val isPlaying: Boolean = true,
    val progressMs: Long = 0L,
    val totalMs: Long = 234000L, // fallback to ~3:54 if null
    val isRepeatEnabled: Boolean = false
)

@HiltViewModel
class SongViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongUiState())
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    
    // Playlist logic
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

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
                        totalMs = song.trackTimeMillis ?: 234000L,
                        progressMs = 0L
                    )
                }
                startProgress()
                markAsPlayed(song)

                // Load album playlist
                val albumQuery = song.collectionName ?: song.artistName
                loadPlaylist(albumQuery, song.trackId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPlaylist(query: String, currentTrackId: Long) {
        viewModelScope.launch {
            repository.searchSongs(query, limit = 200, offset = 0)
                .catch { /* ignore */ }
                .collect { state ->
                    if (state is ResponseState.Success) {
                        // Filter to keep only the actual album tracks if possible, 
                        // but iTunes search is fuzzy so we just use the results as a playlist queue.
                        playlist = state.data
                        val index = playlist.indexOfFirst { it.trackId == currentTrackId }
                        currentIndex = if (index >= 0) index else 0
                    }
                }
        }
    }

    fun nextSong() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        updateToCurrentIndexSong()
    }

    fun previousSong() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        updateToCurrentIndexSong()
    }

    private fun updateToCurrentIndexSong() {
        val nextSong = playlist.getOrNull(currentIndex) ?: return
        _uiState.update {
            it.copy(
                song = nextSong,
                totalMs = nextSong.trackTimeMillis ?: 234000L,
                progressMs = 0L
            )
        }
        markAsPlayed(nextSong)
        if (!_uiState.value.isPlaying) {
            togglePlayPause() // auto-play next song
        } else {
            startProgress() // restart progress timer
        }
    }

    private fun markAsPlayed(song: Song) {
        viewModelScope.launch {
            repository.markSongAsPlayed(song)
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

    fun toggleRepeat() {
        _uiState.update { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(1000)
                val current = _uiState.value
                val newProgress = current.progressMs + 1000
                if (newProgress >= current.totalMs) {
                    if (current.isRepeatEnabled) {
                        _uiState.update { it.copy(progressMs = 0L) }
                    } else {
                        nextSong()
                        break
                    }
                } else {
                    _uiState.update { it.copy(progressMs = newProgress) }
                }
            }
        }
    }
}
