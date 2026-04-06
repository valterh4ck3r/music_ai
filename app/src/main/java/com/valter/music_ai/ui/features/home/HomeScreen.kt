package com.valter.music_ai.ui.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valter.music_ai.ui.features.home.components.SearchBar
import com.valter.music_ai.ui.features.home.components.SongListItem
import com.valter.music_ai.ui.theme.DarkBackground
import com.valter.music_ai.ui.theme.Music_aiTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val songs by viewModel.songs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(searchQuery, songs) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Songs",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Song list
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = filteredSongs,
                key = { it.id }
            ) { song ->
                SongListItem(
                    title = song.title,
                    artist = song.artist,
                    albumArtUrl = song.albumArtUrl,
                    onClick = { viewModel.onSongClick(song) },
                    onMoreClick = { viewModel.onSongMoreClick(song) }
                )
            }
        }
    }
}

// --- Preview ---

private val previewSongs = listOf(
    SongUi(id = "1", title = "Purple Rain", artist = "Prince"),
    SongUi(id = "2", title = "Power Of Equality", artist = "Red Hot Chili Peppers"),
    SongUi(id = "3", title = "Something", artist = "The Beatles"),
    SongUi(id = "4", title = "Like A Virgin", artist = "Madonna"),
    SongUi(id = "5", title = "Get Lucky", artist = "Daft Punk feat. Pharrell Williams")
)

data class SongUi(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String? = null
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    Music_aiTheme {
        HomeScreen()
    }
}

