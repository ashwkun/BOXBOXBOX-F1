package com.f1tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
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
            description = "REFLEX TEST",
            icon = Icons.Default.Timer,
            color = Color(0xFFFF0000), // Red
            imageRes = R.drawable.game_hotlap,
            isAvailable = true
        ),
        GameItem(
            id = "strategy",
            title = "STRATEGY",
            description = "CALL THE SHOTS",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFF00D2BE), // Mercedes Teal
            imageRes = R.drawable.game_strategy,
            isAvailable = false
        ),
        GameItem(
            id = "pitstop",
            title = "PIT STOP",
            description = "PERFECT TIMING",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFFFF8700), // McLaren Orange
            imageRes = R.drawable.game_pitstop,
            isAvailable = false
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            .height(220.dp) // Taller for grid
            .clickable(enabled = game.isAvailable) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            Image(
                painter = painterResource(id = game.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay (Darker at bottom for text)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 100f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Title
                Text(
                    text = game.title,
                    fontFamily = brigendsFont,
                    fontSize = 16.sp,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description
                Text(
                    text = game.description,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = game.color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // "Play" or "Soon" Badge (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        if (game.isAvailable) game.color else Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (game.isAvailable) "PLAY" else "SOON",
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = if (game.isAvailable) Color.Black else Color.White,
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
    val imageRes: Int,
    val isAvailable: Boolean
)
