package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiResult
import com.example.data.ai.GeminiAiService
import com.example.data.ai.NextStoryChapter
import com.example.data.local.GolpoDao
import com.example.data.local.GolpoDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GeneratedStory
import com.example.data.model.GolpoCharacter
import com.example.data.model.InteractiveStoryProgress
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.model.PreloadedCharacters
import com.example.data.model.PreloadedStories
import com.example.data.model.StoryChoice
import com.example.data.model.StoryStarter
import com.example.data.model.StoryTurn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class GolpoRepository(
    private val dao: GolpoDao,
    val aiService: GeminiAiService
) {
    companion object {
        @Volatile
        private var INSTANCE: GolpoRepository? = null

        fun getInstance(context: Context): GolpoRepository {
            return INSTANCE ?: synchronized(this) {
                val db = GolpoDatabase.getDatabase(context)
                val service = GeminiAiService()
                val repo = GolpoRepository(db.golpoDao(), service)
                INSTANCE = repo
                repo.initializeDefaultData()
                repo
            }
        }
    }

    private fun initializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getAllCharacters().firstOrNull()
            if (existing.isNullOrEmpty()) {
                dao.insertCharacters(PreloadedCharacters.characters)
            }
        }
    }

    // Characters
    val allCharacters: Flow<List<GolpoCharacter>> = dao.getAllCharacters()

    suspend fun getCharacterById(id: String): GolpoCharacter? = dao.getCharacterById(id)

    suspend fun createCustomCharacter(character: GolpoCharacter) {
        dao.insertCharacter(character)
    }

    suspend fun deleteCharacter(id: String) {
        dao.deleteCharacter(id)
    }

    // Conversations & Chat
    val allConversations: Flow<List<Conversation>> = dao.getAllConversations()

    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>> {
        return dao.getMessagesForConversation(convId)
    }

    suspend fun getOrCreateConversation(character: GolpoCharacter): Conversation {
        val convId = "conv_${character.id}"
        val existing = dao.getConversationById(convId)
        if (existing != null) return existing

        val newConv = Conversation(
            id = convId,
            characterId = character.id,
            characterName = character.name,
            characterSubtitle = character.subtitle,
            characterAvatarEmoji = character.avatarEmoji,
            characterBgColorHex = character.avatarBgColorHex,
            lastMessage = character.openingMessage,
            lastMessageTime = System.currentTimeMillis(),
            messageCount = 1
        )
        dao.insertConversation(newConv)

        // Seed opening message
        val opening = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            sender = MessageSender.CHARACTER,
            text = character.openingMessage,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(opening)

        return newConv
    }

    suspend fun sendMessage(
        character: GolpoCharacter,
        conversationId: String,
        userText: String,
        onBotTyping: (Boolean) -> Unit
    ): AiResult<String> {
        // Save user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.USER,
            text = userText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )
        dao.insertMessage(userMsg)

        // Update conversation preview
        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.insertConversation(
                conv.copy(
                    lastMessage = userText,
                    lastMessageTime = System.currentTimeMillis(),
                    messageCount = conv.messageCount + 1
                )
            )
        }

        onBotTyping(true)

        val history = dao.getMessagesList(conversationId)
        val result = aiService.generateChatReply(character, history, userText)

        onBotTyping(false)

        val botReplyText = when (result) {
            is AiResult.Success -> result.data
            is AiResult.Error -> result.fallbackData ?: "দুঃখিত, সংযোগে সাময়িক সমস্যা হয়েছে।"
        }

        // Save bot reply
        val botMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.CHARACTER,
            text = botReplyText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )
        dao.insertMessage(botMsg)

        // Update conversation with character reply
        val updatedConv = dao.getConversationById(conversationId)
        if (updatedConv != null) {
            dao.insertConversation(
                updatedConv.copy(
                    lastMessage = botReplyText,
                    lastMessageTime = System.currentTimeMillis(),
                    messageCount = updatedConv.messageCount + 1
                )
            )
        }

        return result
    }

    suspend fun regenerateLastResponse(
        character: GolpoCharacter,
        conversationId: String,
        onBotTyping: (Boolean) -> Unit
    ): AiResult<String> {
        val messages = dao.getMessagesList(conversationId)
        if (messages.isEmpty()) return AiResult.Error("কোনো বার্তা নেই")

        // If last message is from character, delete it
        val lastMsg = messages.last()
        val userMsg = if (lastMsg.sender == MessageSender.CHARACTER) {
            dao.deleteMessage(lastMsg.id)
            messages.dropLast(1).lastOrNull { it.sender == MessageSender.USER }
        } else {
            lastMsg
        }

        if (userMsg == null) return AiResult.Error("ব্যবহারকারীর কোনো বার্তা পাওয়া যায়নি")

        onBotTyping(true)
        val history = dao.getMessagesList(conversationId)
        val result = aiService.generateChatReply(character, history, userMsg.text)
        onBotTyping(false)

        val botReplyText = when (result) {
            is AiResult.Success -> result.data
            is AiResult.Error -> result.fallbackData ?: "আবার চেষ্টা করা হলো।"
        }

        val botMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.CHARACTER,
            text = botReplyText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )
        dao.insertMessage(botMsg)

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.insertConversation(
                conv.copy(
                    lastMessage = botReplyText,
                    lastMessageTime = System.currentTimeMillis()
                )
            )
        }

        return result
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessage(messageId)
    }

    suspend fun clearConversation(conversationId: String, character: GolpoCharacter) {
        dao.clearMessagesForConversation(conversationId)
        // reseed initial opening message
        val opening = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.CHARACTER,
            text = character.openingMessage,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(opening)

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.insertConversation(
                conv.copy(
                    lastMessage = character.openingMessage,
                    lastMessageTime = System.currentTimeMillis(),
                    messageCount = 1
                )
            )
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        dao.clearMessagesForConversation(conversationId)
        dao.deleteConversation(conversationId)
    }

    suspend fun clearAllData() {
        dao.clearAllConversations()
    }

    // Interactive Stories
    val allSavedStoryProgress: Flow<List<InteractiveStoryProgress>> = dao.getAllStoryProgress()

    suspend fun getOrStartStory(starter: StoryStarter): InteractiveStoryProgress {
        val existing = dao.getStoryProgress(starter.id)
        if (existing != null) return existing

        val choicesJson = serializeChoices(starter.initialChoices)
        val initialTurns = listOf(
            StoryTurn(
                chapterNumber = 1,
                chapterTitle = starter.initialChapterTitle,
                narrative = starter.initialNarrative,
                selectedChoiceText = null
            )
        )
        val historyJson = serializeHistory(initialTurns)

        val newProgress = InteractiveStoryProgress(
            storyId = starter.id,
            title = starter.title,
            genre = starter.genre,
            protagonist = starter.protagonist,
            coverEmoji = starter.coverEmoji,
            coverBgColorHex = starter.coverBgColorHex,
            currentChapterNumber = 1,
            currentChapterTitle = starter.initialChapterTitle,
            currentNarrative = starter.initialNarrative,
            choicesJson = choicesJson,
            historyJson = historyJson,
            isCompleted = false,
            lastPlayedTime = System.currentTimeMillis()
        )
        dao.saveStoryProgress(newProgress)
        return newProgress
    }

    suspend fun makeStoryChoice(
        progress: InteractiveStoryProgress,
        chosenText: String
    ): AiResult<InteractiveStoryProgress> {
        val currentHistory = deserializeHistory(progress.historyJson).toMutableList()

        // Append current choice to the latest turn
        if (currentHistory.isNotEmpty()) {
            val lastIndex = currentHistory.size - 1
            currentHistory[lastIndex] = currentHistory[lastIndex].copy(selectedChoiceText = chosenText)
        }

        val aiResult = aiService.generateNextStoryChapter(
            storyTitle = progress.title,
            genre = progress.genre,
            protagonist = progress.protagonist,
            currentChapter = progress.currentChapterNumber,
            userDecision = chosenText,
            previousNarrative = progress.currentNarrative
        )

        val nextChapter: NextStoryChapter = when (aiResult) {
            is AiResult.Success -> aiResult.data
            is AiResult.Error -> return AiResult.Error(aiResult.message)
        }

        // Add new turn
        currentHistory.add(
            StoryTurn(
                chapterNumber = progress.currentChapterNumber + 1,
                chapterTitle = nextChapter.chapterTitle,
                narrative = nextChapter.narrative,
                selectedChoiceText = null
            )
        )

        val updated = progress.copy(
            currentChapterNumber = progress.currentChapterNumber + 1,
            currentChapterTitle = nextChapter.chapterTitle,
            currentNarrative = nextChapter.narrative,
            choicesJson = serializeChoices(nextChapter.choices),
            historyJson = serializeHistory(currentHistory),
            isCompleted = nextChapter.isEnding,
            endingType = nextChapter.endingType,
            lastPlayedTime = System.currentTimeMillis()
        )
        dao.saveStoryProgress(updated)
        return AiResult.Success(updated, (aiResult as? AiResult.Success)?.isOnlineModel ?: false, "Story Model")
    }

    suspend fun restartStory(storyId: String) {
        val starter = PreloadedStories.starters.firstOrNull { it.id == storyId }
        if (starter != null) {
            dao.deleteStoryProgress(storyId)
            getOrStartStory(starter)
        }
    }

    // Generated Stories
    val allGeneratedStories: Flow<List<GeneratedStory>> = dao.getAllGeneratedStories()

    suspend fun saveGeneratedStory(story: GeneratedStory) {
        dao.insertGeneratedStory(story)
    }

    suspend fun deleteGeneratedStory(id: String) {
        dao.deleteGeneratedStory(id)
    }

    // Serializers
    fun serializeChoices(choices: List<StoryChoice>): String {
        val array = JSONArray()
        for (c in choices) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("text", c.text)
            obj.put("effectSummary", c.effectSummary)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeChoices(json: String): List<StoryChoice> {
        val list = mutableListOf<StoryChoice>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StoryChoice(
                        id = obj.optString("id", "c$i"),
                        text = obj.optString("text", ""),
                        effectSummary = obj.optString("effectSummary", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun serializeHistory(history: List<StoryTurn>): String {
        val array = JSONArray()
        for (turn in history) {
            val obj = JSONObject()
            obj.put("chapterNumber", turn.chapterNumber)
            obj.put("chapterTitle", turn.chapterTitle)
            obj.put("narrative", turn.narrative)
            obj.put("selectedChoiceText", turn.selectedChoiceText ?: "")
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeHistory(json: String): List<StoryTurn> {
        val list = mutableListOf<StoryTurn>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StoryTurn(
                        chapterNumber = obj.optInt("chapterNumber", 1),
                        chapterTitle = obj.optString("chapterTitle", ""),
                        narrative = obj.optString("narrative", ""),
                        selectedChoiceText = obj.optString("selectedChoiceText", "").ifEmpty { null }
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
