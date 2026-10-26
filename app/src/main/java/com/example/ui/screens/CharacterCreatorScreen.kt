package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GolpoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreatorScreen(
    viewModel: GolpoViewModel,
    modifier: Modifier = Modifier
) {
    val name by viewModel.newCharName.collectAsState()
    val subtitle by viewModel.newCharSubtitle.collectAsState()
    val description by viewModel.newCharDescription.collectAsState()
    val personality by viewModel.newCharPersonality.collectAsState()
    val ageCategory by viewModel.newCharAgeCategory.collectAsState()
    val genre by viewModel.newCharGenre.collectAsState()
    val speakingStyle by viewModel.newCharSpeakingStyle.collectAsState()
    val dialect by viewModel.newCharDialect.collectAsState()
    val backstory by viewModel.newCharBackstory.collectAsState()
    val openingMessage by viewModel.newCharOpeningMessage.collectAsState()
    val selectedEmoji by viewModel.newCharAvatarEmoji.collectAsState()
    val selectedColorHex by viewModel.newCharBgColorHex.collectAsState()
    val isPrivate by viewModel.newCharIsPrivate.collectAsState()
    val isCreating by viewModel.isCreatingCharacter.collectAsState()

    val availableEmojis = listOf("🧙‍♂️", "👸", "🕵️", "🥷", "🧚‍♀️", "🧑‍🚀", "🎭", "🦊", "🐯", "🤖", "🎨", "🌟")
    val availableColors = listOf("#9333EA", "#2563EB", "#059669", "#DC2626", "#DB2777", "#D97706", "#4F46E5", "#0D9488")
    val ageOptions = listOf("প্রাপ্তবয়স্ক", "কিশোর/কিশোরী", "কাল্পনিক সত্তা")

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "নিজের AI চরিত্র তৈরি করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "তোমার পছন্দমতো স্বভাব ও কথার ভঙ্গি দিন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("creator_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar Selector
            item {
                Text(
                    text = "অবতার ইমোজি ও ব্যাকগ্রাউন্ড রঙ নির্বাচন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availableEmojis) { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.newCharAvatarEmoji.value = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availableColors) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.newCharBgColorHex.value = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.newCharName.value = it },
                    label = { Text("চরিত্রের নাম (যেমন: দীপন, প্রিয়ন্তী, মেঘদূত)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("char_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Subtitle
            item {
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { viewModel.newCharSubtitle.value = it },
                    label = { Text("পরিচয় বা উপাধি (যেমন: পাহাড়ের চিত্রশিল্পী)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Age category
            item {
                Text(
                    text = "বয়সের ধরন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ageOptions.forEach { opt ->
                        FilterChip(
                            selected = ageCategory == opt,
                            onClick = { viewModel.newCharAgeCategory.value = opt },
                            label = { Text(opt, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Genre
            item {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { viewModel.newCharGenre.value = it },
                    label = { Text("গল্পের ধরন (যেমন: রোমান্টিক, ফ্যান্টাসি, গোয়েন্দা)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Personality
            item {
                OutlinedTextField(
                    value = personality,
                    onValueChange = { viewModel.newCharPersonality.value = it },
                    label = { Text("চরিত্রের ব্যক্তিত্ব (যেমন: শান্ত, হাসিখুশি, সাহসী)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Speaking Style & Dialect
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = speakingStyle,
                        onValueChange = { viewModel.newCharSpeakingStyle.value = it },
                        label = { Text("কথা বলার ভঙ্গি") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = dialect,
                        onValueChange = { viewModel.newCharDialect.value = it },
                        label = { Text("পছন্দের উপভাষা") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Backstory
            item {
                OutlinedTextField(
                    value = backstory,
                    onValueChange = { viewModel.newCharBackstory.value = it },
                    label = { Text("চরিত্রের অতীত বা প্রেক্ষাপট") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // Opening message
            item {
                OutlinedTextField(
                    value = openingMessage,
                    onValueChange = { viewModel.newCharOpeningMessage.value = it },
                    label = { Text("প্রথম সূচনা বার্তা (চ্যাট খোলার সময় যা বলবে)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // Privacy Switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ব্যক্তিগত (Private) চরিত্র",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "শুধুমাত্র এই ডিভাইসেই সংরক্ষিত থাকবে",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { viewModel.newCharIsPrivate.value = it }
                    )
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.createCharacter() },
                    enabled = !isCreating && name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_create_character_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("চরিত্র তৈরি হচ্ছে...")
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("চরিত্র তৈরি ও চ্যাট শুরু", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
