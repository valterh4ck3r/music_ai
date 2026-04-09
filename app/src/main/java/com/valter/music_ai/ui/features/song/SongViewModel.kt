package com.valter.music_ai.ui.features.song

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.google.gson.Gson
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
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

enum class SongUiStatus { LOADING, PLAYING, SUCCESS, ERROR }

data class SongUiState(
    val song: Song? = null,
    val progressMs: Long = 0L,
    val totalMs: Long = 0L,
    val isRepeatEnabled: Boolean = false,
    val status: SongUiStatus = SongUiStatus.LOADING
)

@HiltViewModel
class SongViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private var player: Player? = null
    private val _uiState = MutableStateFlow(SongUiState())
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateStatus(isPlaying, player?.playbackState ?: Player.STATE_IDLE)
            if (isPlaying) startProgressUpdater() else stopProgressUpdater()
        }

        override fun onPlaybackStateChanged(state: Int) {
            updateStatus(player?.isPlaying ?: false, state)
            if (state == Player.STATE_READY) {
                _uiState.update { it.copy(totalMs = player?.duration ?: 0L) }
            }
            if (state == Player.STATE_ENDED) {
                if (_uiState.value.isRepeatEnabled) {
                    player?.seekTo(0)
                    player?.play()
                } else {
                    nextSong()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val song = playlist.find { it.trackId.toString() == item.mediaId }
                if (song != null) {
                    _uiState.update { it.copy(song = song) }
                    markAsPlayed(song)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _uiState.update { it.copy(status = SongUiStatus.ERROR) }
        }
    }

    init {
        observeConnection()
        val songBase64: String? = savedStateHandle["songBase64"]
        songBase64?.let { base64 ->
            try {
                val json = String(Base64.decode(base64, Base64.URL_SAFE or Base64.NO_WRAP))
                val song = Gson().fromJson(json, Song::class.java)
                _uiState.update { it.copy(song = song) }
                loadPlaylist(song.collectionName ?: song.artistName, song.trackId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPlayer(player: Player?) {
        this.player?.removeListener(playerListener)
        this.player = player
        this.player?.addListener(playerListener)
        
        // Sync initial state
        player?.let { p ->
            _uiState.update {
                it.copy(
                    totalMs = p.duration.coerceAtLeast(0L),
                    progressMs = p.currentPosition.coerceAtLeast(0L)
                )
            }
            updateStatus(p.isPlaying, p.playbackState)
            if (p.isPlaying) startProgressUpdater()
            
            checkAndPlayInitialSong()
        }
    }

    private fun updateStatus(isPlaying: Boolean, playbackState: Int) {
        val newStatus = when {
            playbackState == Player.STATE_BUFFERING -> SongUiStatus.LOADING
            isPlaying -> SongUiStatus.PLAYING
            playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED -> SongUiStatus.SUCCESS
            else -> SongUiStatus.LOADING
        }
        _uiState.update { it.copy(status = newStatus) }
    }

    private fun checkAndPlayInitialSong() {
        val currentPlayer = player ?: return
        val currentSong = _uiState.value.song ?: return
        
        val currentMediaId = currentPlayer.currentMediaItem?.mediaId
        if (currentMediaId != currentSong.trackId.toString()) {
            prepareSong(currentSong)
            markAsPlayed(currentSong)
        } else {
             _uiState.update { it.copy(song = currentSong) }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                if (connected && _uiState.value.status == SongUiStatus.ERROR) {
                    _uiState.value.song?.let { prepareSong(it) }
                }
            }
        }
    }

    private fun prepareSong(song: Song) {
        _uiState.update {
            it.copy(
                song = song,
                progressMs = 0L,
                totalMs = 0L,
                status = SongUiStatus.LOADING
            )
        }
        val playbackUrl = song.previewUrlLocal ?: song.previewUrl
        playbackUrl?.let { url ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.trackName)
                .setArtist(song.artistName)
                .setArtworkUri(android.net.Uri.parse(song.artworkUrl100))
                .setAlbumTitle(song.collectionName)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(song.trackId.toString())
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()
            
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }
    }

    private fun loadPlaylist(query: String, currentTrackId: Long) {
        viewModelScope.launch {
            repository.searchSongs(query, limit = 200, offset = 0)
                .catch { /* ignore */ }
                .collect { state ->
                    if (state is ResponseState.Success) {
                        playlist = state.data
                        currentIndex = playlist.indexOfFirst { it.trackId == currentTrackId }
                    }
                }
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun pause() {
        player?.pause()
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
        prepareSong(nextSong)
        markAsPlayed(nextSong)
    }

    private fun markAsPlayed(song: Song) {
        viewModelScope.launch { repository.markSongAsPlayed(song) }
    }

    fun seekTo(progressMs: Long) {
        player?.seekTo(progressMs)
        _uiState.update { it.copy(progressMs = progressMs) }
    }

    fun toggleRepeat() {
        _uiState.update { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(progressMs = player?.currentPosition ?: 0L) }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        player?.removeListener(playerListener)
        player = null
    }
}
