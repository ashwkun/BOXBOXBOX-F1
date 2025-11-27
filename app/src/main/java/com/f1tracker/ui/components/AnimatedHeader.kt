package com.f1tracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R

/**
 * Animated BOX BOX·BOX header - left aligned, no card
 * Matches exact design: White BOX, Pink BOX, dot, White BOX
 * Using Brigends Expanded font
 */
@Composable
fun AnimatedHeader(
    isUpdateAvailable: Boolean = false,
    onUpdateClick: () -> Unit = {}
) {
    // Load custom font
    val brigendsFont = FontFamily(
        Font(R.font.brigends_expanded, FontWeight.Normal)
    )
    
    // Pulse animation for the dot only
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    // Shimmer animation for update card
    val shimmerTranslateAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by shimmerTranslateAnim.animateFloat(
        initialValue = -100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black)
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isUpdateAvailable) {
            // Update Available State - Glassmorphic Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable { onUpdateClick() }
                    .padding(horizontal = 16.dp)
            ) {
                // BOX (Pink)
                Text(
                    text = "BOX",
                    fontSize = 24.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF0080), // Hot pink
                    letterSpacing = 0.sp
                )
                
                // Dot separator (Pink)
                Text(
                    text = "·",
                    fontSize = 24.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF0080), // Pink dot
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Glassmorphic Card for "UPDATE AVAILABLE"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f)) // Glass effect
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Shimmer effect overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(shimmerTranslate - 100f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(shimmerTranslate, 0f)
                                )
                            )
                    )
                    
                    Text(
                        text = "UPDATE AVAILABLE",
                        fontSize = 12.sp,
                        fontFamily = brigendsFont,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            // Standard Logo State
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // First BOX (White)
                Text(
                    text = "BOX",
                    fontSize = 28.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Second BOX (Pink/Magenta)
                Text(
                    text = "BOX",
                    fontSize = 28.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF0080), // Hot pink/magenta
                    letterSpacing = 0.sp
                )
                
                // Dot separator (Pink, pulsing)
                Text(
                    text = "·",
                    fontSize = 28.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF0080).copy(alpha = dotPulse), // Pulsing pink dot
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Third BOX (White)
                Text(
                    text = "BOX",
                    fontSize = 28.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

