package com.valter.music_ai.ui.features.song

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.Player
import com.valter.music_ai.MockData
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.repository.HomeRepository
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SongViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModelTest {

    private lateinit var repository: HomeRepository
    private lateinit var connectivityObserver: NetworkConnectivityObserver
    private lateinit var viewModel: SongViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Mock Player
    private val player: Player = mockk(relaxed = true)
    
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
        
        // Mock static Base64 and Uri to avoid Android framework dependency crash
        mockkStatic(Base64::class)
        mockkStatic(Uri::class)
        
        every { Base64.decode(songBase64, any()) } returns songJson.toByteArray()
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        // Default repository mocks
        coEvery { repository.searchSongs(any(), any(), any(), any()) } returns flowOf(ResponseState.Success(listOf(song)))
        coEvery { connectivityObserver.isConnected } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Base64::class)
        unmockkStatic(Uri::class)
    }

    /**
     * Test that the ViewModel correctly initializes with a song from SavedStateHandle
     */
    @Test
    fun `initialization should load song from SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        advanceUntilIdle()

        assertEquals("Song ID should match", song.trackId, viewModel.uiState.value.song?.trackId)
    }

    /**
     * Test that setting a player updates the internal state and starts playback
     */
    @Test
    fun `setPlayer should sync player state and prepare song`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        
        // Set the player
        viewModel.setPlayer(player)
        advanceUntilIdle()
        
        // After setPlayer, the ViewModel triggers prepareSong because currentMediaId doesn't match
        // This resets totalMs and progressMs to 0 while loading
        val state = viewModel.uiState.value
        assertEquals("Status should be LOADING after setPlayer triggers prepareSong", SongUiStatus.LOADING, state.status)
        
        // Verify player was prepared for the song
        verify { player.prepare() }
        verify { player.play() }
    }

    /**
     * Test the play/pause toggle functionality
     */
    @Test
    fun `togglePlayPause should alternate between play and pause`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        viewModel.setPlayer(player)

        // Case: Currently playing -> should pause
        every { player.isPlaying } returns true
        viewModel.togglePlayPause()
        verify { player.pause() }

        // Case: Currently paused -> should play
        every { player.isPlaying } returns false
        viewModel.togglePlayPause()
        verify { player.play() }
    }

    /**
     * Test the repeat mode toggle
     */
    @Test
    fun `toggleRepeat should flip repeat enabled state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        
        assertFalse("Initially repeat should be disabled", viewModel.uiState.value.isRepeatEnabled)
        
        viewModel.toggleRepeat()
        assertTrue("Repeat should be enabled after toggle", viewModel.uiState.value.isRepeatEnabled)
        
        viewModel.toggleRepeat()
        assertFalse("Repeat should be disabled after second toggle", viewModel.uiState.value.isRepeatEnabled)
    }

    /**
     * Test seeking to a specific position
     */
    @Test
    fun `seekTo should update player position and UI state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        viewModel.setPlayer(player)
        
        val targetPos = 15000L
        viewModel.seekTo(targetPos)
        
        verify { player.seekTo(targetPos) }
        assertEquals("UI state progress should match target position", targetPos, viewModel.uiState.value.progressMs)
    }

    /**
     * Test next song functionality
     */
    @Test
    fun `nextSong should cycle through the loaded playlist`() = runTest {
        // Mock a playlist of 2 songs
        val song1 = MockData.createSong(1L)
        val song2 = MockData.createSong(2L)
        coEvery { repository.searchSongs(any(), any(), any(), any()) } returns flowOf(ResponseState.Success(listOf(song1, song2)))

        val savedStateHandle = SavedStateHandle(mapOf("songBase64" to songBase64))
        viewModel = SongViewModel(savedStateHandle, repository, connectivityObserver)
        viewModel.setPlayer(player)
        advanceUntilIdle()

        // Move to next song (song 2)
        viewModel.nextSong()
        advanceUntilIdle()
        
        assertEquals("Should have moved to song 2", 2L, viewModel.uiState.value.song?.trackId)
        
        // Move to next song again (wraps back to song 1)
        viewModel.nextSong()
        advanceUntilIdle()
        
        assertEquals("Should have wrapped back to song 1", 1L, viewModel.uiState.value.song?.trackId)
    }
}
