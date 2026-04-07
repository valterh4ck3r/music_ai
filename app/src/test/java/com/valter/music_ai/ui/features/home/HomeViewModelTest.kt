package com.valter.music_ai.ui.features.home

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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        coEvery {
            repository.searchSongs(any(), any(), any())
        } returns flowOf(Result.success(testSongs))

        coEvery {
            repository.getRecentlyPlayedSongs()
        } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(repository)
    }

    @Test
    fun `initial state loads songs with default search term`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.songs.size)
        assertEquals("Purple Rain", state.songs[0].title)
    }

    @Test
    fun `onSearchQueryChanged updates query and triggers search with debounce`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Prince")
        assertEquals("Prince", viewModel.uiState.value.searchQuery)

        // Before debounce — still shows old data
        advanceTimeBy(200)

        // After debounce
        advanceTimeBy(400)
        advanceUntilIdle()

        coVerify { repository.searchSongs("Prince", any(), 0) }
    }

    @Test
    fun `loadNextPage increments offset and appends songs`() = runTest {
        // Initial load needs PAGE_SIZE (20) songs so canLoadMore = true
        val initialSongs = (1L..20L).map { i ->
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
            repository.searchSongs(any(), any(), any())
        } returns flowOf(Result.success(initialSongs))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.songs.size)
        assertTrue(viewModel.uiState.value.canLoadMore)

        // Set up for next page
        val nextPageSongs = listOf(
            Song(
                trackId = 21L,
                trackName = "Billie Jean",
                artistName = "Michael Jackson",
                collectionName = "Thriller",
                artworkUrl100 = null,
                previewUrl = null,
                trackTimeMillis = 280000
            )
        )
        coEvery {
            repository.searchSongs(any(), any(), any())
        } returns flowOf(Result.success(nextPageSongs))

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Should have original 20 + 1 new
        assertEquals(21, state.songs.size)
        assertFalse(state.isLoadingMore)
        // 1 < PAGE_SIZE, so canLoadMore should be false now
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `onSongClick marks song as played`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val song = viewModel.uiState.value.songs.first()
        viewModel.onSongClick(song)
        advanceUntilIdle()

        coVerify { repository.markSongAsPlayed(1L) }
    }

    @Test
    fun `error state is set when search fails`() = runTest {
        coEvery {
            repository.searchSongs(any(), any(), any())
        } returns flowOf(Result.failure(Exception("Network error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Network error", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery {
            repository.searchSongs(any(), any(), any())
        } returns flowOf(Result.failure(Exception("Error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error != null)

        viewModel.clearError()
        assertTrue(viewModel.uiState.value.error == null)
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

        assertEquals(1, viewModel.uiState.value.recentlyPlayed.size)
        assertEquals("Recent Song", viewModel.uiState.value.recentlyPlayed[0].title)
    }
}
