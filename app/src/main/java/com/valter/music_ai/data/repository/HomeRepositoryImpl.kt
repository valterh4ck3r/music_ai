package com.valter.music_ai.data.repository

import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toDomainList
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toEntityList
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.mapper.SongDtoMapper.toDomainList
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class HomeRepositoryImpl(
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
    ): Flow<Result<List<Song>>> = flow {
        // Step 1: Try cache first if not forcing remote
        val cached = if (!forceRemote) songDao.searchSongs(term, limit, offset).firstOrNull() else null
        if (!cached.isNullOrEmpty()) {
            emit(Result.success(cached.toDomainList()))
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

            emit(Result.success(songs))
        } catch (e: Exception) {
            // Step 4: If cache was empty too, emit failure
            if (cached.isNullOrEmpty()) {
                emit(Result.failure(e))
            }
            // If cache was available, we already emitted it — just silently fail network
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
