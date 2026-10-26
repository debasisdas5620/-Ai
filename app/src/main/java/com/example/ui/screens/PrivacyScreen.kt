package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GolpoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: GolpoViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("গোপনীয়তা ও নিরাপত্তা নীতি", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("privacy_back_button")
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
            item {
                PrivacySection(
                    title = "১. AI চরিত্র সংক্রান্ত স্পষ্ট ঘোষণা",
                    body = "গল্পAI অ্যাপের সমস্ত চরিত্র সম্পূর্ণ কাল্পনিক ও কৃত্রিম বুদ্ধিমত্তা দ্বারা পরিচালিত। এই চরিত্রগুলো বাস্তব রক্ত-মাংসের মানুষ নয় এবং কোনো চরিত্রই নিজেকে বাস্তব ব্যক্তি দাবি করার অনুমতিপ্রাপ্ত নয়। এগুলি সাহিত্যিক চর্চা ও নির্মল বিনোদনের উদ্দেশ্যে তৈরি।"
                )
            }

            item {
                PrivacySection(
                    title = "২. সম্পূর্ণ ফ্রি ও বিজ্ঞাপনবিহীন অঙ্গীকার",
                    body = "গল্পAI-তে কোনো ধরণের পেইড সাবস্ক্রিপশন, ইন-অ্যাপ কেনাকাটা বা ব্যবহারকারীর দৃষ্টি বিভ্রান্তকারী বিজ্ঞাপন নেই। সমস্ত গল্প, ক্যারেক্টার ও ফিচার সবার জন্য সম্পূর্ণ উন্মুক্ত।"
                )
            }

            item {
                PrivacySection(
                    title = "৩. তথ্যের গোপনীয়তা ও স্থানীয় সংরক্ষণ",
                    body = "আপনার কথোপকথন ও ব্যক্তিগত চ্যাট হিস্ট্রি আপনার ডিভাইসের নিজস্ব ডাটাবেজে (Room SQLite) স্থানীয়ভাবে সংরক্ষিত থাকে। আপনার সম্মতি ব্যতীত আপনার বার্তা কোনো তৃতীয় পক্ষের বিজ্ঞাপনদাতার সাথে শেয়ার করা হয় না।"
                )
            }

            item {
                PrivacySection(
                    title = "৪. ডেটা মুছে ফেলার পূর্ণ অধিকার",
                    body = "ব্যবহারকারী যেকোনো সময় তাঁর ব্যক্তিগত চ্যাট বা সমস্ত কথোপকথন একক ক্লিকে 'প্রোফাইল > সকল চ্যাট ও ইতিহাস মুছুন' বিকল্পের মাধ্যমে স্থায়ীভাবে মুছে ফেলতে পারবেন।"
                )
            }

            item {
                PrivacySection(
                    title = "৫. নিরাপত্তা ও কন্টেন্ট শালীনতা",
                    body = "ক্ষতিকর, অপমানজনক বা বিপজ্জনক কোনো প্রম্পট বা বিষয়বস্তু সৃষ্টি না করার জন্য কঠোর নিরাপত্তা ব্যবস্থা অনুসরণ করা হয়।"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun PrivacySection(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
