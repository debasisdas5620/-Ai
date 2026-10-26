package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GeneratedStory
import com.example.data.model.GolpoCharacter
import com.example.data.model.InteractiveStoryProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface GolpoDao {

    // Characters
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun getAllCharacters(): Flow<List<GolpoCharacter>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterById(id: String): GolpoCharacter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<GolpoCharacter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: GolpoCharacter)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacter(id: String)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getMessagesList(convId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chat_messages WHERE conversationId = :convId")
    suspend fun clearMessagesForConversation(convId: String)

    // Interactive Stories
    @Query("SELECT * FROM interactive_stories ORDER BY lastPlayedTime DESC")
    fun getAllStoryProgress(): Flow<List<InteractiveStoryProgress>>

    @Query("SELECT * FROM interactive_stories WHERE storyId = :storyId LIMIT 1")
    suspend fun getStoryProgress(storyId: String): InteractiveStoryProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStoryProgress(progress: InteractiveStoryProgress)

    @Query("DELETE FROM interactive_stories WHERE storyId = :storyId")
    suspend fun deleteStoryProgress(storyId: String)

    // Generated Stories
    @Query("SELECT * FROM generated_stories ORDER BY createdAt DESC")
    fun getAllGeneratedStories(): Flow<List<GeneratedStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedStory(story: GeneratedStory)

    @Query("DELETE FROM generated_stories WHERE id = :id")
    suspend fun deleteGeneratedStory(id: String)
}
