package com.f1tracker.ui.screens.feed.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.ui.screens.feed.components.InstagramFeedList

/**
 * Social tab wrapper that displays Instagram feed content.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialTab(
    posts: List<InstagramPost>,
    michromaFont: FontFamily,
    onOpenInInstagram: (String) -> Unit,
    onNavigateToReels: (String) -> Unit,
    onScrollDirectionChange: (Boolean) -> Unit,
    pagerState: PagerState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    InstagramFeedList(
        posts = posts,
        michromaFont = michromaFont,
        onOpenInInstagram = onOpenInInstagram,
        onNavigateToReels = onNavigateToReels,
        onScrollDirectionChange = onScrollDirectionChange,
        pagerState = pagerState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    )
}
