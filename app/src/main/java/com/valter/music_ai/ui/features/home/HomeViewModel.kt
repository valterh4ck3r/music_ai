package com.valter.music_ai.ui.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import com.valter.music_ai.ui.features.home.model.HomeUiData
import com.valter.music_ai.ui.features.home.model.SongUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    val isConnected = connectivityObserver.isConnected

    private val _uiState = MutableStateFlow<ResponseState<HomeUiData>>(ResponseState.Loading)
    val uiState: StateFlow<ResponseState<HomeUiData>> = _uiState.asStateFlow()

    private var uiData = HomeUiData()
    fun getUiData() = uiData

    private var allLoadedSongs: List<SongUi> = emptyList()
    private var currentOffset = 0
    private var searchJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 800L
        private val RANDOM_TERMS = listOf(
            "Drake", "The Beatles", "Daft Punk", "Rihanna",
            "Queen", "Coldplay", "Metallica", "Eminem",
            "Bruno Mars", "Pop", "Rock", "Jazz", "Lofi"
        )
    }

    init {
        loadRecentlyPlayed()
        searchSongs(RANDOM_TERMS.random())
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
                searchSongs(RANDOM_TERMS.random())
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

        viewModelScope.launch {
            loadRecentlyPlayed()
            val nextBatch = allLoadedSongs.drop(currentOffset).take(PAGE_SIZE)
            currentOffset += nextBatch.size

            uiData = uiData.copy(
                songs = uiData.songs + nextBatch,
                isLoadingMore = false,
                canLoadMore = currentOffset < allLoadedSongs.size
            )
            _uiState.value = ResponseState.Success(uiData)
        }
    }

    fun onSongClick(song: SongUi) {
        viewModelScope.launch {
            repository.markSongAsPlayed(song.originalSong)
        }
    }

    fun clearError() {
        _uiState.value = ResponseState.Success(uiData)
    }

    fun refreshSongs() {
        viewModelScope.launch {
            // Clears the recently played cache and updates the UI
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
            // Requesting 200 items as requested for memory lazy loading
            repository.searchSongs(term, 200, 0, forceRemote = isRefresh)
                .collect { state ->
                    when (state) {
                        is ResponseState.Success -> {
                            allLoadedSongs = state.data.toUiList()

                            if (currentOffset < PAGE_SIZE) {
                                currentOffset = PAGE_SIZE
                            }

                            val visibleSongs = allLoadedSongs.take(currentOffset)

                            uiData = uiData.copy(
                                songs = visibleSongs,
                                isRefreshing = false,
                                canLoadMore = currentOffset < allLoadedSongs.size
                            )
                            _uiState.value = ResponseState.Success(uiData)
                        }

                        is ResponseState.Error -> {
                            _uiState.value = ResponseState.Error(
                                statusCode = state.statusCode,
                                message = state.message
                            )
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
                .catch { Log.d("HomeViewModel", "Error loading recently played: ${it.message}")}
                .collect { songs ->
                    val recentlyPlayedUi = songs.toUiList()
                    uiData = uiData.copy(recentlyPlayed = recentlyPlayedUi)
                    
                    // If we are currently in Success state, update it.
                    // If we are in Loading or Error, the next Success emission will use the updated uiData.
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
