package com.f1tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.ui.components.AnimatedHeader
import com.f1tracker.ui.components.BottomNavBar
import com.f1tracker.ui.components.NavDestination
import com.f1tracker.R

@Composable
fun MainAppScreen() {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var selectedRace by remember { mutableStateOf<com.f1tracker.data.models.Race?>(null) }
    
    // Show Race Detail if race is selected
    if (selectedRace != null) {
        RaceDetailScreen(
            race = selectedRace!!,
            onBackClick = { selectedRace = null }
        )
    } else if (webViewUrl != null) {
        // Show WebView if URL is set
        WebViewScreen(
            url = webViewUrl!!,
            onBackClick = { webViewUrl = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top header (reduced height inside the composable)
            AnimatedHeader()

            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentDestination) {
                    NavDestination.HOME -> HomeScreen(
                        onNewsClick = { url -> webViewUrl = url },
                        onNavigateToNews = { currentDestination = NavDestination.NEWS }
                    )
                    NavDestination.SCHEDULE -> ScheduleScreen()
                    NavDestination.LIVE -> LiveScreen()
                    NavDestination.STANDINGS -> StandingsScreen()
                    NavDestination.NEWS -> NewsScreen(
                        onNewsClick = { url -> webViewUrl = url }
                    )
                }
            }

            // Fixed bottom navigation bar
            BottomNavBar(
                currentDestination = currentDestination,
                onNavigate = { destination -> currentDestination = destination }
            )
        }
    }
}

@Composable
private fun PlaceholderContent(
    title: String,
    message: String
) {
    val michromaFont = FontFamily(
        Font(R.font.michroma, FontWeight.Normal)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(),
            fontFamily = michromaFont,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontFamily = michromaFont,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

