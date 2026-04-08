package com.valter.music_ai.data.local.mapper

import com.valter.music_ai.data.local.entity.SongEntity
import com.valter.music_ai.domain.model.Song

object SongEntityMapper {

    fun SongEntity.toDomain(): Song {
        return Song(
            trackId = trackId,
            collectionId = collectionId,
            trackName = trackName,
            artistName = artistName,
            collectionName = collectionName,
            artworkUrl100 = artworkUrl100,
            previewUrl = previewUrl,
            trackTimeMillis = trackTimeMillis,
            lastPlayedAt = lastPlayedAt,
            previewUrlLocal = previewUrlLocal
        )
    }

    fun Song.toEntity(): SongEntity {
        return SongEntity(
            trackId = trackId,
            collectionId = collectionId,
            trackName = trackName,
            artistName = artistName,
            collectionName = collectionName,
            artworkUrl100 = artworkUrl100,
            previewUrl = previewUrl,
            trackTimeMillis = trackTimeMillis,
            lastPlayedAt = lastPlayedAt,
            previewUrlLocal = previewUrlLocal
        )
    }

    fun List<SongEntity>.toDomainList(): List<Song> {
        return map { it.toDomain() }
    }

    fun List<Song>.toEntityList(): List<SongEntity> {
        return map { it.toEntity() }
    }
}
