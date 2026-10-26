package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String,
    val characterId: String,
    val characterName: String,
    val characterSubtitle: String,
    val characterAvatarEmoji: String,
    val characterBgColorHex: String,
    val lastMessage: String,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)
