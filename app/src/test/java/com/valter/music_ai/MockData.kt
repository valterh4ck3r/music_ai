package com.valter.music_ai

import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.ui.features.home.model.SongUi

/**
 * Utility object providing mock data for testing purposes.
 */
object MockData {
    
    /**
     * Creates a mock Song domain object.
     */
    fun createSong(id: Long = 1L) = Song(
        trackId = id,
        trackName = "Song $id",
        artistName = "Artist $id",
        collectionName = "Album $id",
        artworkUrl100 = "https://example.com/image$id.jpg",
        previewUrl = "https://example.com/preview$id.m4a",
        trackTimeMillis = 300000,
        collectionId = 100L
    )

    /**
     * Creates a mock SongUi object.
     */
    fun createSongUi(id: String = "1") = SongUi(
        id = id,
        title = "Song $id",
        artist = "Artist $id",
        albumArtUrl = "https://example.com/image$id.jpg",
        originalSong = createSong(id.toLong())
    )

    /**
     * Returns a list of mock songs.
     */
    val testSongs = listOf(
        createSong(1L),
        createSong(2L)
    )
}
