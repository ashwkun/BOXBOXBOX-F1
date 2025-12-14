package com.f1tracker.ui.screens.feed.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Creates a shimmer effect brush for loading states.
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.DarkGray.copy(alpha = 0.6f),
            Color.DarkGray.copy(alpha = 0.2f),
            Color.DarkGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800), repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Parses a date string to ZonedDateTime, supporting ISO and RFC formats.
 */
fun parseDate(dateString: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try RFC 1123 (common for RSS/Podcasts)
            ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e2: Exception) {
            ZonedDateTime.now().minusYears(1) // Fallback
        }
    }
}

/**
 * Formats a published date string to a human-readable relative time.
 */
fun formatPublishedDate(publishedString: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dateTime = ZonedDateTime.parse(publishedString, formatter)
        val now = ZonedDateTime.now()
        
        val hours = Duration.between(dateTime, now).toHours()
        when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            hours < 48 -> "Yesterday"
            else -> {
                val days = hours / 24
                "${days}d ago"
            }
        }
    } catch (e: Exception) {
        "Recently"
    }
}
