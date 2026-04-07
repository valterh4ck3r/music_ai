package com.valter.music_ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valter.music_ai.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE trackId = :trackId")
    suspend fun getSongById(trackId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE trackName LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit OFFSET :offset")
    fun searchSongs(query: String, limit: Int, offset: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE trackId = :trackId")
    suspend fun markAsPlayed(trackId: Long, timestamp: Long)

    @Query("DELETE FROM songs WHERE cachedAt < :threshold AND lastPlayedAt IS NULL")
    suspend fun clearExpiredCache(threshold: Long)

    @Query("UPDATE songs SET lastPlayedAt = NULL")
    suspend fun clearRecentlyPlayed()
}
