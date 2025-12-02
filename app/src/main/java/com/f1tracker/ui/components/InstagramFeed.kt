package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Verified
import com.f1tracker.util.InstagramConstants
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.f1tracker.data.models.InstagramPost

@Composable
fun InstagramFeedList(
    posts: List<InstagramPost>,
    michromaFont: FontFamily,
    onOpenInInstagram: (String) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentlyPlayingIndex = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    
    // Observe lifecycle to pause videos when screen is not visible
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    // Stop all videos when app is backgrounded or screen loses focus
                    currentlyPlayingIndex.value = null
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Resume auto-play when returning
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    for (i in firstVisibleIndex until posts.size) {
                        if (posts[i].media_type == "VIDEO") {
                            currentlyPlayingIndex.value = i
                            break
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Track scroll position and update playing index with debounce
    androidx.compose.runtime.LaunchedEffect(listState.firstVisibleItemIndex) {
        // Debounce to avoid rapid switching during fast scrolls
        kotlinx.coroutines.delay(100)
        
        // Find the first video in the visible area
        val firstVisibleIndex = listState.firstVisibleItemIndex
        
        // Look for the first video starting from the first visible item
        for (i in firstVisibleIndex until posts.size) {
            if (posts[i].media_type == "VIDEO") {
                currentlyPlayingIndex.value = i
                break
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(posts) { index, post ->
            InstagramPostCard(
                post = post,
                index = index,
                michromaFont = michromaFont,
                onOpenInInstagram = onOpenInInstagram,
                currentlyPlayingIndex = currentlyPlayingIndex
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InstagramPostCard(
    post: InstagramPost,
    index: Int,
    michromaFont: FontFamily,
    onOpenInInstagram: (String) -> Unit,
    currentlyPlayingIndex: androidx.compose.runtime.MutableState<Int?>
) {
    // Determine if this is a reel (VIDEO with portrait orientation)
    val isReel = post.media_type == "VIDEO"
    val isCarousel = post.media_type == "CAROUSEL_ALBUM"
    val showVideoPlayer = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Auto-play when this is the current playing index
    androidx.compose.runtime.LaunchedEffect(currentlyPlayingIndex.value) {
        if (isReel && currentlyPlayingIndex.value == index) {
            showVideoPlayer.value = true
        } else {
            showVideoPlayer.value = false
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Header: User Info (Author + Verified Badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeAuthor = post.author ?: "f1"
                val isOfficial = InstagramConstants.isOfficialAccount(safeAuthor)
                
                // Author Avatar (Placeholder with first letter)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isOfficial) Color.White else Color(0xFFFFD700)), // White for official, Gold for meme
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = safeAuthor.take(2).uppercase(),
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@$safeAuthor",
                            fontFamily = michromaFont,
                            fontSize = 12.sp,
                            color = if (isOfficial) Color.White else Color(0xFFFFD700), // Gold for memes
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isOfficial) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF1DA1F2), // Twitter/Insta Blue
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = if (isOfficial) "Official Team" else "F1 Culture",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Open in Instagram Icon
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open in Instagram",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onOpenInInstagram(post.permalink) }
                )
            }
            
            // Media Content with Dynamic Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isReel) 9f / 16f else 4f / 5f)
                    .background(Color.Black)
            ) {
                // Always show thumbnail as base layer
                val imageUrl = if (post.media_type == "VIDEO") {
                    post.thumbnail_url ?: post.media_url
                } else {
                    post.media_url
                }
                
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = post.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("View on Instagram", color = Color.Gray)
                    }
                }
                
                // Layer video player on top when playing (eliminates black frame)
                if (showVideoPlayer.value && isReel) {
                    val videoUrl = post.media_url ?: ""
                    if (videoUrl.isNotEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        // Create and remember ExoPlayer
                        val exoPlayer = androidx.compose.runtime.remember(showVideoPlayer.value) {
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
                                prepare()
                                playWhenReady = true
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                            }
                        }
                        
                        // Cleanup when showVideoPlayer changes or composable is disposed
                        androidx.compose.runtime.DisposableEffect(showVideoPlayer.value) {
                            onDispose {
                                exoPlayer.stop()
                                exoPlayer.release()
                            }
                        }
                        
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Close button for video player
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                                .clickable { 
                                    showVideoPlayer.value = false 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Reel Overlay (only show when not playing)
                if (isReel && !showVideoPlayer.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable {
                                // Try to play video in-app
                                val videoUrl = post.media_url ?: ""
                                if (videoUrl.isNotEmpty()) {
                                    showVideoPlayer.value = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Large gradient play button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF0080),
                                            Color(0xFFE6007E)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Play Reel",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    
                    // "REEL" badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF0080), Color(0xFFFF4D94))
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "REEL",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Carousel Indicator
                if (isCarousel) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "Album",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ALBUM",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Footer: Caption & Metrics
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Likes",
                        tint = Color(0xFFFF0080),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(post.like_count),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(post.comments_count),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                post.caption?.let { caption ->
                    Text(
                        text = caption,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        
        when {
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            else -> "${duration.toDays() / 7}w"
        }
    } catch (e: Exception) {
        "Just now"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
