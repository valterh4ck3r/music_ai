package com.valter.music_ai.data.remote.mapper

import com.valter.music_ai.data.remote.dto.TrackDto
import com.valter.music_ai.domain.model.Song

object SongDtoMapper {

    fun TrackDto.toDomain(): Song? {
        val id = trackId ?: return null
        return Song(
            trackId = id,
            collectionId = collectionId,
            trackName = trackName ?: "Unknown",
            artistName = artistName ?: "Unknown",
            collectionName = collectionName,
            artworkUrl100 = artworkUrl100,
            previewUrl = previewUrl,
            trackTimeMillis = trackTimeMillis
        )
    }

    fun List<TrackDto>.toDomainList(): List<Song> {
        return mapNotNull { it.toDomain() }
    }
}
