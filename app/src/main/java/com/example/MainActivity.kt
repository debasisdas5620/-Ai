package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.components.GolpoBottomBar
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CharacterCreatorScreen
import com.example.ui.screens.CharacterDetailScreen
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedChatsScreen
import com.example.ui.screens.StoryGeneratorScreen
import com.example.ui.screens.StoryPlayerScreen
import com.example.ui.screens.StoryWorldScreen
import com.example.ui.theme.GolpoTheme
import com.example.ui.viewmodel.GolpoScreen
import com.example.ui.viewmodel.GolpoViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GolpoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            GolpoTheme(darkTheme = isDark) {
                GolpoApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GolpoApp(viewModel: GolpoViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    val isTopLevel = currentScreen in listOf(
        GolpoScreen.HOME,
        GolpoScreen.CHARACTERS,
        GolpoScreen.STORIES,
        GolpoScreen.CHATS,
        GolpoScreen.PROFILE
    )

    BackHandler(enabled = !isTopLevel) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevel) {
                GolpoBottomBar(
                    currentScreen = currentScreen,
                    unreadChatsCount = conversations.size,
                    onTabSelected = { screen -> viewModel.navigateTo(screen) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTopLevel) innerPadding else androidx.compose.foundation.layout.PaddingValues())
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    GolpoScreen.HOME -> HomeScreen(viewModel = viewModel)
                    GolpoScreen.CHARACTERS -> CharactersScreen(
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                    GolpoScreen.STORIES -> StoryWorldScreen(
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                    GolpoScreen.CHATS -> SavedChatsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                    GolpoScreen.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                    GolpoScreen.CHAT_SCREEN -> ChatScreen(viewModel = viewModel)
                    GolpoScreen.STORY_PLAYER -> StoryPlayerScreen(viewModel = viewModel)
                    GolpoScreen.STORY_GENERATOR -> StoryGeneratorScreen(viewModel = viewModel)
                    GolpoScreen.CHARACTER_CREATOR -> CharacterCreatorScreen(viewModel = viewModel)
                    GolpoScreen.CHARACTER_DETAIL -> CharacterDetailScreen(viewModel = viewModel)
                    GolpoScreen.ADMIN_DASHBOARD -> AdminScreen(viewModel = viewModel)
                    GolpoScreen.PRIVACY_POLICY -> PrivacyScreen(viewModel = viewModel)
                    GolpoScreen.SETTINGS -> ProfileScreen(
                        viewModel = viewModel,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }
        }
    }
}
