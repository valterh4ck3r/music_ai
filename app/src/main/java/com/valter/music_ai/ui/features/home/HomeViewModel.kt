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

data class HomeUiState(
    val songs: List<SongUi> = emptyList(),
    val recentlyPlayed: List<SongUi> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val canLoadMore: Boolean = true
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    val isConnected = connectivityObserver.isConnected

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private var searchJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val DEFAULT_SEARCH_TERM = "pop"
    }

    init {
        loadRecentlyPlayed()
        searchSongs(DEFAULT_SEARCH_TERM)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

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
        val state = _uiState.value
        if (state.isLoadingMore || !state.canLoadMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        val query = state.searchQuery.ifBlank { DEFAULT_SEARCH_TERM }

        viewModelScope.launch {
            repository.searchSongs(query, PAGE_SIZE, currentOffset)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, error = e.message)
                    }
                }
                .collect { result ->
                    result.onSuccess { songs ->
                        currentOffset += songs.size
                        _uiState.update { state ->
                            state.copy(
                                songs = state.songs + songs.toUiList(),
                                isLoadingMore = false,
                                canLoadMore = songs.size >= PAGE_SIZE,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(isLoadingMore = false, error = e.message)
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
        _uiState.update { it.copy(error = null) }
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
            _uiState.update { it.copy(searchQuery = query) }

            searchSongs(query, isRefresh = true)
        }
    }

    private fun searchSongs(term: String, isRefresh: Boolean = false) {
        currentOffset = 0
        if (isRefresh) {
            _uiState.update { it.copy(isRefreshing = true, canLoadMore = true) }
        } else {
            _uiState.update { it.copy(isLoading = true, songs = emptyList(), canLoadMore = true) }
        }

        viewModelScope.launch {
            repository.searchSongs(term, PAGE_SIZE, currentOffset, forceRemote = isRefresh)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = e.message)
                    }
                }
                .collect { result ->
                    result.onSuccess { songs ->
                        currentOffset = songs.size
                        _uiState.update {
                            it.copy(
                                songs = songs.toUiList(),
                                isLoading = false,
                                isRefreshing = false,
                                canLoadMore = songs.size >= PAGE_SIZE,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, error = e.message)
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
                    _uiState.update {
                        it.copy(recentlyPlayed = songs.toUiList())
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
                albumArtUrl = song.artworkUrl100
            )
        }
    }

}
