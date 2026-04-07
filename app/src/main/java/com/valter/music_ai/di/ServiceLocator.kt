package com.valter.music_ai.di

import android.content.Context
import com.valter.music_ai.data.connectivity.AndroidNetworkConnectivityObserver
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import com.valter.music_ai.data.local.db.MusicDatabase
import com.valter.music_ai.data.remote.api.ITunesApiService
import com.valter.music_ai.data.repository.HomeRepositoryImpl
import com.valter.music_ai.domain.repository.HomeRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Manual Service Locator — centralizes all dependency creation.
 * Can be replaced by Hilt/Koin/Dagger later without affecting consumers.
 */
object ServiceLocator {

    private lateinit var applicationContext: Context

    // Lazy singletons
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ITunesApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: ITunesApiService by lazy {
        retrofit.create(ITunesApiService::class.java)
    }

    private val database: MusicDatabase by lazy {
        MusicDatabase.getInstance(applicationContext)
    }

    val homeRepository: HomeRepository by lazy {
        HomeRepositoryImpl(apiService, database.songDao())
    }

    val networkConnectivityObserver: NetworkConnectivityObserver by lazy {
        AndroidNetworkConnectivityObserver(applicationContext)
    }

    /**
     * Must be called once from MainActivity.onCreate() before accessing any dependency.
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
