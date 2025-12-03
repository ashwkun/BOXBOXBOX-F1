
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
            onClose = onClose
        )
    }
}

@Composable
fun ReelItem(
    post: InstagramPost,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // ExoPlayer State
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }
    
    // Create/Release Player based on isPlaying and Lifecycle
    DisposableEffect(context, isPlaying, lifecycleOwner) {
        if (isPlaying) {
            val player = ExoPlayer.Builder(context).build().apply {
                post.media_url?.let { url ->
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_ONE
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                isVideoReady = true
                            }
                        }
                    })
                }
            }
            exoPlayer = player
        }

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
            exoPlayer?.release()
            exoPlayer = null
            isVideoReady = false
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
        
        // Thumbnail / Loading State
        if (!isVideoReady) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = post.thumbnail_url ?: post.media_url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit, // Match video fit
                    modifier = Modifier.fillMaxSize()
                )
                
                // Glassmorphic Loading Indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicLoadingIndicator()
                }
            }
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

@Composable
fun GlassmorphicLoadingIndicator() {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = Color(0xFFFF0080),
            strokeWidth = 3.dp
        )
    }
}
