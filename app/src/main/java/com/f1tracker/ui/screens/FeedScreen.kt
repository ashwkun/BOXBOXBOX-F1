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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
// Feed module imports
import com.f1tracker.ui.screens.feed.tabs.VideosTab
import com.f1tracker.ui.screens.feed.tabs.NewsTab
import com.f1tracker.ui.screens.feed.tabs.PodcastsTab
import com.f1tracker.ui.screens.feed.tabs.LatestTab

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
    refreshTrigger: Long = 0L,
    startPermalink: String? = null
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

    // Scroll to specific post if requested
    LaunchedEffect(startPermalink, instagramPosts) {
        if (startPermalink != null && instagramPosts.isNotEmpty()) {
            val index = instagramPosts.indexOfFirst { it.permalink == startPermalink }
            if (index != -1) {
                instagramPagerState.scrollToPage(index)
            }
        }
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
    // Initialize pager with the persisted state or initialTab if provided
    // We prioritize the explicit initialTab (navigation) over ViewModel state
    val startPage = if (initialTab != 0) initialTab else selectedTabIndex
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

                        LatestTab(
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
                            instagramPosts = instagramPosts,
                            onNavigateToReels = onNavigateToReels,
                            onSocialPostClick = { permalink ->
                                // Find the post index and scroll to it in Social tab
                                val postIndex = instagramPosts.indexOfFirst { it.permalink == permalink }
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1) // Go to Social tab
                                    if (postIndex >= 0) {
                                        instagramPagerState.animateScrollToPage(postIndex)
                                    }
                                }
                            }
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
                    2 -> NewsTab(
                        articles = newsArticles, 
                        michromaFont = michromaFont, 
                        onNewsClick = onNewsClick, 
                        listState = newsListState,
                        isRefreshing = isRefreshing,
                        onRefresh = { newsViewModel.refreshNews() },
                        selectedFilter = selectedNewsFilter,
                        onFilterSelected = { newsViewModel.setSelectedFilter(it) }
                    )
                    3 -> VideosTab(
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
                    5 -> PodcastsTab(
                        podcasts = podcasts, 
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



