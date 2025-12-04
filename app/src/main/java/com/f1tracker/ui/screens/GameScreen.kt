package com.f1tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.f1tracker.ui.screens.games.HotlapGame

enum class GameView {
    LIST,
    HOTLAP
}

@Composable
fun GameScreen(
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onFullScreenToggle: (Boolean) -> Unit
) {
    var currentView by remember { mutableStateOf(GameView.LIST) }

    // Handle system back button
    BackHandler(enabled = currentView != GameView.LIST) {
        currentView = GameView.LIST
        onFullScreenToggle(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (currentView) {
            GameView.LIST -> GameList(
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                onGameClick = { gameId ->
                    if (gameId == "hotlap") {
                        currentView = GameView.HOTLAP
                        onFullScreenToggle(true)
                    }
                }
            )
            GameView.HOTLAP -> HotlapGame(
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                onBack = { 
                    currentView = GameView.LIST
                    onFullScreenToggle(false)
                }
            )
        }
    }
}

@Composable
fun GameList(
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onGameClick: (String) -> Unit
) {
    val games = listOf(
        GameItem(
            id = "hotlap",
            title = "HOTLAP",
            description = "TEST YOUR REFLEXES",
            icon = Icons.Default.Timer,
            color = Color(0xFFFF0000), // Red
            isAvailable = true
        ),
        GameItem(
            id = "strategy",
            title = "STRATEGY MASTER",
            description = "CALL THE SHOTS",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFF00D2BE), // Mercedes Teal
            isAvailable = false
        ),
        GameItem(
            id = "pitstop",
            title = "PIT STOP PRO",
            description = "PERFECT TIMING",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFFFF8700), // McLaren Orange
            isAvailable = false
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Removed "F1 ARCADE" header as requested

        items(games) { game ->
            GameCard(
                game = game,
                michromaFont = michromaFont,
                brigendsFont = brigendsFont,
                onClick = { onGameClick(game.id) }
            )
        }
    }
}

@Composable
fun GameCard(
    game: GameItem,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = game.isAvailable) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                game.color.copy(alpha = 0.15f),
                                Color.Black
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                        )
                    )
            )
            
            // Accent Line
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(game.color)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Icon(
                    imageVector = game.icon,
                    contentDescription = null,
                    tint = game.color,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = game.title,
                    fontFamily = brigendsFont,
                    fontSize = 24.sp,
                    color = if (game.isAvailable) Color.White else Color.Gray,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = game.description,
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = if (game.isAvailable) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }
            
            // "Play" or "Soon" Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(
                        if (game.isAvailable) game.color.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (game.isAvailable) game.color else Color.Gray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (game.isAvailable) "PLAY" else "SOON",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = if (game.isAvailable) game.color else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class GameItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isAvailable: Boolean
)
