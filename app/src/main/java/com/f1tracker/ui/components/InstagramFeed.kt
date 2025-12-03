package com.f1tracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import com.f1tracker.util.InstagramConstants
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.f1tracker.data.models.InstagramPost

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun InstagramFeedList(
    posts: List<InstagramPost>,
    michromaFont: FontFamily,
    onOpenInInstagram: (String) -> Unit,
    onNavigateToReels: (String) -> Unit = {},
    onScrollDirectionChange: (Boolean) -> Unit = {}, // true = up (show), false = down (hide)
    pagerState: androidx.compose.foundation.pager.PagerState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val currentlyPlayingIndex = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val isGlobalMuted = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) } // Start muted by default
    
    // Lifecycle handling to stop video when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                currentlyPlayingIndex.value = null // Stop playing
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Scroll detection to hide/show tabs
    val nestedScrollConnection = androidx.compose.runtime.remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10) { // Scrolling down
                    onScrollDirectionChange(false)
                } else if (available.y > 10) { // Scrolling up
                    onScrollDirectionChange(true)
                }
                return Offset.Zero
            }
        }
    }
    
    // Update currentlyPlayingIndex based on Pager state
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        if (posts.isNotEmpty()) {
            currentlyPlayingIndex.value = pagerState.currentPage
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        androidx.compose.foundation.pager.VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(16.dp),
            pageSpacing = 16.dp
        ) { index ->
            if (posts.isEmpty()) {
                SkeletonInstagramPostCard()
            } else {
                InstagramPostCard(
                    post = posts[index],
                    index = index,
                    michromaFont = michromaFont,
                    onOpenInInstagram = onOpenInInstagram,
                    onNavigateToReels = onNavigateToReels,
                    currentlyPlayingIndex = currentlyPlayingIndex,
                    isMuted = isGlobalMuted.value,
                    onMuteToggle = { isGlobalMuted.value = !isGlobalMuted.value }
                )
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }
}

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): androidx.compose.ui.graphics.Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.DarkGray.copy(alpha = 0.6f),
            Color.DarkGray.copy(alpha = 0.2f),
            Color.DarkGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition()
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800), repeatMode = RepeatMode.Reverse
            )
        )
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun SkeletonInstagramPostCard() {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Skeleton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(shimmerBrush())
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush())
                )
            }
            
            // Media Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(shimmerBrush())
            )
            
            // Footer Skeleton
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush())
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush())
                )
            }
        }
    }
}

@Composable
private fun InstagramPostCard(
    post: InstagramPost,
    michromaFont: FontFamily,
    onOpenInInstagram: (String) -> Unit,
    onNavigateToReels: (String) -> Unit = {},
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    currentlyPlayingIndex: androidx.compose.runtime.MutableState<Int?>,
    index: Int
) {
    // Determine if this is a reel (VIDEO with portrait orientation)
    val isReel = post.media_type == "VIDEO"
    val isCarousel = post.media_type == "CAROUSEL_ALBUM"
    val showVideoPlayer = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auto-play when this is the current playing index
    androidx.compose.runtime.LaunchedEffect(currentlyPlayingIndex.value) {
        if (isReel && currentlyPlayingIndex.value == index) {
            showVideoPlayer.value = true
        } else {
            showVideoPlayer.value = false
        }
    }
    
    // Container Box to center the card in the Pager item
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = if (isReel) Modifier.fillMaxSize() else Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = if (isReel) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                verticalArrangement = if (isReel) Arrangement.Top else Arrangement.Top
            ) {
            // Header: Minimal User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeAuthor = post.author ?: "f1"
                val isOfficial = InstagramConstants.isOfficialAccount(safeAuthor)
                
                // Author Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isOfficial) Color.White else Color(0xFFFFD700)),
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
                
                Text(
                    text = "@$safeAuthor",
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    color = if (isOfficial) Color.White else Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
                
                if (isOfficial) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF1DA1F2),
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Open in Instagram Icon
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = "Open",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onOpenInInstagram(post.permalink) }
                )
            }
            
            // Media Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isReel) Modifier.weight(1f) else Modifier) // Only weight for Reels
                    .background(if (isReel) Color.Black else Color.Transparent)
            ) {
                // Thumbnail
                val imageUrl = if (post.media_type == "VIDEO") {
                    post.thumbnail_url ?: post.media_url
                } else {
                    post.media_url
                }
                
                    if (imageUrl != null) {
                        SubcomposeAsyncImage(
                            model = imageUrl,
                            contentDescription = post.caption,
                            modifier = if (isReel) {
                                Modifier.fillMaxSize() 
                            } else {
                                Modifier.fillMaxWidth().wrapContentHeight().heightIn(max = 500.dp)
                            },
                            contentScale = if (isReel) ContentScale.Fit else ContentScale.FillWidth,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(shimmerBrush())
                                )
                            }
                        )
                    }
                
                // Video Player
                if (showVideoPlayer.value && isReel) {
                    val videoUrl = post.media_url ?: ""
                    if (videoUrl.isNotEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        val isVideoReady = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        val exoPlayer = androidx.compose.runtime.remember(showVideoPlayer.value) {
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
                                prepare()
                                playWhenReady = true
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                                volume = if (isMuted) 0f else 1f
                                
                                addListener(object : androidx.media3.common.Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        if (playbackState == androidx.media3.common.Player.STATE_READY) {
                                            isVideoReady.value = true
                                        }
                                    }
                                })
                            }
                        }
                        
                        androidx.compose.runtime.LaunchedEffect(isMuted) {
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        }
                        
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
                                    useController = false
                                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT) // Fix black frame
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isVideoReady.value) 1f else 0f)
                        )
                        
                        // Glassmorphic Loading Indicator (while buffering)
                        if (!isVideoReady.value) {
                            GlassmorphicLoadingIndicator()
                        }
                        
                        // Custom Controls Overlay (Bottom Right)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    // Click to Enter Reels Mode
                                    onNavigateToReels(post.permalink)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sound Toggle (Only this remains)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onMuteToggle() }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = "Toggle Sound",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                }
                            }
                        }
                    }
                }

                // Reel Overlay (when not playing)
                if (isReel && !showVideoPlayer.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable {
                                // Click to Enter Reels Mode
                                onNavigateToReels(post.permalink)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )

                    }
                }
            }
            
            // Footer: Minimal Caption
            if (!post.caption.isNullOrEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = post.caption,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
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

@Composable
fun GlassmorphicLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Glassmorphic Container
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f)) // Semi-transparent white
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Spinning Indicator
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}
