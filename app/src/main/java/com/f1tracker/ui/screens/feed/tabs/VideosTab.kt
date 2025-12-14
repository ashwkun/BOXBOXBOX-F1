package com.f1tracker.ui.screens.feed.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.data.models.F1Video
import com.f1tracker.ui.screens.feed.components.cards.VideoCard

@Composable
fun VideosTab(
    videos: List<F1Video>,
    michromaFont: FontFamily,
    onVideoClick: (String) -> Unit,
    listState: LazyListState,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val availableFilters = listOf("Hot", "Latest", "Highlights", "Popular", "Official")
    
    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            "Hot" -> videos
            "Latest" -> videos.sortedByDescending { 
                try {
                    java.time.Instant.parse(it.publishedDate)
                } catch (e: Exception) {
                    java.time.Instant.EPOCH
                }
            }
            "Highlights" -> {
                val strictHighlightPattern = Regex(
                    "^(Race|FP1|FP2|FP3|Qualifying|Sprint|Sprint Qualifying)\\s+Highlights",
                    RegexOption.IGNORE_CASE
                )
                val exclusionPatterns = listOf(
                    Regex("#shorts", RegexOption.IGNORE_CASE),
                    Regex("react", RegexOption.IGNORE_CASE),
                    Regex("interview", RegexOption.IGNORE_CASE),
                    Regex("debrief", RegexOption.IGNORE_CASE),
                    Regex("press conference", RegexOption.IGNORE_CASE),
                    Regex("\\bF2\\b", RegexOption.IGNORE_CASE),
                    Regex("\\bF3\\b", RegexOption.IGNORE_CASE),
                    Regex("Formula 2", RegexOption.IGNORE_CASE),
                    Regex("Formula 3", RegexOption.IGNORE_CASE)
                )
                
                videos.filter { video ->
                    val isHighlight = strictHighlightPattern.containsMatchIn(video.title)
                    val isExcluded = exclusionPatterns.any { it.containsMatchIn(video.title) }
                    isHighlight && !isExcluded
                }.sortedByDescending {
                    try {
                        java.time.Instant.parse(it.publishedDate)
                    } catch (e: Exception) {
                        java.time.Instant.EPOCH
                    }
                }
            }
            "Popular" -> {
                val now = java.time.Instant.now()
                val sorted = videos.sortedByDescending { video ->
                    val randomFactor = 0.85 + (Math.random() * 0.30)
                    val f1Debuff = if (video.channelTitle == "FORMULA 1") 0.6 else 1.0
                    
                    val hoursAge = try {
                        val publishTime = java.time.Instant.parse(video.publishedDate)
                        java.time.Duration.between(publishTime, now).toHours().toDouble()
                    } catch (e: Exception) { 8760.0 }
                    
                    val decayHalfLifeHours = if (video.channelTitle == "FORMULA 1") 4380.0 else 8760.0
                    val ageFactor = Math.exp(-hoursAge / decayHalfLifeHours)
                    
                    video.viewCount * randomFactor * f1Debuff * ageFactor
                }
                val diversified = mutableListOf<F1Video>()
                val pending = sorted.toMutableList()
                while (pending.isNotEmpty()) {
                    val candidate = pending.firstOrNull { video ->
                        val recentChannels = diversified.takeLast(5).map { it.channelTitle }
                        recentChannels.count { it == video.channelTitle } < 3
                    } ?: pending.first()
                    diversified.add(candidate)
                    pending.remove(candidate)
                }
                diversified
            }
            "Official" -> videos.filter { it.channelTitle == "FORMULA 1" }
                .sortedByDescending { it.viewCount }
            else -> videos
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onFilterSelected(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.uppercase(),
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        item {
            Text(
                text = "${filteredVideos.size} videos",
                modifier = Modifier.padding(horizontal = 20.dp),
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        items(filteredVideos) { video ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                VideoCard(
                    video = video,
                    michromaFont = michromaFont,
                    onVideoClick = onVideoClick
                )
            }
        }
    }
}
