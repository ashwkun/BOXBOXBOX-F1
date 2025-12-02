
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
import androidx.compose.foundation.clickable
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
    onClose: () -> Unit
) {
    val reels by viewModel.reels.collectAsState()
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    if (reels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading Reels...", color = Color.White, fontFamily = michromaFont)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
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
    
    // Create/Release Player based on isPlaying and Lifecycle
    DisposableEffect(context, isPlaying, lifecycleOwner) {
        if (isPlaying) {
            val player = ExoPlayer.Builder(context).build().apply {
                post.media_url?.let { url ->
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_ONE
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
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Thumbnail Placeholder
            AsyncImage(
                model = post.thumbnail_url ?: post.media_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Gradient Overlay (Bottom 30%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        // 3. Metadata HUD (Minimal)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 20.dp) // Space for nav bar
                .fillMaxWidth(0.8f) // Leave space for right-side buttons
        ) {
            // Author Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val safeAuthor = post.author ?: "f1"
                val isOfficial = InstagramConstants.isOfficialAccount(safeAuthor)
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
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
                    color = if (isOfficial) Color.White else Color(0xFFFFD700),
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(2f, 2f), 4f))
                )
                
                if (isOfficial) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF1DA1F2),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Caption
            post.caption?.let { caption ->
                Text(
                    text = caption,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(1f, 1f), 2f))
                )
            }
        }

        // 4. Right Side Actions (Minimal Icons Only)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Open in Instagram
            Icon(
                imageVector = Icons.Default.MoreVert, 
                contentDescription = "Open",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.permalink))
                        context.startActivity(intent)
                    }
            )

            // Share
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
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

        // 5. Close Button (Top Left)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = Color.White,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(32.dp)
                .clickable { onClose() }
                .align(Alignment.TopStart)
        )
    }
}
