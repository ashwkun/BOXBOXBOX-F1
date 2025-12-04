package com.f1tracker.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HotlapGame(
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Game Header (Internal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "HOTLAP",
                fontFamily = brigendsFont, // Using Brigends as requested
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Game Content Placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color(0xFF1A0000) // Subtle red tint at bottom
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFFFF0000),
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "GAME LOADING...",
                    fontFamily = brigendsFont,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PREPARE YOUR REFLEXES",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    letterSpacing = 4.sp
                )
            }
        }
    }
}
