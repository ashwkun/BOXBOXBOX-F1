package com.f1tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R

@Composable
fun BottomNavBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit
) {
    // Load Brigends font
    val brigendsFont = FontFamily(
        Font(R.font.brigends_expanded, FontWeight.Normal)
    )
    
    // Pink accent from header
    val accentColor = Color(0xFFFF0080)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        // Very subtle top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Pure pitch black background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (1st)
            NavItemWithLabel(
                icon = Icons.Default.Home,
                label = "HOME",
                brigendsFont = brigendsFont,
                isSelected = currentDestination == NavDestination.HOME,
                accentColor = accentColor,
                onClick = { onNavigate(NavDestination.HOME) }
            )
            
            // Schedule (2nd)
            NavItemWithLabel(
                icon = Icons.Default.CalendarMonth,
                label = "SCHEDULE",
                brigendsFont = brigendsFont,
                isSelected = currentDestination == NavDestination.SCHEDULE,
                accentColor = accentColor,
                onClick = { onNavigate(NavDestination.SCHEDULE) }
            )
            
            // Feed (3rd - formerly Social)
            NavItemFeed(
                brigendsFont = brigendsFont,
                isSelected = currentDestination == NavDestination.SOCIAL,
                accentColor = accentColor,
                onClick = { onNavigate(NavDestination.SOCIAL) }
            )
            
            // Standings (4th)
            NavItemWithLabel(
                icon = Icons.Default.EmojiEvents,
                label = "STANDINGS",
                brigendsFont = brigendsFont,
                isSelected = currentDestination == NavDestination.STANDINGS,
                accentColor = accentColor,
                onClick = { onNavigate(NavDestination.STANDINGS) }
            )
            
            // Live (5th - Swapped with Social and restyled)
            NavItemWithLabel(
                icon = Icons.Default.LiveTv,
                label = "LIVE",
                brigendsFont = brigendsFont,
                isSelected = currentDestination == NavDestination.LIVE,
                accentColor = accentColor,
                onClick = { onNavigate(NavDestination.LIVE) }
            )
        }
    }
}

@Composable
private fun NavItemWithLabel(
    icon: ImageVector,
    label: String,
    brigendsFont: FontFamily,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    // Pulse animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Column(
        modifier = Modifier
            .width(72.dp) // Narrower for 5 items
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp) // Slightly smaller for 5 items
        ) {
            // Neon glow behind icon (active only)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .blur(20.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.5f * glowPulse),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // Icon only - no background circle
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp), // Slightly smaller for 5 items
                tint = if (isSelected) accentColor else Color(0xFF6B6B6B)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label text
        Text(
            text = label,
            fontFamily = brigendsFont,
            fontSize = 8.sp,
            color = if (isSelected) accentColor else Color(0xFF6B6B6B),
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Neon underglow line (active only)
        Box(
            modifier = Modifier
                .width(if (isSelected) 28.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = glowPulse),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        )
    }
}

@Composable
private fun NavItemFeed(
    brigendsFont: FontFamily,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    // Pulse animation for dot
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
    
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Column(
        modifier = Modifier
            .width(72.dp) // Narrower for 5 items
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp) // Slightly smaller for 5 items
        ) {
            // Neon glow behind text (active only)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .blur(20.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.5f * glowPulse),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // FEED text with dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FEED",
                    fontFamily = brigendsFont,
                    fontSize = 14.sp, // Slightly smaller
                    color = if (isSelected) accentColor else Color(0xFF6B6B6B),
                    letterSpacing = 0.8.sp
                )
                
                Spacer(modifier = Modifier.width(3.dp))
                
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(5.dp) // Slightly smaller
                        .clip(CircleShape)
                        .background(
                            if (isSelected) accentColor.copy(alpha = dotPulse) else Color(0xFF6B6B6B)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Neon underglow line (active only)
        Box(
            modifier = Modifier
                .width(if (isSelected) 28.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = glowPulse),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        )
    }
}


