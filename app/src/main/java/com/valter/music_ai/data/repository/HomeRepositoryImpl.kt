package com.valter.music_ai.data.repository

import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toDomainList
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toEntityList
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.mapper.SongDtoMapper.toDomainList
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import com.valter.music_ai.domain.model.ResponseState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: ITunesApiService,
    private val songDao: SongDao
) : HomeRepository {

    /**
     * Offline-first search:
     * 1. Emit cached results immediately if available
     * 2. Fetch from network in background
     * 3. Cache network results and emit them
     * 4. If network fails and we had no cache, emit failure
     */
    override fun searchSongs(
        term: String,
        limit: Int,
        offset: Int,
        forceRemote: Boolean
    ): Flow<ResponseState<List<Song>>> = flow {
        // Step 1: Try cache first if not forcing remote
        emit(ResponseState.Loading)
        val cached = if (!forceRemote) songDao.searchSongs(term, limit, offset).firstOrNull() else null
        if (!cached.isNullOrEmpty()) {
            emit(ResponseState.Success(cached.toDomainList()))
        }

        // Step 2: Fetch from network
        try {
            val response = apiService.searchSongs(
                term = term,
                limit = limit,
                offset = offset
            )
            val songs = response.results.toDomainList()

            // Step 3: Cache the results
            songDao.insertAll(songs.toEntityList())

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

    override suspend fun markSongAsPlayed(trackId: Long) {
        songDao.markAsPlayed(trackId, System.currentTimeMillis())
    }

    override suspend fun clearRecentlyPlayed() {
        songDao.clearRecentlyPlayed()
    }
}
