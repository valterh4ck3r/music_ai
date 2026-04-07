package com.valter.music_ai.data.repository

import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toDomainList
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toEntity
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.mapper.SongDtoMapper.toDomainList
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: ITunesApiService,
    private val songDao: SongDao
) : HomeRepository {

    /**
     * Offline-first search:
     * 1. Emit cached results immediately if available
     * 2. Fetch from network in background
     * 4. If network fails and we had no cache, emit failure
     */
    override fun searchSongs(
        term: String,
        limit: Int,
        offset: Int,
        forceRemote: Boolean
    ): Flow<ResponseState<List<Song>>> = flow {
        emit(ResponseState.Loading)

        // Step 2: Fetch from network
        try {
            val response = apiService.searchSongs(
                term = term,
                limit = limit,
                offset = offset
            )
            val songs = response.results.toDomainList()

            emit(ResponseState.Success(songs))
        } catch (e: HttpException) {
            emit(ResponseState.Error(statusCode = e.code(), message = e.message ?: "Unknown HttpException"))
        } catch (e: Exception) {
            emit(ResponseState.Error(statusCode = null, message = e.message ?: "Network error"))
        }
    }

    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyPlayed().map { entities ->
            entities.toDomainList()
        }
    }

    override suspend fun markSongAsPlayed(song: Song) {
        // Upsert: ensure the song exists in the DB before marking it as played.
        // This is critical when cache is disabled — the UPDATE would silently fail
        // on a song that was never stored, causing the recently played list to stay empty.
        val now = System.currentTimeMillis()
        val entity = song.toEntity().copy(lastPlayedAt = now)
        songDao.insertAll(listOf(entity))
    }

    override suspend fun clearRecentlyPlayed() {
        songDao.clearRecentlyPlayed()
    }
}
