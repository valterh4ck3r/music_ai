package com.valter.music_ai.ui.features.home.model

data class SongUi(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String? = null
)
