package com.valter.music_ai.domain.repository

import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.model.ResponseState
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    /**
     * Search songs by term with pagination.
     * Returns a Flow to support offline-first: emits cached data first, then network data.
     */
    fun searchSongs(term: String, limit: Int, offset: Int, forceRemote: Boolean = false): Flow<ResponseState<List<Song>>>

    /**
     * Get recently played songs from local cache.
     */
    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    /**
     * Mark a song as recently played (updates lastPlayedAt timestamp).
     */
    suspend fun markSongAsPlayed(song: Song)

    /**
     * Clear all recently played songs from local cache.
     */
    suspend fun clearRecentlyPlayed()
}
