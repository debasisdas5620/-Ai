package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class StoryChoice(
    val id: String,
    val text: String,
    val effectSummary: String = ""
)

data class StoryTurn(
    val chapterNumber: Int,
    val chapterTitle: String,
    val narrative: String,
    val selectedChoiceText: String? = null
)

@Entity(tableName = "interactive_stories")
data class InteractiveStoryProgress(
    @PrimaryKey
    val storyId: String,
    val title: String,
    val genre: String,
    val protagonist: String,
    val coverEmoji: String,
    val coverBgColorHex: String,
    val currentChapterNumber: Int = 1,
    val currentChapterTitle: String,
    val currentNarrative: String,
    val choicesJson: String, // serialized choices
    val historyJson: String, // serialized history turns
    val isCompleted: Boolean = false,
    val endingType: String? = null,
    val lastPlayedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_stories")
data class GeneratedStory(
    @PrimaryKey
    val id: String,
    val title: String,
    val genre: String,
    val protagonist: String,
    val lengthCategory: String, // ছোট, মাঝারি, দীর্ঘ
    val setting: String,
    val mood: String,
    val endingType: String,
    val fullText: String,
    val createdAt: Long = System.currentTimeMillis()
)
