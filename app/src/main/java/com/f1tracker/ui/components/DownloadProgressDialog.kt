package com.f1tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.f1tracker.R

@Composable
fun DownloadProgressDialog(
    progress: Int,
    onDismiss: () -> Unit
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val accentColor = Color(0xFFFF0080)

    Dialog(
        onDismissRequest = {}, // Prevent dismissal during download
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A0A0F),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Text(
                    text = "DOWNLOADING UPDATE",
                    fontFamily = brigendsFont,
                    fontSize = 14.sp,
                    color = accentColor,
                    letterSpacing = 2.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Progress Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    // Background track
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 8.dp
                    )
                    
                    // Progress
                    CircularProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor,
                        strokeWidth = 8.dp
                    )
                    
                    // Percentage Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$progress%",
                            fontFamily = michromaFont,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Status Text
                Text(
                    text = if (progress >= 100) "Finalizing..." else "Please wait while we download the latest version...",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
