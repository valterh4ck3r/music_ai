package com.valter.music_ai.data.repository

import android.content.Context
import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toDomainList
import com.valter.music_ai.data.local.mapper.SongEntityMapper.toEntity
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.remote.mapper.SongDtoMapper.toDomainList
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.domain.repository.HomeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: ITunesApiService,
    private val songDao: SongDao,
    @ApplicationContext private val context: Context
) : HomeRepository {

    /**
     * Offline-first search:
     * 1. Emit cached results immediately if available
     * 2. Fetch from network in background
     * 3. If network fails and we had no cache, emit failure
     */
    override fun searchSongs(
        term: String,
        limit: Int,
        offset: Int,
        forceRemote: Boolean
    ): Flow<ResponseState<List<Song>>> = flow {
        emit(ResponseState.Loading)

        // Fetch from network
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
        val now = System.currentTimeMillis()

        // 1. Fetch current stored data for this song from DB to keep local path
        val existingEntity = songDao.getSongById(song.trackId)

        // 2. Prepare the new entity with current timestamp and existing local path
        var updatedEntity = song.toEntity().copy(
            lastPlayedAt = now,
            previewUrlLocal = existingEntity?.previewUrlLocal ?: song.previewUrlLocal
        )

        // 3. Only download if we don't have it locally or the file was deleted
        val needsDownload = song.previewUrl != null && (
            updatedEntity.previewUrlLocal == null ||
            !File(updatedEntity.previewUrlLocal).exists()
        )

        if (needsDownload) {
            val localPath = downloadAndSavePreview(song.trackId, song.previewUrl)
            if (localPath != null) {
                updatedEntity = updatedEntity.copy(previewUrlLocal = localPath)
            }
        }

        songDao.insertAll(listOf(updatedEntity))
    }

    private suspend fun downloadAndSavePreview(trackId: Long, previewUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.downloadFile(previewUrl)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@withContext null
                    val file = File(context.filesDir, "previews/$trackId.m4a")
                    file.parentFile?.mkdirs()

                    body.byteStream().use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    file.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun clearRecentlyPlayed() {
        songDao.clearRecentlyPlayed()
    }
}
