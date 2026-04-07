package com.valter.music_ai.data.repository

import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.entity.SongEntity
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.dto.ITunesSearchResponse
import com.valter.music_ai.data.remote.dto.TrackDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HomeRepositoryImplTest {

    private lateinit var apiService: ITunesApiService
    private lateinit var songDao: SongDao
    private lateinit var repository: HomeRepositoryImpl

    @Before
    fun setup() {
        apiService = mockk()
        songDao = mockk(relaxed = true)
        repository = HomeRepositoryImpl(apiService, songDao)
    }

    // -- searchSongs --

    @Test
    fun `searchSongs emits network results on success`() = runTest {
        // Given
        val trackDto = TrackDto(
            trackId = 1L,
            trackName = "Purple Rain",
            artistName = "Prince",
            collectionName = "Purple Rain",
            artworkUrl100 = "https://example.com/art.jpg",
            previewUrl = "https://example.com/preview.mp3",
            trackTimeMillis = 300000
        )
        coEvery {
            apiService.searchSongs(any(), any(), any(), any(), any(), any())
        } returns ITunesSearchResponse(resultCount = 1, results = listOf(trackDto))

        coEvery { songDao.searchSongs(any(), any(), any()) } returns flowOf(emptyList())

        // When
        val results = repository.searchSongs("Purple Rain", 20, 0).toList()

        // Then
        assertTrue(results.any { it.isSuccess })
        val songs = results.last().getOrNull()!!
        assertEquals(1, songs.size)
        assertEquals("Purple Rain", songs[0].trackName)
        assertEquals("Prince", songs[0].artistName)
    }

    @Test
    fun `searchSongs emits cached results first when cache is available`() = runTest {
        // Given
        val cachedEntity = SongEntity(
            trackId = 1L,
            trackName = "Cached Song",
            artistName = "Cached Artist",
            collectionName = null,
            artworkUrl100 = null,
            previewUrl = null,
            trackTimeMillis = null
        )
        coEvery { songDao.searchSongs(any(), any(), any()) } returns flowOf(listOf(cachedEntity))

        val networkDto = TrackDto(
            trackId = 2L,
            trackName = "Network Song",
            artistName = "Network Artist",
            collectionName = null,
            artworkUrl100 = null,
            previewUrl = null,
            trackTimeMillis = null
        )
        coEvery {
            apiService.searchSongs(any(), any(), any(), any(), any(), any())
        } returns ITunesSearchResponse(resultCount = 1, results = listOf(networkDto))

        // When
        val results = repository.searchSongs("test", 20, 0).toList()

        // Then
        assertTrue(results.size >= 2)
        // First emission should be cached
        assertEquals("Cached Song", results[0].getOrNull()!![0].trackName)
        // Second emission should be network
        assertEquals("Network Song", results[1].getOrNull()!![0].trackName)
    }

    @Test
    fun `searchSongs emits failure when network fails and cache is empty`() = runTest {
        // Given
        coEvery { songDao.searchSongs(any(), any(), any()) } returns flowOf(emptyList())
        coEvery {
            apiService.searchSongs(any(), any(), any(), any(), any(), any())
        } throws IOException("No connection")

        // When
        val results = repository.searchSongs("test", 20, 0).toList()

        // Then
        assertTrue(results.any { it.isFailure })
    }

    @Test
    fun `searchSongs uses cache when network fails but cache exists`() = runTest {
        // Given
        val cachedEntity = SongEntity(
            trackId = 1L,
            trackName = "Cached Song",
            artistName = "Cached Artist",
            collectionName = null,
            artworkUrl100 = null,
            previewUrl = null,
            trackTimeMillis = null
        )
        coEvery { songDao.searchSongs(any(), any(), any()) } returns flowOf(listOf(cachedEntity))
        coEvery {
            apiService.searchSongs(any(), any(), any(), any(), any(), any())
        } throws IOException("No connection")

        // When
        val results = repository.searchSongs("test", 20, 0).toList()

        // Then
        // Should have at least the cache result, and no failure
        assertTrue(results.any { it.isSuccess })
        val successResult = results.first { it.isSuccess }.getOrNull()!!
        assertEquals("Cached Song", successResult[0].trackName)
    }

    @Test
    fun `searchSongs caches network results`() = runTest {
        // Given
        val networkDto = TrackDto(
            trackId = 1L,
            trackName = "Network Song",
            artistName = "Artist",
            collectionName = null,
            artworkUrl100 = null,
            previewUrl = null,
            trackTimeMillis = null
        )
        coEvery { songDao.searchSongs(any(), any(), any()) } returns flowOf(emptyList())
        coEvery {
            apiService.searchSongs(any(), any(), any(), any(), any(), any())
        } returns ITunesSearchResponse(resultCount = 1, results = listOf(networkDto))

        // When
        repository.searchSongs("test", 20, 0).toList()

        // Then
        coVerify { songDao.insertAll(any()) }
    }

    // -- getRecentlyPlayedSongs --

    @Test
    fun `getRecentlyPlayedSongs returns mapped songs from dao`() = runTest {
        // Given
        val entities = listOf(
            SongEntity(
                trackId = 1L,
                trackName = "Recent Song",
                artistName = "Artist",
                collectionName = null,
                artworkUrl100 = null,
                previewUrl = null,
                trackTimeMillis = null,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
        coEvery { songDao.getRecentlyPlayed() } returns flowOf(entities)

        // When
        val results = repository.getRecentlyPlayedSongs().toList()

        // Then
        assertEquals(1, results[0].size)
        assertEquals("Recent Song", results[0][0].trackName)
    }

    // -- markSongAsPlayed --

    @Test
    fun `markSongAsPlayed calls dao with correct trackId`() = runTest {
        // When
        repository.markSongAsPlayed(123L)

        // Then
        coVerify { songDao.markAsPlayed(123L, any()) }
    }
}
