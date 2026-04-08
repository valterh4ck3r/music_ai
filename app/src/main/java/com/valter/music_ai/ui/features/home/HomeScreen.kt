package com.valter.music_ai.ui.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valter.music_ai.domain.model.ResponseState
import com.valter.music_ai.domain.model.Song
import com.valter.music_ai.ui.features.home.components.PullToRefreshLayout
import com.valter.music_ai.ui.features.home.components.SearchBar
import com.valter.music_ai.ui.features.home.components.SongListItem
import com.valter.music_ai.ui.features.home.components.SongListItemShimmer
import com.valter.music_ai.ui.features.song.components.SongOptionsBottomSheet
import com.valter.music_ai.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onNavigateToSong: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit
) {
    val stateResponse by viewModel.uiState.collectAsStateWithLifecycle()
    val uiState by remember(stateResponse) {
        derivedStateOf {
            (stateResponse as? ResponseState.Success)?.data ?: viewModel.getUiData()
        }
    }

    val errorMessage = (stateResponse as? ResponseState.Error)?.message
    
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle(initialValue = true)
    val listState = rememberLazyListState()
    
    var isSearchVisible by remember { mutableStateOf(false) }
    
    // Bottom Sheet state
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    // Load Cache Itens
    LaunchedEffect(Unit) {
        viewModel.loadRecentlyPlayed()
    }

    // Hide search bar on pull down to refresh
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            isSearchVisible = false
        }
    }

    // Pagination: trigger load when scrolled near end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3 && !uiState.isLoadingMore && uiState.canLoadMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.songs.isNotEmpty()) {
            viewModel.loadNextPage()
        }
    }

    PullToRefreshLayout(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshSongs() },
        isEnabled = isConnected,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Title and Search Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Songs",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Open search bar",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Search bar
            item {
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Recently played section
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently Played",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.recentlyPlayed,
                            key = { "recent_${it.id}" }
                        ) { song ->
                            // Custom item for horizontal scroll
                            Column(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable {
                                        viewModel.onSongClick(song)
                                        val json = com.google.gson.Gson().toJson(song.originalSong)
                                        val base64 = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                                        onNavigateToSong(base64)
                                    },
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1A1A1A))
                                ) {
                                    val artworkUrl = song.albumArtUrl?.replace("100x100bb", "300x300bb")
                                    coil.compose.AsyncImage(
                                        model = artworkUrl,
                                        contentDescription = "Album art for ${song.title}",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = com.valter.music_ai.ui.theme.OnDarkTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Text(
                        text = "All Songs",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            when(stateResponse){
                is ResponseState.Success -> {
                    // Song list with pagination
                    items(
                        items = uiState.songs,
                        key = { "song_${it.id}" }
                    ) { song ->
                        SongListItem(
                            title = song.title,
                            artist = song.artist,
                            albumArtUrl = song.albumArtUrl,
                            onClick = {
                                viewModel.onSongClick(song)
                                val json = com.google.gson.Gson().toJson(song.originalSong)
                                val base64 = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                                onNavigateToSong(base64)
                            },
                            onMoreClick = {
                                selectedSongForOptions = song.originalSong
                                showSheet = true
                            }
                        )
                    }
                }
                is ResponseState.Loading -> {
                    // Song List Shimmer Layout
                    items(10) {
                        SongListItemShimmer()
                    }
                }
                is ResponseState.Error -> {
                    // Shows Error
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(46.dp))
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Warning",
                                tint = Color.White,
                                modifier = Modifier.width(45.dp).height(45.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "No Internet",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Loading more indicator
            if (uiState.isLoadingMore) {
                items(3) {
                    SongListItemShimmer()
                }
            }
        }
    }

    if (showSheet) {
        SongOptionsBottomSheet(
            song = selectedSongForOptions,
            sheetState = sheetState,
            onDismissRequest = { },
            onViewAlbumClick = {
                selectedSongForOptions?.let { s ->
                    val json = com.google.gson.Gson().toJson(s)
                    val base64 = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    onNavigateToAlbum(base64)
                }
            }
        )
    }
}
