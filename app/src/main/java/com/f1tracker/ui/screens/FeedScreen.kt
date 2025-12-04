package com.f1tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.pager.HorizontalPager
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.f1tracker.R
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.models.Podcast
import com.f1tracker.data.models.PodcastEpisode
import com.f1tracker.data.models.F1Video
import com.f1tracker.ui.viewmodels.NewsViewModel
import com.f1tracker.ui.viewmodels.MultimediaViewModel
import com.f1tracker.ui.components.TabSelector
import com.f1tracker.ui.models.FeedItem
import com.f1tracker.ui.components.InstagramFeedList
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    newsViewModel: NewsViewModel = hiltViewModel(),
    multimediaViewModel: MultimediaViewModel = hiltViewModel(),
    onNewsClick: (String?) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onNavigateToReels: (String) -> Unit = {},
    onEpisodeClick: (PodcastEpisode) -> Unit = {},
    onPlayPause: () -> Unit = {},
    currentlyPlayingEpisode: PodcastEpisode? = null,
    isPlaying: Boolean = false,
    initialTab: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    refreshTrigger: Long = 0L
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    var isGameFullScreen by remember { mutableStateOf(false) }

    
    val newsArticles by newsViewModel.newsArticles.collectAsState()
    val isRefreshing by newsViewModel.isRefreshing.collectAsState()
    val youtubeVideos by multimediaViewModel.youtubeVideos.collectAsState()
    val podcasts by multimediaViewModel.podcasts.collectAsState()
    val instagramPosts by multimediaViewModel.instagramPosts.collectAsState()
    val selectedTabIndex by multimediaViewModel.selectedTabIndex.collectAsState()
    val selectedNewsFilter by newsViewModel.selectedFilter.collectAsState()
    val selectedVideoFilter by multimediaViewModel.selectedVideoFilter.collectAsState()
    
    // Hoisted Pager State for Instagram Feed
    val instagramPagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = multimediaViewModel.instagramScrollIndex
    ) {
        if (instagramPosts.isEmpty()) 3 else instagramPosts.size
    }

    // Sync Instagram scroll state to ViewModel
    LaunchedEffect(instagramPagerState.currentPage) {
        multimediaViewModel.updateInstagramScrollPosition(instagramPagerState.currentPage)
    }
    val isInstagramRefreshing by multimediaViewModel.isInstagramRefreshing.collectAsState()
    
    // Hoisted scroll state for NewsList
    val newsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = newsViewModel.newsScrollIndex,
        initialFirstVisibleItemScrollOffset = newsViewModel.newsScrollOffset
    )

    // Hoisted scroll state for LatestFeed
    val latestListState = rememberLazyListState(
        initialFirstVisibleItemIndex = multimediaViewModel.latestScrollIndex,
        initialFirstVisibleItemScrollOffset = multimediaViewModel.latestScrollOffset
    )

    // Hoisted scroll state for VideosList
    val videosListState = rememberLazyListState(
        initialFirstVisibleItemIndex = multimediaViewModel.videosScrollIndex,
        initialFirstVisibleItemScrollOffset = multimediaViewModel.videosScrollOffset
    )

    // Sync scroll state to ViewModel
    LaunchedEffect(newsListState.firstVisibleItemIndex, newsListState.firstVisibleItemScrollOffset) {
        newsViewModel.updateScrollPosition(
            newsListState.firstVisibleItemIndex,
            newsListState.firstVisibleItemScrollOffset
        )
    }

    LaunchedEffect(latestListState.firstVisibleItemIndex, latestListState.firstVisibleItemScrollOffset) {
        multimediaViewModel.updateLatestScrollPosition(
            latestListState.firstVisibleItemIndex,
            latestListState.firstVisibleItemScrollOffset
        )
    }

    LaunchedEffect(videosListState.firstVisibleItemIndex, videosListState.firstVisibleItemScrollOffset) {
        multimediaViewModel.updateVideosScrollPosition(
            videosListState.firstVisibleItemIndex,
            videosListState.firstVisibleItemScrollOffset
        )
    }
    
    val tabs = listOf(
        TabItem("LATEST", Icons.Default.FlashOn),
        TabItem("SOCIAL", Icons.Default.Public),
        TabItem("NEWS", Icons.Default.Article),
        TabItem("VIDEOS", Icons.Default.PlayCircle),
        TabItem("GAMES", Icons.Default.SportsEsports),
        TabItem("AUDIO", Icons.Default.Headphones)
    )
    // Initialize pager with the persisted state or initialTab if provided (though initialTab is mostly 0)
    // We prioritize the ViewModel state if it's not 0, otherwise we use initialTab
    val startPage = if (selectedTabIndex != 0) selectedTabIndex else initialTab
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = startPage) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    
    // State for selected podcast detail view
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    
    // Sync pager state with ViewModel
    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        multimediaViewModel.setSelectedTab(pagerState.currentPage)
        onTabChanged(pagerState.currentPage)
        
        // Reset scroll positions if switching away from tabs
        if (pagerState.currentPage != 0) { // Not Latest
            multimediaViewModel.resetLatestScrollPosition()
            latestListState.scrollToItem(0)
        }
        
        if (pagerState.currentPage != 2) { // Not News (Index 2)
            newsViewModel.resetScrollPosition()
            newsListState.scrollToItem(0)
        }
        
        if (pagerState.currentPage != 3) { // Not Videos (Index 3)
            multimediaViewModel.resetVideosScrollPosition()
            videosListState.scrollToItem(0)
        }
        
        // No specific scroll state reset needed for Game tab (Index 4) or Audio (Index 5) yet


    }

    // Handle Navbar Refresh Trigger
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            // Scroll to top and refresh current tab
            when (pagerState.currentPage) {
                0 -> latestListState.animateScrollToItem(0)
                1 -> {
                    instagramPagerState.animateScrollToPage(0)
                    multimediaViewModel.refreshInstagramFeed()
                }
                2 -> {
                    newsListState.animateScrollToItem(0)
                    newsViewModel.refreshNews()
                }
                3 -> videosListState.animateScrollToItem(0)
                // 4 (Game) and 5 (Audio) don't have scrollable lists to reset yet
            }
        }
    }
    
    // Scroll to initial tab only if it's explicitly requested and different from current
    LaunchedEffect(initialTab) {
        if (initialTab != 0 && initialTab != pagerState.currentPage) {
            pagerState.scrollToPage(initialTab)
        }
    }
    


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Header

            
            // Custom Tab Selector (Always Visible unless in Game Fullscreen)
            if (!isGameFullScreen) {
            Column {
                IconTabSelector(
                    tabs = tabs,
                    selectedTab = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            if (pagerState.currentPage == index) {
                                // Already on this tab: Scroll to top and refresh
                                when (index) {
                                    0 -> latestListState.animateScrollToItem(0)
                                    1 -> {
                                        instagramPagerState.animateScrollToPage(0)
                                        multimediaViewModel.refreshInstagramFeed()
                                    }
                                    2 -> {
                                        newsListState.animateScrollToItem(0)
                                        newsViewModel.refreshNews()
                                    }
                                    3 -> videosListState.animateScrollToItem(0)
                                    // 4 and 5 don't have scrollable lists yet
                                }
                            } else {
                                // Switch tab
                                if (kotlin.math.abs(pagerState.currentPage - index) > 1) {
                                    pagerState.scrollToPage(index)
                                } else {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
            
            // Content with Swipe Navigation
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1,
                userScrollEnabled = !isGameFullScreen
            ) { page ->
                when (page) {
                    0 -> {

                        LatestFeed(
                            newsArticles = newsArticles,
                            videos = youtubeVideos,
                            podcasts = podcasts,
                            michromaFont = michromaFont,
                            onNewsClick = onNewsClick,
                            onVideoClick = onVideoClick,
                            onEpisodeClick = onEpisodeClick,
                            onPlayPause = onPlayPause,
                            currentlyPlayingEpisode = currentlyPlayingEpisode,
                            isPlaying = isPlaying,
                            onExploreClick = { tabIndex ->
                                coroutineScope.launch { pagerState.animateScrollToPage(tabIndex) }
                            },
                            listState = latestListState,
                            instagramPosts = instagramPosts,
                            onNavigateToReels = onNavigateToReels
                        )
                    }
                    1 -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        InstagramFeedList(
                            posts = instagramPosts,
                            michromaFont = michromaFont,
                            onOpenInInstagram = { permalink ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(permalink))
                                context.startActivity(intent)
                            },
                            onNavigateToReels = onNavigateToReels,
                            onScrollDirectionChange = { /* No-op, tab is always visible */ },
                            pagerState = instagramPagerState,
                            isRefreshing = isInstagramRefreshing,
                            onRefresh = { multimediaViewModel.refreshInstagramFeed() }
                        )
                    }
                    2 -> NewsList(
                        articles = newsArticles, 
                        michromaFont = michromaFont, 
                        onNewsClick = onNewsClick, 
                        listState = newsListState,
                        isRefreshing = isRefreshing,
                        onRefresh = { newsViewModel.refreshNews() },
                        selectedFilter = selectedNewsFilter,
                        onFilterSelected = { newsViewModel.setSelectedFilter(it) }
                    )
                    3 -> VideosList(
                        videos = youtubeVideos, 
                        michromaFont = michromaFont, 
                        onVideoClick = onVideoClick,
                        listState = videosListState,
                        selectedFilter = selectedVideoFilter,
                        onFilterSelected = { multimediaViewModel.setSelectedVideoFilter(it) }
                    )

                    4 -> GameScreen(
                        michromaFont = michromaFont, 
                        brigendsFont = brigendsFont,
                        onFullScreenToggle = { isGameFullScreen = it }
                    )
                    5 -> PodcastsList(
                        podcasts = podcasts, 
                        michromaFont = michromaFont, 
                        currentlyPlayingEpisode = currentlyPlayingEpisode, 
                        isPlaying = isPlaying, 
                        onEpisodeClick = onEpisodeClick, 
                        onPlayPause = onPlayPause,
                        onPodcastClick = { podcast -> 
                            selectedPodcast = if (selectedPodcast?.name == podcast.name) null else podcast
                        },
                        selectedPodcast = selectedPodcast
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestFeed(
    newsArticles: List<NewsArticle>,
    videos: List<F1Video>,
    podcasts: List<Podcast>,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    onVideoClick: (String) -> Unit,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onExploreClick: (Int) -> Unit,
    instagramPosts: List<com.f1tracker.data.models.InstagramPost>,
    onNavigateToReels: (String) -> Unit,
    listState: LazyListState
) {
    val combinedItems = remember(newsArticles, videos, podcasts, instagramPosts) {
        val items = mutableListOf<FeedItem>()
        
        // Take most recent items from each content type to ensure diversity
        newsArticles
            .sortedByDescending { parseDate(it.published) }
            .take(3)
            .forEach { items.add(FeedItem.NewsItem(it)) }
        
        videos
            .sortedByDescending { parseDate(it.publishedDate) }
            .take(3)
            .forEach { items.add(FeedItem.VideoItem(it)) }
        
        instagramPosts
            .sortedByDescending { parseDate(it.timestamp) }
            .take(3)
            .forEach { items.add(FeedItem.InstagramItem(it)) }
        
        // For podcasts, take the most recent episode from each podcast
        podcasts.forEach { podcast ->
            podcast.episodes
                .sortedByDescending { parseDate(it.publishedDate) }
                .take(1)
                .forEach { episode ->
                    items.add(FeedItem.PodcastItem(episode))
                }
        }
        
        // Now sort all these items by published date and take top 30
        items.sortedByDescending { parseDate(it.publishedDate) }.take(30)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(combinedItems) { item ->
            when (item) {
                is FeedItem.NewsItem -> com.f1tracker.ui.components.NewsCard(
                    article = item.article,
                    onNewsClick = onNewsClick,
                    showTag = true,
                    modifier = Modifier.fillMaxWidth()
                )
                is FeedItem.VideoItem -> VideoCard(item.video, michromaFont, onVideoClick, showTag = true)
                is FeedItem.PodcastItem -> LargePodcastCard(
                    episode = item.episode,
                    isCurrentlyPlaying = currentlyPlayingEpisode?.audioUrl == item.episode.audioUrl,
                    isPlaying = isPlaying && currentlyPlayingEpisode?.audioUrl == item.episode.audioUrl,
                    michromaFont = michromaFont,
                    onEpisodeClick = onEpisodeClick,
                    onPlayPause = onPlayPause
                )
                is FeedItem.InstagramItem -> {
                    val isReel = item.post.media_type == "VIDEO"
                    LatestInstagramCard(
                        post = item.post,
                        michromaFont = michromaFont,
                        onClick = {
                            if (isReel) {
                                // Navigate to Reels screen for videos
                                onNavigateToReels(item.post.permalink)
                            } else {
                                // Navigate to Social tab for regular posts
                                onExploreClick(1) // Social tab is index 1
                            }
                        }
                    )
                }
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Explore more in the dedicated feeds",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): androidx.compose.ui.graphics.Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.DarkGray.copy(alpha = 0.6f),
            Color.DarkGray.copy(alpha = 0.2f),
            Color.DarkGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800), repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LatestInstagramCard(
    post: com.f1tracker.data.models.InstagramPost,
    michromaFont: FontFamily,
    onClick: () -> Unit
) {
    val isReel = post.media_type == "VIDEO"
    val isCarousel = post.media_type == "CAROUSEL_ALBUM" && !post.children?.data.isNullOrEmpty()
    
    Card(
        modifier = if (isReel || isCarousel) {
            Modifier
                .fillMaxWidth()
                .height(600.dp)
        } else {
            Modifier.fillMaxWidth()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = if (isReel || isCarousel) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        ) {
            // Header: User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeAuthor = post.author ?: "f1"
                val isOfficial = com.f1tracker.util.InstagramConstants.isOfficialAccount(safeAuthor)
                
                // Avatar
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
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF1DA1F2),
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Tag in top right
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SOCIAL",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Media Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isReel || isCarousel) Modifier.weight(1f) else Modifier)
                    .background(if (isReel || isCarousel) Color.Black else Color.Transparent)
            ) {
                if (isCarousel) {
                    val children = post.children!!.data
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { children.size })
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val child = children[page]
                        FeedMediaItem(
                            mediaUrl = child.media_url,
                            thumbnailUrl = child.thumbnail_url,
                            mediaType = child.media_type,
                            isReel = true, // Treat carousel items as full height
                            onClick = onClick
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
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                } else {
                    FeedMediaItem(
                        mediaUrl = post.media_url,
                        thumbnailUrl = post.thumbnail_url,
                        mediaType = post.media_type,
                        isReel = isReel,
                        onClick = onClick
                    )
                }
            }
            
            // Caption
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

@Composable
fun FeedMediaItem(
    mediaUrl: String?,
    thumbnailUrl: String?,
    mediaType: String,
    isReel: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) {
        val imageUrl = if (mediaType == "VIDEO") {
            thumbnailUrl ?: mediaUrl
        } else {
            mediaUrl
        }
        
        if (imageUrl != null) {
            coil.compose.SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = if (isReel) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                },
                contentScale = if (isReel) ContentScale.Fit else ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush())
                    )
                }
            )
        }
        
        // Video Player logic
        if (mediaType == "VIDEO") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            val isMuted = remember { mutableStateOf(true) }
            val isVideoReady = remember { mutableStateOf(false) }
            val showVideoPlayer = remember { mutableStateOf(true) }
            
            if (showVideoPlayer.value && mediaUrl != null) {
                val exoPlayer = remember(showVideoPlayer.value) {
                    androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                        setMediaItem(androidx.media3.common.MediaItem.fromUri(mediaUrl))
                        prepare()
                        playWhenReady = true
                        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                        volume = if (isMuted.value) 0f else 1f
                        
                        addListener(object : androidx.media3.common.Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                                    isVideoReady.value = true
                                }
                            }
                        })
                    }
                }
                
                LaunchedEffect(isMuted.value) {
                    exoPlayer.volume = if (isMuted.value) 0f else 1f
                }
                
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        exoPlayer.stop()
                        exoPlayer.release()
                    }
                }
                
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isVideoReady.value) 1f else 0f)
                )
                
                if (!isVideoReady.value) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        com.f1tracker.ui.components.GlassmorphicLoadingIndicator()
                    }
                }
                
                // Mute toggle button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isMuted.value = !isMuted.value }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted.value) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Toggle Sound",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseDate(dateString: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try RFC 1123 (common for RSS/Podcasts)
            ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e2: Exception) {
            ZonedDateTime.now().minusYears(1) // Fallback
        }
    }
}

