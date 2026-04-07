package com.valter.music_ai.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.valter.music_ai.data.local.dao.SongDao
import com.valter.music_ai.data.local.entity.SongEntity

@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_ai_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
