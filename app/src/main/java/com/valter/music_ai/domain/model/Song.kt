package com.valter.music_ai.domain.model

data class Song(
    val trackId: Long,
    val collectionId: Long? = null,
    val trackName: String,
    val artistName: String,
    val collectionName: String?,
    val artworkUrl100: String?,
    val previewUrl: String?,
    val trackTimeMillis: Long?,
    val lastPlayedAt: Long? = null,
    val previewUrlLocal: String? = null
)