@Composable
private fun LargePodcastCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isCurrentlyPlaying) onPlayPause() else onEpisodeClick(episode)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            // 200dp Height Image Area with Blurred Background + Square Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Blurred Background (Zoomed in)
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.7f)) // Dark overlay
                        },
                    contentScale = ContentScale.Crop
                )
                
                // Content Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Square Artwork with Play Button
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = episode.imageUrl,
                            contentDescription = episode.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Play Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isCurrentlyPlaying) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFFF0080), Color(0xFFE6007E))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                                            )
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isCurrentlyPlaying && isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Right side info (optional, or just keep it clean)
                    // For now, we'll just keep the artwork prominent in this section
                }

                // Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PODCAST",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Duration badge
                if (episode.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPublishedDate(episode.publishedDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NewsList(
    articles: List<NewsArticle>,
    michromaFont: FontFamily,
    onNewsClick: (String?) -> Unit,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    val tabs = listOf("ALL", "HEADLINES", "PADDOCK", "EXTRAS")
    
    val filteredArticles = remember(articles, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> articles
            "HEADLINES" -> articles.filter { 
                val cat = NewsCategorizer.categorize(it.headline)
                cat == NewsCategory.NUCLEAR || cat == NewsCategory.MAJOR
            }
            "PADDOCK" -> articles.filter { 
                NewsCategorizer.categorize(it.headline) == NewsCategory.PADDOCK 
            }
            "EXTRAS" -> articles.filter { 
                val cat = NewsCategorizer.categorize(it.headline)
                cat == NewsCategory.OTHERS || cat == NewsCategory.HEADLINES
            }
            else -> articles
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedFilter == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { onFilterSelected(tab) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tab,
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            items(filteredArticles) { article ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    com.f1tracker.ui.components.NewsCard(
                        article = article,
                        onNewsClick = onNewsClick,
                        showTag = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.Black,
            contentColor = Color(0xFFFF0080)
        )
    }
}

@Composable
private fun VideosList(
    videos: List<F1Video>,
    michromaFont: FontFamily,
    onVideoClick: (String) -> Unit,
    listState: LazyListState,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    // Determine available filters based on content
    val availableFilters = remember(videos) {
        val filters = mutableListOf("All", "Top")
        
        if (videos.any { it.title.contains("highlight", ignoreCase = true) }) {
            filters.add("Highlights")
        }
        if (videos.any { it.title.contains("react", ignoreCase = true) || it.title.contains("reaction", ignoreCase = true) }) {
            filters.add("Reactions")
        }
        filters
    }
    
    // Filter and sort videos
    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            "All" -> videos // Default order (latest)
            "Top" -> videos.sortedByDescending { it.viewCount }
            "Highlights" -> videos.filter { it.title.contains("highlight", ignoreCase = true) }
            "Reactions" -> videos.filter { it.title.contains("react", ignoreCase = true) || it.title.contains("reaction", ignoreCase = true) }
            else -> videos
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp), // Bottom padding for list
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Chips Row (Fixed, no scrolling)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                availableFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFF0080) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onFilterSelected(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp) // Smaller padding
                    ) {
                        Text(
                            text = filter.uppercase(),
                            fontFamily = michromaFont,
                            fontSize = 9.sp, // Smaller font
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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

@Composable
private fun PodcastsList(
    podcasts: List<Podcast>,
    michromaFont: FontFamily,
    currentlyPlayingEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    selectedPodcast: Podcast?
) {
    // Use the new PodcastsSection component which handles the layout internally
    // We wrap it in a LazyColumn to ensure it scrolls if content exceeds screen height
    // although PodcastsSection uses LazyRows internally, the main page structure expects a composable here.
    // Since PodcastsSection is a Column, we can put it inside a LazyColumn item or just use a Column with verticalScroll
    // But the parent `HorizontalPager` expects a page content.
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            com.f1tracker.ui.components.PodcastsSection(
                podcasts = podcasts,
                currentlyPlayingEpisode = currentlyPlayingEpisode,
                isPlaying = isPlaying,
                onEpisodeClick = onEpisodeClick,
                onPlayPause = onPlayPause,
                onViewMoreClick = { /* TODO: Handle view more */ },
                onPodcastClick = onPodcastClick,
                selectedPodcast = selectedPodcast
            )
        }
        
        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}



@Composable
private fun VideoCard(
    video: F1Video,
    michromaFont: FontFamily,
    onVideoClick: (String) -> Unit,
    showTag: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick(video.videoId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column {
            Box(modifier = Modifier.height(200.dp)) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Duration badge
                if (video.duration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (showTag) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color(0xFFFF0080), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "VIDEO",
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = video.title,
                    fontFamily = michromaFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.views,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPublishedDate(video.publishedDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}



@Composable
private fun EpisodeCard(
    episode: PodcastEpisode,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    michromaFont: FontFamily,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentlyPlaying) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C1C2E),
                            Color(0xFF1C1315)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E),
                            Color(0xFF131315)
                        )
                    )
                }
            )
            .clickable {
                if (isCurrentlyPlaying) {
                    onPlayPause()
                } else {
                    onEpisodeClick(episode)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Episode artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isCurrentlyPlaying) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF0080),
                                            Color(0xFFE6007E)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2C2C2E),
                                            Color(0xFF1C1C1E)
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isCurrentlyPlaying && isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Episode info
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = episode.title,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = if (isCurrentlyPlaying) Color(0xFFFF0080) else Color.White,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
                
                if (episode.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = episode.duration,
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPublishedDate(publishedString: String): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val dateTime = java.time.ZonedDateTime.parse(publishedString, formatter)
        val now = java.time.ZonedDateTime.now()
        
        val hours = java.time.Duration.between(dateTime, now).toHours()
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            hours < 48 -> "Yesterday"
            else -> {
                val days = hours / 24
                "${days}d ago"
            }
        }
    } catch (e: Exception) {
        "Recently"
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun IconTabSelector(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(if (isSelected) 2.5f else 1f)
                    .fillMaxHeight()
                    .animateContentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            Color(0xFFFF0080).copy(alpha = 0.02f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = Color(0xFFFF0080).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) Color(0xFFFF0080) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.label,
                            fontFamily = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal)),
                            fontSize = 8.sp,
                            color = Color.White,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}



