package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.ui.screens.feed.components.InstagramPostCard

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip

@Composable
fun SocialSection(
    posts: List<InstagramPost>,
    onSocialClick: (String) -> Unit,
    onViewMoreClick: () -> Unit
) {
    if (posts.isEmpty()) return

    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    // Auto-play state: Always try to play the first item (Index 0)
    // We bind currentlyPlayingIndex to 0. 
    val currentlyPlayingIndex = remember { mutableStateOf<Int?>(0) }
    val isMuted = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header with navigation arrow (Replicated from NewsSection)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMoreClick() }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOCIAL >",
                fontFamily = brigendsFont,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
        
        // List of Cards (Max 3) - Horizontal Scroll
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(posts.take(3)) { index, post ->
                // Fixed width for horizontal scrolling cards
                Box(
                    modifier = Modifier
                        .width(320.dp) // consistent width for horizontal cards
                        .height(500.dp)
                        .clickable { onSocialClick(post.permalink) }
                ) {
                    InstagramPostCard(
                        post = post,
                        index = index, // 0, 1, 2
                        michromaFont = michromaFont,
                        onOpenInInstagram = { /* Optional: maybe deep link? or just use onSocialClick */ },
                        onNavigateToReels = { onSocialClick(post.permalink) },
                        currentlyPlayingIndex = currentlyPlayingIndex,
                        isMuted = isMuted.value,
                        onMuteToggle = { isMuted.value = !isMuted.value },
                        enableCarousel = false
                    )
                }
            }
            
            // View More Card (Replicated behavior)
            item {
                SocialViewMoreCard(onClick = onViewMoreClick, michromaFont = michromaFont)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SocialViewMoreCard(
    onClick: () -> Unit,
    michromaFont: FontFamily
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(500.dp) // Match Social Card height
            .clip(RoundedCornerShape(16.dp)) // Match Social Card corner
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFF0080).copy(alpha = 0.1f),
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "View More",
                    tint = Color(0xFFFF0080),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "View All",
                fontFamily = michromaFont,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
