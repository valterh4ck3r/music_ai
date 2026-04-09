package com.valter.music_ai.data.repository

import android.content.Context
import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.entity.SongEntity
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.dto.ITunesSearchResponse
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for HomeRepositoryImpl
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryImplTest {

    private lateinit var repository: HomeRepositoryImpl
    private val apiService: ITunesApiService = mockk()
    private val songDao: SongDao = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = HomeRepositoryImpl(apiService, songDao, context)
    }

    @Test
    fun `searchSongs should emit loading then success when api call is successful`() = runTest {
        // Prepare mock data
        val mockResults = ITunesSearchResponse(
            resultCount = 0,
            results = emptyList()
        )
        
        // Mock API service behavior
        coEvery { 
            apiService.searchSongs(any(), any(), any()) 
        } returns mockResults

        // Execute the repository method
        val results = repository.searchSongs("test", 20, 0).toList()

        // Verify the sequence of emissions: Loading followed by Success
        assertTrue("First emission should be Loading", results[0] is ResponseState.Loading)
        assertTrue("Second emission should be Success", results[1] is ResponseState.Success)
    }

    @Test
    fun `searchSongs should emit error when api call fails`() = runTest {
        // Mock API service to throw an exception
        coEvery { 
            apiService.searchSongs(any(), any(), any()) 
        } throws Exception("Network Error")

        // Execute the repository method
        val results = repository.searchSongs("test", 20, 0).toList()

        // Verify that an Error state is emitted with the correct message
        assertTrue("Should emit Error state", results[1] is ResponseState.Error)
        assertEquals("Network Error", (results[1] as ResponseState.Error).message)
    }

    @Test
    fun `getRecentlyPlayedSongs should return list of songs from DAO`() = runTest {
        // Mock DAO to return a list of song entities
        val entities = listOf(
            SongEntity(
                trackId = 1L,
                trackName = "Song 1",
                artistName = "Artist 1",
                collectionName = "Album 1",
                artworkUrl100 = "url",
                previewUrl = "url",
                trackTimeMillis = 1000,
                lastPlayedAt = 1000L,
                collectionId = 1L
            )
        )
        coEvery { songDao.getRecentlyPlayed() } returns flowOf(entities)

        // Execute the repository method
        val flow = repository.getRecentlyPlayedSongs()
        val results = flow.toList()

        // Verify that the songs are correctly mapped from entities to domain models
        assertEquals(1, results[0].size)
        assertEquals("Song 1", results[0][0].trackName)
    }

    @Test
    fun `markSongAsPlayed should insert song into DAO`() = runTest {
        // Prepare a domain song model
        val song = Song(
            trackId = 1L,
            trackName = "Song 1",
            artistName = "Artist 1",
            collectionName = "Album 1",
            artworkUrl100 = "url",
            previewUrl = null,
            trackTimeMillis = 1000
        )
        
        // Mock DAO getSongById to return null (song not yet in DB)
        coEvery { songDao.getSongById(any()) } returns null

        // Execute the repository method
        repository.markSongAsPlayed(song)

        // Verify that the DAO's insert method was called
        coVerify { songDao.insertAll(any()) }
    }

    @Test
    fun `clearRecentlyPlayed should call DAO clear method`() = runTest {
        // Execute the repository method
        repository.clearRecentlyPlayed()

        // Verify that the DAO's clear method was called
        coVerify { songDao.clearRecentlyPlayed() }
    }
}
