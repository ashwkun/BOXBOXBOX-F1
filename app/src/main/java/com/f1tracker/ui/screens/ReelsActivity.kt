
package com.f1tracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.ui.theme.F1TrackerTheme
import com.f1tracker.ui.viewmodels.ReelsViewModel
import com.f1tracker.util.InstagramConstants
import com.f1tracker.ui.components.GlassmorphicLoadingIndicator
import com.f1tracker.ui.util.ExoPlayerPool
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F1TrackerTheme {
                ReelsScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = hiltViewModel(),
    onClose: () -> Unit,
    startPermalink: String? = null,
    refreshTrigger: Long = 0L
) {
    val reels by viewModel.reels.collectAsState()
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    // Handle start_permalink from Intent if not passed explicitly (fallback)
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val effectiveStartPermalink = startPermalink ?: activity?.intent?.getStringExtra("start_permalink")
    
    if (reels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            GlassmorphicLoadingIndicator()
        }
        return
    }

    // Find initial index
    val initialIndex = remember(reels, effectiveStartPermalink) {
        if (effectiveStartPermalink != null) {
            reels.indexOfFirst { it.permalink == effectiveStartPermalink }.coerceAtLeast(0)
        } else {
            0
        }
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { reels.size })

    // Handle Refresh Trigger
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            pagerState.animateScrollToPage(0)
            // viewModel.refreshReels() // If we had a refresh function
        }
    }

    VerticalPager(
        state = pagerState,
        beyondBoundsPageCount = 1, // Preload next page
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        // Preload next video logic could go here, but ExoPlayer handles buffering.
        // We can optimize by creating the player for page + 1 but not playing it.
        
        ReelItem(
            post = reels[page],
            isPlaying = (pagerState.currentPage == page),
            michromaFont = michromaFont,
            onClose = onClose,
            onVideoError = { viewModel.onVideoError() }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelItem(
    post: InstagramPost,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onClose: () -> Unit,
    onVideoError: () -> Unit = {}
) {
    val context = LocalContext.current
    val isCarousel = post.media_type == "CAROUSEL_ALBUM" && !post.children?.data.isNullOrEmpty()
    
    android.util.Log.d("ReelsDebug", "Post ID: ${post.id}, Type: ${post.media_type}, Children: ${post.children?.data?.size}, isCarousel: $isCarousel")
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (post.media_type == "CAROUSEL_ALBUM") {
            if (!post.children?.data.isNullOrEmpty()) {
                val children = post.children!!.data
                val pagerState = rememberPagerState(pageCount = { children.size })
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val child = children[page]
                    // Only play video if the parent ReelItem is playing AND this is the current slide
                    val isChildPlaying = isPlaying && pagerState.currentPage == page
                    
                    ReelMediaItem(
                        mediaUrl = child.media_url,
                        thumbnailUrl = child.thumbnail_url,
                        mediaType = child.media_type,
                        isPlaying = isChildPlaying,
                        onVideoError = onVideoError
                    )
                }
                
                // Carousel Indicator
                if (children.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(children.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            } else {
                // Loading state for carousel with missing children
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicLoadingIndicator()
                }
            }
        } else {
            ReelMediaItem(
                mediaUrl = post.media_url,
                thumbnailUrl = post.thumbnail_url,
                mediaType = post.media_type,
                isPlaying = isPlaying,
                onVideoError = onVideoError
            )
        }

        // 2. Gradient Overlay (Bottom 40% for better readability)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // 3. Metadata HUD (Premium Layout)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(0.85f) 
        ) {
            // Author Row with Glass Effect
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)) // Subtle backing
                    .padding(end = 12.dp)
            ) {
                val safeAuthor = post.author ?: "f1"
                val isOfficial = InstagramConstants.isOfficialAccount(safeAuthor)
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isOfficial) Color.White else Color(0xFFFFD700)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = safeAuthor.take(2).uppercase(),
                        fontFamily = michromaFont,
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Text(
                    text = "@$safeAuthor",
                    color = if (isOfficial) Color.White else Color(0xFFFFD700),
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(2f, 2f), 4f))
                )
                
                if (isOfficial) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF1DA1F2),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Caption
            post.caption?.let { caption ->
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(1f, 1f), 2f))
                )
            }
        }

        // 4. Right Side Actions (Glassmorphic Buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Open in Instagram
            GlassActionButton(
                icon = Icons.Default.MoreVert,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.permalink))
                    context.startActivity(intent)
                }
            )

            // Share
            GlassActionButton(
                icon = Icons.Default.Share,
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this F1 reel: ${post.permalink}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )
        }
    }
}

@Composable
fun ReelMediaItem(
    mediaUrl: String?,
    thumbnailUrl: String?,
    mediaType: String,
    isPlaying: Boolean,
    onVideoError: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // ExoPlayer State - using pool for efficient reuse
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    
    // Create/Release Player based on isPlaying and Lifecycle
    // Using ExoPlayerPool for efficient player reuse
    // NOTE: Removed isPlaying from keys to allow preloading when composed
    DisposableEffect(mediaUrl) {
        if (mediaType == "VIDEO" && mediaUrl != null) {
            val player = ExoPlayerPool.acquire(context)
            player.setMediaItem(MediaItem.fromUri(mediaUrl))
            player.prepare()
            // Don't play immediately if not active page, but do prepare
            player.playWhenReady = isPlaying 
            player.volume = 1f // Ensure volume is up
            player.repeatMode = Player.REPEAT_MODE_ONE
            
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isVideoReady = true
                        hasError = false
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("ReelsDebug", "Player Error for $mediaUrl: ${error.message}", error)
                    hasError = true
                    isVideoReady = false
                    onVideoError() // Trigger refresh of feed data
                }
            }
            player.addListener(listener)
            exoPlayer = player

            onDispose {
                player.removeListener(listener)
                ExoPlayerPool.release(player)
                exoPlayer = null
                isVideoReady = false
                hasError = false
            }
        } else {
            onDispose { 
                exoPlayer?.let { ExoPlayerPool.release(it) }
                exoPlayer = null
            }
        }
    }

    // React to isPlaying changes (swipe)
    LaunchedEffect(isPlaying, exoPlayer) {
        exoPlayer?.playWhenReady = isPlaying
        if (isPlaying && hasError) {
             // Try to prepare again if we come back to a failed video
             exoPlayer?.prepare()
        }
    }
    
    // Separate lifecycle handling to pause/resume
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Video Player Layer
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Changed to FIT
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // Handle buffering manually
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isVideoReady) 1f else 0f) // Fade in
            )
        }
        
        // Thumbnail / Loading State / Image Fallback
        if (!isVideoReady || hasError) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = thumbnailUrl ?: mediaUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit, // Match video fit
                    modifier = Modifier.fillMaxSize()
                )
                
                // Glassmorphic Loading Indicator (only if it's a video trying to load)
                if (mediaType == "VIDEO" && isPlaying && !hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassmorphicLoadingIndicator()
                    }
                }
                
                // Error Indicator
                if (hasError) {
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Video Failed to Load", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f)) // Dark glass
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

// GlassmorphicLoadingIndicator now imported from shared components
