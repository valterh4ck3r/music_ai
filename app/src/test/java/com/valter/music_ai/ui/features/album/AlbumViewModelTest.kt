package com.valter.music_ai.ui.features.album

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import com.valter.music_ai.MockData
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.repository.HomeRepository
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AlbumViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    private lateinit var repository: HomeRepository
    private lateinit var connectivityObserver: NetworkConnectivityObserver
    private lateinit var viewModel: AlbumViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Mock data
    private val song = MockData.createSong(1L)
    private val songJson = Gson().toJson(song)
    private val songBase64 = "mockBase64"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock dependencies
        repository = mockk(relaxed = true)
        connectivityObserver = mockk(relaxed = true)
        
        // Mock static Base64 to avoid Android framework dependency crash
        mockkStatic(Base64::class)
        every { Base64.decode(songBase64, any()) } returns songJson.toByteArray()

        // Default repository mocks
        coEvery { repository.getAlbumTracks(any()) } returns flowOf(ResponseState.Success(listOf(song)))
        coEvery { connectivityObserver.isConnected } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test that the ViewModel correctly decodes the song from SavedStateHandle and loads album tracks
     */
    @Test
    fun `initialization should load album tracks from repository using collectionId`() = runTest {
        // Prepare SavedStateHandle with base64 encoded song
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        
        viewModel = AlbumViewModel(savedStateHandle, repository, connectivityObserver)
        advanceUntilIdle()

        // Verify that the UI state is Success and contains the expected data
        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is ResponseState.Success)
        
        val data = (state as ResponseState.Success).data
        assertEquals("Album title should match", song.collectionName, data.albumTitle)
        assertEquals("Artist name should match", song.artistName, data.artistName)
        assertEquals("Should have 1 song in list", 1, data.songs.size)
        
        // Verify repository call
        coVerify { repository.getAlbumTracks(song.collectionId!!) }
    }

    /**
     * Test that if the collectionId is null, it searches for songs by name instead
     */
    @Test
    fun `initialization should search by name if collectionId is null`() = runTest {
        val songNoCollection = song.copy(collectionId = null)
        val jsonNoCollection = Gson().toJson(songNoCollection)
        every { Base64.decode("noColl", any()) } returns jsonNoCollection.toByteArray()
        
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to "noColl"))
        
        coEvery { repository.searchSongs(any(), any(), any()) } returns flowOf(ResponseState.Success(listOf(songNoCollection)))
        
        viewModel = AlbumViewModel(savedStateHandle, repository, connectivityObserver)
        advanceUntilIdle()

        // Verify that searchSongs was called instead of getAlbumTracks
        coVerify { repository.searchSongs(songNoCollection.collectionName ?: songNoCollection.trackName, 50, 0) }
    }

    /**
     * Test that errors during album loading are reflected in the UI state
     */
    @Test
    fun `loadAlbum failure should update state to Error`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        coEvery { repository.getAlbumTracks(any()) } returns flowOf(ResponseState.Error(message = "Fetch failed"))
        
        viewModel = AlbumViewModel(savedStateHandle, repository, connectivityObserver)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is ResponseState.Error)
        assertEquals("Error message should match", "Fetch failed", (state as ResponseState.Error).message)
    }
}
