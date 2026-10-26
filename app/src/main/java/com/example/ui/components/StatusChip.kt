package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.StoryAmber

@Composable
fun StatusChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isOnline) Color(0xFF064E3B) else Color(0xFF451A03)
    val dotColor = if (isOnline) EmeraldGreen else StoryAmber
    val label = if (isOnline) "Gemini 2.5 Flash সক্রিয়" else "লোকাল স্টোরি ইঞ্জিন সক্রিয়"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
