package com.valter.music_ai.ui.features.home.model

import com.valter.music_ai.domain.model.ResponseState

typealias HomeUiState = ResponseState<HomeUiData>

data class HomeUiData(
    val songs: List<SongUi> = emptyList(),
    val recentlyPlayed: List<SongUi> = emptyList(),
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val canLoadMore: Boolean = true
)
