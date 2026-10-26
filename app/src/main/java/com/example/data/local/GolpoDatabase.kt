package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GeneratedStory
import com.example.data.model.GolpoCharacter
import com.example.data.model.InteractiveStoryProgress

@Database(
    entities = [
        GolpoCharacter::class,
        Conversation::class,
        ChatMessage::class,
        InteractiveStoryProgress::class,
        GeneratedStory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GolpoDatabase : RoomDatabase() {
    abstract fun golpoDao(): GolpoDao

    companion object {
        @Volatile
        private var INSTANCE: GolpoDatabase? = null

        fun getDatabase(context: Context): GolpoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GolpoDatabase::class.java,
                    "golpo_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
