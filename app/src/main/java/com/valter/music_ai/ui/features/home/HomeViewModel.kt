package com.valter.music_ai.ui.features.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    private val _songs = MutableStateFlow<List<SongUi>>(emptyList())
    val songs: StateFlow<List<SongUi>> = _songs.asStateFlow()

    init {
        _songs.value = listOf(
            SongUi(id = "1", title = "Purple Rain", artist = "Prince"),
            SongUi(id = "2", title = "Power Of Equality", artist = "Red Hot Chili Peppers"),
            SongUi(id = "3", title = "Something", artist = "The Beatles"),
            SongUi(id = "4", title = "Like A Virgin", artist = "Madonna"),
            SongUi(id = "5", title = "Get Lucky", artist = "Daft Punk feat. Pharrell Williams")
        )
    }

    fun onSongClick(song: SongUi) {
        // TODO
    }

    fun onSongMoreClick(song: SongUi) {
        // TODO
    }
}
