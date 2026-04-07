package com.valter.music_ai.di

import android.content.Context
import com.valter.music_ai.data.connectivity.AndroidNetworkConnectivityObserver
import com.valter.music_ai.data.connectivity.NetworkConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context
    ): NetworkConnectivityObserver {
        return AndroidNetworkConnectivityObserver(context)
    }
}
