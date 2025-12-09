package com.f1tracker.ui.screens.feed.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NewsTab(
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
                                .background(if (isSelected) Color(0xFFFF0080) else Color(0xFF1A1A1A))
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
