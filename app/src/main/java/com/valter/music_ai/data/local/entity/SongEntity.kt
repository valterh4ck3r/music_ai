package com.valter.music_ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val trackId: Long,
    val trackName: String,
    val artistName: String,
    val collectionName: String?,
    val artworkUrl100: String?,
    val previewUrl: String?,
    val trackTimeMillis: Long?,
    val lastPlayedAt: Long? = null,
    val previewUrlLocal: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
