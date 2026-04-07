package com.valter.music_ai.ui.features.home.model

import com.valter.music_ai.domain.model.Song

data class SongUi(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String? = null,
    val originalSong: Song
)
