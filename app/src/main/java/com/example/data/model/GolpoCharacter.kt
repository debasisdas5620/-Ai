package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class GolpoCharacter(
    @PrimaryKey
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val personality: String,
    val backstory: String,
    val speakingStyle: String,
    val dialect: String,
    val interests: String,
    val genre: String,
    val openingMessage: String,
    val systemPrompt: String,
    val avatarEmoji: String,
    val avatarBgColorHex: String,
    val isCustom: Boolean = false,
    val isPrivate: Boolean = false,
    val ageCategory: String = "প্রাপ্তবয়স্ক",
    val chatCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
