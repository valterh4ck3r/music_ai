package com.valter.music_ai.ui.features.home

import com.valter.music_ai.MockData
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
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

/**
 * Unit tests for HomeViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: HomeRepository
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testSongs = MockData.testSongs

    private lateinit var observer: NetworkConnectivityObserver

    @Before
    fun setup() {
        // Set up the main dispatcher for testing coroutines
        Dispatchers.setMain(testDispatcher)
        
        // Mock dependencies
        repository = mockk(relaxed = true)
        observer = mockk(relaxed = true)

        // Default mock behaviors
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
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Helper to create the ViewModel with mocked dependencies
     */
    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(repository, observer)
    }

    /**
     * Test that the initial state loads songs successfully using a random default term
     */
    @Test
    fun `initial state loads songs with default search term`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("State should not be loading", state is ResponseState.Loading)
        
        val data = (state as ResponseState.Success).data
        assertEquals("Should load 2 songs", 2, data.songs.size)
        assertEquals("First song title should match", "Song 1", data.songs[0].title)
    }

    /**
     * Test that when the search query changes, it updates the state and triggers a debounced search
     */
    @Test
    fun `onSearchQueryChanged updates query and triggers search with debounce`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Update search query
        viewModel.onSearchQueryChanged("Prince")
        val data = (viewModel.uiState.value as ResponseState.Success).data
        assertEquals("Search query should be updated in UI state", "Prince", data.searchQuery)

        // Advance time but stay within debounce window (800ms)
        advanceTimeBy(400)
        coVerify(exactly = 1) { repository.searchSongs(any(), any(), any(), any()) } // Initial search only

        // Advance time past debounce window
        advanceTimeBy(500)
        advanceUntilIdle()

        // Verify that the repository was called with the new query
        coVerify { repository.searchSongs("Prince", 200, 0, any()) }
    }

    /**
     * Test pagination: verify that loadNextPage fetches and appends more songs
     */
    @Test
    fun `loadNextPage increments offset and appends songs`() = runTest {
        // Mock a large list of songs to test pagination
        val initialSongs = (1L..25L).map { i -> MockData.createSong(i) }
        
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Success(initialSongs))

        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        // Page size is 20, so first load should show 20
        assertEquals("Initial load should show 20 songs", 20, data.songs.size)
        assertTrue("Should be able to load more", data.canLoadMore)

        // Trigger next page load
        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updatedData = (state as ResponseState.Success).data
        // Should have 20 (original) + 5 (remaining) = 25
        assertEquals("Total songs should be 25 after pagination", 25, updatedData.songs.size)
        assertFalse("Should not be loading more", updatedData.isLoadingMore)
        assertFalse("Should not be able to load more songs", updatedData.canLoadMore)
    }

    /**
     * Test that clicking a song marks it as played in the repository
     */
    @Test
    fun `onSongClick marks song as played`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        val songUi = data.songs.first()
        
        // Click the song
        viewModel.onSongClick(songUi)
        advanceUntilIdle()

        // Verify repository call with the original domain song
        coVerify { repository.markSongAsPlayed(songUi.originalSong) }
    }

    /**
     * Test that search failures update the UI state to Error
     */
    @Test
    fun `error state is set when search fails`() = runTest {
        // Mock a failure response
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Error(message = "Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is ResponseState.Error)
        assertEquals("Error message should match", "Network error", (state as ResponseState.Error).message)
    }

    /**
     * Test that clearError resets the UI state back to Success with previous data
     */
    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery {
            repository.searchSongs(any(), any(), any(), any())
        } returns flowOf(ResponseState.Error(message = "Error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue("Should initially be in Error state", viewModel.uiState.value is ResponseState.Error)

        // Clear the error
        viewModel.clearError()
        assertTrue("State should be Success after clearing error", viewModel.uiState.value is ResponseState.Success)
    }

    /**
     * Test that recently played songs are correctly loaded and displayed on initialization
     */
    @Test
    fun `recently played songs are loaded on init`() = runTest {
        val recentSongs = listOf(MockData.createSong(99L).copy(lastPlayedAt = 123456789L))
        coEvery { repository.getRecentlyPlayedSongs() } returns flowOf(recentSongs)

        viewModel = createViewModel()
        advanceUntilIdle()

        val data = (viewModel.uiState.value as ResponseState.Success).data
        assertEquals("Recently played list should have 1 item", 1, data.recentlyPlayed.size)
        assertEquals("Recently played song title should match", "Song 99", data.recentlyPlayed[0].title)
    }
}

