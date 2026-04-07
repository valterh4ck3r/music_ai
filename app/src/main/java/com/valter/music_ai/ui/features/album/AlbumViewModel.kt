package com.valter.music_ai.ui.features.album

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import com.valter.music_ai.ui.features.home.model.SongUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiData(
    val albumTitle: String = "",
    val artistName: String = "",
    val albumArtUrl: String? = null,
    val songs: List<SongUi> = emptyList()
)

typealias AlbumUiState = ResponseState<AlbumUiData>

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(ResponseState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        val songBase64: String? = savedStateHandle["songBase64"]
        songBase64?.let { base64 ->
            try {
                val json = String(Base64.decode(base64, Base64.URL_SAFE or Base64.NO_WRAP))
                val song = Gson().fromJson(json, Song::class.java)
                
                val albumQuery = song.collectionName ?: song.trackName
                loadAlbum(albumQuery, song)
            } catch (e: Exception) {
                _uiState.value = ResponseState.Error(message = "Failed to load album info")
            }
        }
    }

    private fun loadAlbum(query: String, initialSong: Song) {
        viewModelScope.launch {
            repository.searchSongs(query, limit = 50, offset = 0)
                .collect { state ->
                    when (state) {
                        is ResponseState.Loading -> {
                            // Keep loading
                        }
                        is ResponseState.Success -> {
                            val songs = state.data.map { s ->
                                SongUi(
                                    id = s.trackId.toString(),
                                    title = s.trackName,
                                    artist = s.artistName,
                                    albumArtUrl = s.artworkUrl100,
                                    originalSong = s
                                )
                            }
                            _uiState.value = ResponseState.Success(
                                AlbumUiData(
                                    albumTitle = initialSong.collectionName ?: initialSong.trackName,
                                    artistName = initialSong.artistName,
                                    albumArtUrl = initialSong.artworkUrl100,
                                    songs = songs
                                )
                            )
                        }
                        is ResponseState.Error -> {
                            _uiState.value = ResponseState.Error(
                                statusCode = state.statusCode,
                                message = state.message
                            )
                        }
                    }
                }
        }
    }
}
