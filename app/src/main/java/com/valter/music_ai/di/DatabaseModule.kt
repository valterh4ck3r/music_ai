package com.valter.music_ai.di

import android.content.Context
import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.db.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return MusicDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSongDao(database: MusicDatabase): SongDao {
        return database.songDao()
    }
}
