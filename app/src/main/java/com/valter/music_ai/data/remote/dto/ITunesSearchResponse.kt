package com.valter.music_ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ITunesSearchResponse(
    @SerializedName("resultCount")
    val resultCount: Int,
    @SerializedName("results")
    val results: List<TrackDto>
)

data class TrackDto(
    @SerializedName("trackId")
    val trackId: Long?,
    @SerializedName("collectionId")
    val collectionId: Long?,
    @SerializedName("trackName")
    val trackName: String?,
    @SerializedName("artistName")
    val artistName: String?,

    @SerializedName("collectionName")
    val collectionName: String?,
    @SerializedName("artworkUrl100")
    val artworkUrl100: String?,
    @SerializedName("previewUrl")
    val previewUrl: String?,
    @SerializedName("trackTimeMillis")
    val trackTimeMillis: Long?
)
