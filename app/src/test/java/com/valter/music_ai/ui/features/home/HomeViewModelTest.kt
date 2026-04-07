package com.valter.music_ai.ui.features.home

import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: HomeRepository
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testSongs = listOf(
        Song(
            trackId = 1L,
            trackName = "Purple Rain",
            artistName = "Prince",
            collectionName = "Purple Rain",
            artworkUrl100 = "https://example.com/art.jpg",
            previewUrl = null,
            trackTimeMillis = 300000
        ),
        Song(
            trackId = 2L,
            trackName = "Get Lucky",
            artistName = "Daft Punk",
            collectionName = "Random Access Memories",
            artworkUrl100 = null,
            previewUrl = null,
            trackTimeMillis = 250000
        )
    )

    private lateinit var observer: NetworkConnectivityObserver

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        observer = mockk(relaxed = true)

        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Success(testSongs))

        coEvery {
            observer.isConnected
        } returns flowOf(true)

        coEvery {
            repository.getRecentlyPlayedSongs()
        } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(repository, observer)
    }

    @Test
    fun `initial state loads songs with default search term`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state is ResponseState.Loading)
        val data = (state as ResponseState.Success).data
        assertEquals(2, data.songs.size)
        assertEquals("Purple Rain", data.songs[0].title)
    }

    @Test
    fun `onSearchQueryChanged updates query and triggers search with debounce`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Prince")
        val data = (viewModel.uiState.value as ResponseState.Success).data
        assertEquals("Prince", data.searchQuery)

        // Before debounce — still shows old data
        advanceTimeBy(200)

        // After debounce
        advanceTimeBy(400)
        advanceUntilIdle()

        coVerify { repository.searchSongs("Prince", 200, 0, any()) }
    }

    @Test
    fun `loadNextPage increments offset and appends songs`() = runTest {
        // Initial load needs > 20 songs so canLoadMore = true internally
        val initialSongs = (1L..21L).map { i ->
            Song(
                trackId = i,
                trackName = "Song $i",
                artistName = "Artist $i",
                collectionName = null,
                artworkUrl100 = null,
                previewUrl = null,
                trackTimeMillis = null
            )
        }
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Success(initialSongs))

        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        assertEquals(20, data.songs.size)
        assertTrue(data.canLoadMore)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updatedData = (state as ResponseState.Success).data
        // Should have original 20 + 1 new
        assertEquals(21, updatedData.songs.size)
        assertFalse(updatedData.isLoadingMore)
        assertFalse(updatedData.canLoadMore)
    }

    @Test
    fun `onSongClick marks song as played`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        val song = data.songs.first()
        viewModel.onSongClick(song)
        advanceUntilIdle()

        coVerify { repository.markSongAsPlayed(1L) }
    }

    @Test
    fun `error state is set when search fails`() = runTest {
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Error(message = "Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ResponseState.Error)
        assertEquals("Network error", (state as ResponseState.Error).message)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Error(message = "Error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ResponseState.Error)

        viewModel.clearError()
        assertTrue(viewModel.uiState.value is ResponseState.Success)
    }

    @Test
    fun `recently played songs are loaded on init`() = runTest {
        val recentSongs = listOf(
            Song(
                trackId = 99L,
                trackName = "Recent Song",
                artistName = "Recent Artist",
                collectionName = null,
                artworkUrl100 = null,
                previewUrl = null,
                trackTimeMillis = null,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
        coEvery { repository.getRecentlyPlayedSongs() } returns flowOf(recentSongs)

        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        assertEquals(1, data.recentlyPlayed.size)
        assertEquals("Recent Song", data.recentlyPlayed[0].title)
    }
}
