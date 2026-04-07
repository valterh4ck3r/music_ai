package com.valter.music_ai.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.ui.features.home.model.HomeUiState
import com.valter.music_ai.ui.features.home.model.HomeUiData
import com.valter.music_ai.ui.features.home.model.SongUi


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    val isConnected = connectivityObserver.isConnected

    private val _uiState = MutableStateFlow<ResponseState<HomeUiData>>(ResponseState.Loading)
    val uiState: StateFlow<ResponseState<HomeUiData>> = _uiState.asStateFlow()

    private var uiData = HomeUiData()

    private var currentOffset = 0
    private var searchJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 800L
        private const val DEFAULT_SEARCH_TERM = "pop"
    }

    init {
        loadRecentlyPlayed()
        searchSongs(DEFAULT_SEARCH_TERM)
    }

    fun onSearchQueryChanged(query: String) {
        uiData = uiData.copy(searchQuery = query)
        if (_uiState.value is ResponseState.Success) {
            _uiState.value = ResponseState.Success(uiData)
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            if (query.isBlank()) {
                searchSongs(DEFAULT_SEARCH_TERM)
            } else {
                searchSongs(query)
            }
        }
    }

    fun loadNextPage() {
        if (uiData.isLoadingMore || !uiData.canLoadMore) return

        uiData = uiData.copy(isLoadingMore = true)
        if (_uiState.value is ResponseState.Success) {
            _uiState.value = ResponseState.Success(uiData)
        }

        val query = uiData.searchQuery.ifBlank { DEFAULT_SEARCH_TERM }

        viewModelScope.launch {
            repository.searchSongs(query, PAGE_SIZE, currentOffset)
                // wait, if searchSongs emits flow, let's collect it
                .collect { state ->
                    when (state) {
                        is ResponseState.Success -> {
                            val newSongs = state.data.toUiList()
                            currentOffset += newSongs.size
                            uiData = uiData.copy(
                                songs = uiData.songs + newSongs,
                                isLoadingMore = false,
                                canLoadMore = newSongs.size >= PAGE_SIZE
                            )
                            _uiState.value = ResponseState.Success(uiData)
                        }
                        is ResponseState.Error -> {
                            uiData = uiData.copy(isLoadingMore = false)
                            // We still want to maintain the existing items if they exist
                            // so usually we'd keep Success state to keep showing songs, or transition to Error.
                            // Emitting Error here will blank out screen. Let's just emit Error as requested.
                            _uiState.value = ResponseState.Error(statusCode = state.statusCode, message = state.message)
                        }
                        is ResponseState.Loading -> {
                            // Handled by isLoadingMore
                        }
                    }
                }
        }
    }

    fun onSongClick(song: SongUi) {
        viewModelScope.launch {
            repository.markSongAsPlayed(song.id.toLongOrNull() ?: return@launch)
        }
    }

    fun onSongMoreClick(song: SongUi) {
        // TODO: Handle more options (add to playlist, share, etc.)
    }

    fun clearError() {
        _uiState.value = ResponseState.Success(uiData)
    }

    fun refreshSongs() {
        viewModelScope.launch {
            // Clears the recently played cache and updates the UI
            repository.clearRecentlyPlayed()
            loadRecentlyPlayed()

            // Predefined list of popular artists/genres to create a dynamic feed
            val randomTerms = listOf(
                "Drake", "The Beatles", "Daft Punk", "Rihanna", 
                "Queen", "Coldplay", "Metallica", "Eminem", 
                "Bruno Mars", "Pop", "Rock", "Jazz", "Lofi"
            )
            // Pick a random term to simulate a "For You" discovery feed on refresh
            val query = randomTerms.random()
            
            // Update the search query state so the UI reflects what is currently being shown
            uiData = uiData.copy(searchQuery = query)
            if (_uiState.value is ResponseState.Success) {
                _uiState.value = ResponseState.Success(uiData)
            }

            searchSongs(query, isRefresh = true)
        }
    }

    private fun searchSongs(term: String, isRefresh: Boolean = false) {
        currentOffset = 0
        if (isRefresh) {
            uiData = uiData.copy(isRefreshing = true, canLoadMore = true)
            if (_uiState.value is ResponseState.Success) {
                _uiState.value = ResponseState.Success(uiData)
            }
        } else {
            uiData = uiData.copy(songs = emptyList(), canLoadMore = true)
            _uiState.value = ResponseState.Loading
        }

        viewModelScope.launch {
            // Note: searchSongs now returns ResponseState inside the flow
            repository.searchSongs(term, PAGE_SIZE, currentOffset, forceRemote = isRefresh)
                .collect { state ->
                    when (state) {
                        is ResponseState.Success -> {
                            val newSongs = state.data.toUiList()
                            currentOffset = newSongs.size
                            uiData = uiData.copy(
                                songs = newSongs,
                                isRefreshing = false,
                                canLoadMore = newSongs.size >= PAGE_SIZE
                            )
                            _uiState.value = ResponseState.Success(uiData)
                        }
                        is ResponseState.Error -> {
                            _uiState.value = ResponseState.Error(statusCode = state.statusCode, message = state.message)
                        }
                        is ResponseState.Loading -> {
                            if (!isRefresh) {
                                _uiState.value = ResponseState.Loading
                            }
                        }
                    }
                }
        }
    }

    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            repository.getRecentlyPlayedSongs()
                .catch { /* silently fail, recently played is optional */ }
                .collect { songs ->
                    uiData = uiData.copy(recentlyPlayed = songs.toUiList())
                    if (_uiState.value is ResponseState.Success) {
                        _uiState.value = ResponseState.Success(uiData)
                    }
                }
        }
    }

    private fun List<Song>.toUiList(): List<SongUi> {
        return map { song ->
            SongUi(
                id = song.trackId.toString(),
                title = song.trackName,
                artist = song.artistName,
                albumArtUrl = song.artworkUrl100,
                originalSong = song
            )
        }
    }

}
