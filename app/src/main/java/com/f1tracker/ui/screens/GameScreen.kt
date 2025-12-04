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
    michromaFont: FontFamily
) {
    var currentView by remember { mutableStateOf(GameView.LIST) }

    // Handle system back button
    BackHandler(enabled = currentView != GameView.LIST) {
        currentView = GameView.LIST
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (currentView) {
            GameView.LIST -> GameList(
                michromaFont = michromaFont,
                onGameClick = { gameId ->
                    if (gameId == "hotlap") {
                        currentView = GameView.HOTLAP
                    }
                }
            )
            GameView.HOTLAP -> HotlapGame(
                michromaFont = michromaFont,
                onBack = { currentView = GameView.LIST }
            )
        }
    }
}

@Composable
fun GameList(
    michromaFont: FontFamily,
    onGameClick: (String) -> Unit
) {
    val games = listOf(
        GameItem(
            id = "hotlap",
            title = "HOTLAP",
            description = "Test your reaction times on famous F1 corners.",
            icon = Icons.Default.Timer,
            color = Color(0xFFFF0000), // Red
            isAvailable = true
        ),
        GameItem(
            id = "strategy",
            title = "STRATEGY MASTER",
            description = "Call the shots. Win the race. (Coming Soon)",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFF00D2BE), // Mercedes Teal
            isAvailable = false
        ),
        GameItem(
            id = "pitstop",
            title = "PIT STOP PRO",
            description = "The perfect stop requires perfect timing. (Coming Soon)",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFFFF8700), // McLaren Orange
            isAvailable = false
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "F1 ARCADE",
                fontFamily = michromaFont,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(games) { game ->
            GameCard(
                game = game,
                michromaFont = michromaFont,
                onClick = { onGameClick(game.id) }
            )
        }
    }
}

@Composable
fun GameCard(
    game: GameItem,
    michromaFont: FontFamily,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(enabled = game.isAvailable) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                game.color.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(game.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = game.icon,
                        contentDescription = null,
                        tint = game.color,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        fontFamily = michromaFont,
                        fontSize = 18.sp,
                        color = if (game.isAvailable) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = game.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
            
            // "Play" or "Soon" Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        if (game.isAvailable) game.color else Color.DarkGray,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (game.isAvailable) "PLAY" else "SOON",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.Black,
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
