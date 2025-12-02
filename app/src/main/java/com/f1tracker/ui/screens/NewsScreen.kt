package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.ui.viewmodels.NewsViewModel
import com.f1tracker.util.NewsCategorizer
import com.f1tracker.util.NewsCategory

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onNewsClick: (String?) -> Unit = {}
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    val newsArticles by viewModel.newsArticles.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("ALL NEWS", "HEADLINES", "PADDOCK", "EXTRAS")
    
    LaunchedEffect(newsArticles) {
        android.util.Log.d("NewsScreen", "Loaded ${newsArticles.size} articles")
        newsArticles.forEach { 
            val cat = NewsCategorizer.categorize(it.headline)
            android.util.Log.d("NewsScreen", "Article: ${it.headline} -> $cat")
        }
    }
    
    val filteredArticles = remember(newsArticles, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> newsArticles // ALL
            1 -> newsArticles.filter { // HEADLINES (Nuclear + Major)
                val cat = NewsCategorizer.categorize(it.headline)
                cat == NewsCategory.NUCLEAR || cat == NewsCategory.MAJOR
            }
            2 -> newsArticles.filter { // PADDOCK
                NewsCategorizer.categorize(it.headline) == NewsCategory.PADDOCK
            }
            3 -> newsArticles.filter { // EXTRAS (Others + Standard Headlines)
                val cat = NewsCategorizer.categorize(it.headline)
                cat == NewsCategory.OTHERS || cat == NewsCategory.HEADLINES
            }
            else -> newsArticles
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Text(
            text = "F1 NEWS",
            fontFamily = brigendsFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(20.dp)
        )
        
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFFFF0080)
                )
            },
            divider = {
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontFamily = michromaFont,
                            fontSize = 10.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Color(0xFFFF0080) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }
        
        // News List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredArticles) { article ->
                com.f1tracker.ui.components.NewsCard(
                    article = article,
                    onNewsClick = onNewsClick,
                    showTag = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


