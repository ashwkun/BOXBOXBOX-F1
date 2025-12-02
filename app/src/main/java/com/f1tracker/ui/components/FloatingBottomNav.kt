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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class NavDestination {
    HOME, SCHEDULE, LIVE, STANDINGS, FEED
}

@Composable
fun FloatingBottomNav(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // New premium liquid-glass pill
        Box(
            modifier = Modifier
                .width(232.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(26.dp))
                // Soft ambient shadow
                .drawBehind {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.28f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f, 26f),
                        alpha = 0.28f
                    )
                }
                .background(
                    brush = Brush.verticalGradient( // glass body
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f), // top
                            Color.White.copy(alpha = 0.06f), // middle
                            Color.White.copy(alpha = 0.10f)  // bottom
                        )
                    )
                )
                // Inner border highlight
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f, 26f),
                        style = Stroke(width = 1.0f)
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavSlot(
                    selected = currentDestination == NavDestination.SCHEDULE,
                    onClick = { onNavigate(NavDestination.SCHEDULE) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (currentDestination == NavDestination.SCHEDULE) Color.White else Color.White.copy(alpha = 0.55f)
                    )
                }
                NavSlot(
                    selected = currentDestination == NavDestination.LIVE,
                    onClick = { onNavigate(NavDestination.LIVE) }
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = if (currentDestination == NavDestination.LIVE) Color.White else Color.White.copy(alpha = 0.55f)
                    )
                }
                NavSlot(
                    selected = currentDestination == NavDestination.STANDINGS,
                    onClick = { onNavigate(NavDestination.STANDINGS) }
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (currentDestination == NavDestination.STANDINGS) Color.White else Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavSlot(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "slotScale"
    )
    Box(
        modifier = Modifier
            .width(70.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // circular light plate when active
        if (selected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
            )
        }
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            content()
        }
        // dot indicator
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            )
        }
    }
}

// End new navbar

