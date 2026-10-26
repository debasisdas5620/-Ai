package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiResult
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GeneratedStory
import com.example.data.model.GolpoCharacter
import com.example.data.model.InteractiveStoryProgress
import com.example.data.model.PreloadedStories
import com.example.data.model.StoryChoice
import com.example.data.model.StoryStarter
import com.example.data.model.StoryTurn
import com.example.data.repository.GolpoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class GolpoScreen {
    HOME,
    CHARACTERS,
    STORIES,
    CHATS,
    PROFILE,
    CHAT_SCREEN,
    STORY_PLAYER,
    STORY_GENERATOR,
    CHARACTER_CREATOR,
    CHARACTER_DETAIL,
    ADMIN_DASHBOARD,
    PRIVACY_POLICY,
    SETTINGS
}

class GolpoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GolpoRepository.getInstance(application)

    // Navigation stack for clean backhandling
    private val _screenStack = MutableStateFlow(listOf(GolpoScreen.HOME))
    val currentScreen: StateFlow<GolpoScreen> = MutableStateFlow(GolpoScreen.HOME).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                this@apply.value = stack.lastOrNull() ?: GolpoScreen.HOME
            }
        }
    }

    fun navigateTo(screen: GolpoScreen) {
        val currentList = _screenStack.value.toMutableList()
        // If it's a top-level tab, replace root
        if (screen in listOf(GolpoScreen.HOME, GolpoScreen.CHARACTERS, GolpoScreen.STORIES, GolpoScreen.CHATS, GolpoScreen.PROFILE)) {
            _screenStack.value = listOf(screen)
        } else {
            currentList.add(screen)
            _screenStack.value = currentList
        }
    }

    fun navigateBack(): Boolean {
        val currentList = _screenStack.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.size - 1)
            _screenStack.value = currentList
            return true
        }
        return false
    }

    // App Preferences
    val isDarkTheme = MutableStateFlow(true)
    val isOnlineModelActive = MutableStateFlow(repository.aiService.isOnlineConfigured())

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    // Search and Genre Filters
    val searchQuery = MutableStateFlow("")
    val selectedGenreFilter = MutableStateFlow("সব")

    val genres = listOf(
        "সব",
        "রোমান্টিক গল্প",
        "রহস্য ও গোয়েন্দা গল্প",
        "ভৌতিক গল্প",
        "ফ্যান্টাসি ও অ্যাডভেঞ্চার",
        "বন্ধুত্বের গল্প"
    )

    // Character Lists
    val allCharacters: StateFlow<List<GolpoCharacter>> = repository.allCharacters.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredCharacters: StateFlow<List<GolpoCharacter>> = combine(
        allCharacters,
        searchQuery,
        selectedGenreFilter
    ) { characters, query, genre ->
        characters.filter { char ->
            val matchesGenre = (genre == "সব" || char.genre.contains(genre, ignoreCase = true) || genre.contains(char.genre, ignoreCase = true))
            val matchesQuery = query.isEmpty() ||
                    char.name.contains(query, ignoreCase = true) ||
                    char.subtitle.contains(query, ignoreCase = true) ||
                    char.description.contains(query, ignoreCase = true)
            matchesGenre && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Conversations
    val conversations: StateFlow<List<Conversation>> = repository.allConversations.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Active Character & Active Chat
    private val _activeCharacter = MutableStateFlow<GolpoCharacter?>(null)
    val activeCharacter: StateFlow<GolpoCharacter?> = _activeCharacter.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val isBotTyping = MutableStateFlow(false)
    val chatInputText = MutableStateFlow("")
    val editingMessage = MutableStateFlow<ChatMessage?>(null)

    fun startChatWithCharacter(character: GolpoCharacter) {
        _activeCharacter.value = character
        viewModelScope.launch {
            val conv = repository.getOrCreateConversation(character)
            _activeConversation.value = conv
            navigateTo(GolpoScreen.CHAT_SCREEN)

            // collect messages
            repository.getMessagesForConversation(conv.id).collect { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun openCharacterDetails(character: GolpoCharacter) {
        _activeCharacter.value = character
        navigateTo(GolpoScreen.CHARACTER_DETAIL)
    }

    fun sendChatMessage() {
        val text = chatInputText.value.trim()
        val character = _activeCharacter.value ?: return
        val conv = _activeConversation.value ?: return
        if (text.isEmpty()) return

        chatInputText.value = ""
        val editMsg = editingMessage.value

        viewModelScope.launch {
            if (editMsg != null) {
                // Edit and resend
                repository.deleteMessage(editMsg.id)
                editingMessage.value = null
            }
            repository.sendMessage(
                character = character,
                conversationId = conv.id,
                userText = text,
                onBotTyping = { isBotTyping.value = it }
            )
            isOnlineModelActive.value = repository.aiService.isOnlineConfigured()
        }
    }

    fun regenerateLastMessage() {
        val character = _activeCharacter.value ?: return
        val conv = _activeConversation.value ?: return

        viewModelScope.launch {
            repository.regenerateLastResponse(
                character = character,
                conversationId = conv.id,
                onBotTyping = { isBotTyping.value = it }
            )
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun startEditingMessage(message: ChatMessage) {
        editingMessage.value = message
        chatInputText.value = message.text
    }

    fun cancelEditing() {
        editingMessage.value = null
        chatInputText.value = ""
    }

    fun clearCurrentChat() {
        val character = _activeCharacter.value ?: return
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.clearConversation(conv.id, character)
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteConversation(convId)
            if (_activeConversation.value?.id == convId) {
                _activeConversation.value = null
                _activeCharacter.value = null
                if (currentScreen.value == GolpoScreen.CHAT_SCREEN) {
                    navigateBack()
                }
            }
        }
    }

    // Interactive Story Mode
    val preloadedStarters = PreloadedStories.starters

    private val _activeStoryProgress = MutableStateFlow<InteractiveStoryProgress?>(null)
    val activeStoryProgress: StateFlow<InteractiveStoryProgress?> = _activeStoryProgress.asStateFlow()

    val currentStoryChoices = MutableStateFlow<List<StoryChoice>>(emptyList())
    val currentStoryHistory = MutableStateFlow<List<StoryTurn>>(emptyList())
    val isStoryLoading = MutableStateFlow(false)
    val customStoryActionInput = MutableStateFlow("")

    fun startInteractiveStory(starter: StoryStarter) {
        viewModelScope.launch {
            isStoryLoading.value = true
            val progress = repository.getOrStartStory(starter)
            _activeStoryProgress.value = progress
            currentStoryChoices.value = repository.deserializeChoices(progress.choicesJson)
            currentStoryHistory.value = repository.deserializeHistory(progress.historyJson)
            isStoryLoading.value = false
            navigateTo(GolpoScreen.STORY_PLAYER)
        }
    }

    fun chooseStoryAction(chosenText: String) {
        val progress = _activeStoryProgress.value ?: return
        if (isStoryLoading.value) return

        viewModelScope.launch {
            isStoryLoading.value = true
            val result = repository.makeStoryChoice(progress, chosenText)
            if (result is AiResult.Success) {
                val updated = result.data
                _activeStoryProgress.value = updated
                currentStoryChoices.value = repository.deserializeChoices(updated.choicesJson)
                currentStoryHistory.value = repository.deserializeHistory(updated.historyJson)
            }
            isStoryLoading.value = false
            customStoryActionInput.value = ""
        }
    }

    fun submitCustomStoryAction() {
        val text = customStoryActionInput.value.trim()
        if (text.isNotEmpty()) {
            chooseStoryAction(text)
        }
    }

    fun restartActiveStory() {
        val progress = _activeStoryProgress.value ?: return
        viewModelScope.launch {
            repository.restartStory(progress.storyId)
            val starter = PreloadedStories.starters.firstOrNull { it.id == progress.storyId }
            if (starter != null) {
                val newProgress = repository.getOrStartStory(starter)
                _activeStoryProgress.value = newProgress
                currentStoryChoices.value = repository.deserializeChoices(newProgress.choicesJson)
                currentStoryHistory.value = repository.deserializeHistory(newProgress.historyJson)
            }
        }
    }

    // Story Generator Tool
    val genTitle = MutableStateFlow("মায়াবী পাহাড়ি স্টেশন")
    val genGenre = MutableStateFlow("রহস্য ও গোয়েন্দা গল্প")
    val genProtagonist = MutableStateFlow("অদিতি, তরুণী সাংবাদিক")
    val genLength = MutableStateFlow("মাঝারি (৩টি দৃশ্য)")
    val genSetting = MutableStateFlow("দার্জিলিংয়ের কুয়াশাচ্ছন্ন সান্ধ্যকালীন রেলস্টেশন")
    val genMood = MutableStateFlow("গা-ছমছমে ও আবেগপূর্ণ")
    val genEnding = MutableStateFlow("অপ্রত্যাশিত চমৎকার সমাপ্তি")
    val isGeneratingStory = MutableStateFlow(false)
    val generatedStoryOutput = MutableStateFlow<String?>(null)

    val savedGeneratedStories: StateFlow<List<GeneratedStory>> = repository.allGeneratedStories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun generateStoryFromForm() {
        viewModelScope.launch {
            isGeneratingStory.value = true
            generatedStoryOutput.value = null
            val result = repository.aiService.generateCustomStory(
                title = genTitle.value,
                genre = genGenre.value,
                protagonist = genProtagonist.value,
                lengthCategory = genLength.value,
                setting = genSetting.value,
                mood = genMood.value,
                endingType = genEnding.value
            )
            when (result) {
                is AiResult.Success -> {
                    generatedStoryOutput.value = result.data
                    // Save to Room
                    val newStory = GeneratedStory(
                        id = UUID.randomUUID().toString(),
                        title = genTitle.value,
                        genre = genGenre.value,
                        protagonist = genProtagonist.value,
                        lengthCategory = genLength.value,
                        setting = genSetting.value,
                        mood = genMood.value,
                        endingType = genEnding.value,
                        fullText = result.data
                    )
                    repository.saveGeneratedStory(newStory)
                }
                is AiResult.Error -> {
                    generatedStoryOutput.value = "গল্প তৈরি করা সম্ভব হয়নি: ${result.message}"
                }
            }
            isGeneratingStory.value = false
        }
    }

    fun deleteSavedGeneratedStory(id: String) {
        viewModelScope.launch {
            repository.deleteGeneratedStory(id)
        }
    }

    // Custom Character Creator
    val newCharName = MutableStateFlow("")
    val newCharSubtitle = MutableStateFlow("")
    val newCharDescription = MutableStateFlow("")
    val newCharPersonality = MutableStateFlow("")
    val newCharAgeCategory = MutableStateFlow("প্রাপ্তবয়স্ক")
    val newCharGenre = MutableStateFlow("রোমান্টিক গল্প")
    val newCharSpeakingStyle = MutableStateFlow("মার্জিত ও মিষ্টি")
    val newCharDialect = MutableStateFlow("প্রমিত বাংলা")
    val newCharBackstory = MutableStateFlow("")
    val newCharOpeningMessage = MutableStateFlow("")
    val newCharAvatarEmoji = MutableStateFlow("✨")
    val newCharBgColorHex = MutableStateFlow("#9333EA")
    val newCharIsPrivate = MutableStateFlow(false)
    val isCreatingCharacter = MutableStateFlow(false)

    fun createCharacter() {
        val name = newCharName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            isCreatingCharacter.value = true
            val id = "custom_${UUID.randomUUID()}"
            val char = GolpoCharacter(
                id = id,
                name = name,
                subtitle = newCharSubtitle.value.ifEmpty { "কাস্টম AI চরিত্র" },
                description = newCharDescription.value.ifEmpty { "$name এর সাথে চমৎকার গল্প করুন।" },
                personality = newCharPersonality.value.ifEmpty { "বন্ধুবৎসল ও বুদ্ধিদীপ্ত" },
                backstory = newCharBackstory.value.ifEmpty { "এক অনন্য অতীতের অধিকারী।" },
                speakingStyle = newCharSpeakingStyle.value.ifEmpty { "স্বাভাবিক ও প্রাণবন্ত" },
                dialect = newCharDialect.value.ifEmpty { "প্রমিত বাংলা" },
                interests = "গল্প বলা, আড্ডা ও সাহিত্য",
                genre = newCharGenre.value,
                openingMessage = newCharOpeningMessage.value.ifEmpty { "নমস্কার! আমি $name। তোমার সাথে কথা বলতে এসে খুব ভালো লাগছে।" },
                systemPrompt = "You are $name, a custom Bengali AI fictional character with personality: ${newCharPersonality.value}. Speak in natural ${newCharDialect.value} Bengali.",
                avatarEmoji = newCharAvatarEmoji.value,
                avatarBgColorHex = newCharBgColorHex.value,
                isCustom = true,
                isPrivate = newCharIsPrivate.value,
                ageCategory = newCharAgeCategory.value
            )
            repository.createCustomCharacter(char)
            isCreatingCharacter.value = false
            // Reset form
            newCharName.value = ""
            newCharSubtitle.value = ""
            newCharDescription.value = ""
            newCharPersonality.value = ""
            newCharBackstory.value = ""
            newCharOpeningMessage.value = ""
            navigateBack()
            // Open newly created character chat directly
            startChatWithCharacter(char)
        }
    }

    fun deleteCharacter(id: String) {
        viewModelScope.launch {
            repository.deleteCharacter(id)
            if (_activeCharacter.value?.id == id) {
                _activeCharacter.value = null
                navigateBack()
            }
        }
    }

    // Admin Tools
    val adminModelStatus = MutableStateFlow("পরীক্ষা করা হয়নি")
    val adminTestReply = MutableStateFlow("")
    val isAdminTesting = MutableStateFlow(false)
    val customApiKeyInput = MutableStateFlow("")

    fun testAdminModelConnection() {
        viewModelScope.launch {
            isAdminTesting.value = true
            adminModelStatus.value = "সংযোগ পরীক্ষা করা হচ্ছে..."
            try {
                val dummyChar = GolpoCharacter(
                    id = "test",
                    name = "পরীক্ষক",
                    subtitle = "সিস্টেম টেস্ট",
                    description = "টেস্ট",
                    personality = "টেস্ট",
                    backstory = "টেস্ট",
                    speakingStyle = "টেস্ট",
                    dialect = "প্রমিত বাংলা",
                    interests = "টেস্ট",
                    genre = "টেস্ট",
                    openingMessage = "টেস্ট",
                    systemPrompt = "You are a test assistant. Reply with 'বাংলা AI সম্পূর্ণ সচল ও প্রস্তুত!'",
                    avatarEmoji = "⚡",
                    avatarBgColorHex = "#9333EA"
                )
                val res = repository.aiService.generateChatReply(dummyChar, emptyList(), "হাই, সিস্টেম টেস্ট")
                when (res) {
                    is AiResult.Success -> {
                        adminModelStatus.value = if (res.isOnlineModel) "অনলাইন (Gemini 2.5 Flash সক্রিয়)" else "অফলাইন স্টোরি ইঞ্জিন সক্রিয়"
                        adminTestReply.value = res.data
                    }
                    is AiResult.Error -> {
                        adminModelStatus.value = "সমস্যা: ${res.message}"
                        adminTestReply.value = res.fallbackData ?: ""
                    }
                }
            } catch (e: Exception) {
                adminModelStatus.value = "এরর: ${e.message}"
            }
            isAdminTesting.value = false
        }
    }

    fun applyCustomApiKey(key: String) {
        repository.aiService.customApiKey = key.trim()
        isOnlineModelActive.value = repository.aiService.isOnlineConfigured()
        testAdminModelConnection()
    }

    fun clearAllConversationsAndHistory() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeConversation.value = null
            _activeCharacter.value = null
            _chatMessages.value = emptyList()
        }
    }
}
