package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.Race
import com.f1tracker.data.models.RaceResult
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun LastRaceCard(
    race: Race?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded))
    val michromaFont = FontFamily(Font(R.font.michroma))
    
    Box(
        modifier = modifier
            .width(340.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() } // Make clickable
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161616),
                        Color(0xFF000000)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (race == null || race.results == null) {
            // Loading or no data
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Loading...",
                    fontFamily = michromaFont,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        } else {
            // Main layout with top strip spanning entire width
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top strip with "RESULTS • RACE NAME" and FLAG aligned to right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "RESULTS",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            
                            Text(
                                text = "•",
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            
                            Text(
                                text = race.raceName.uppercase(),
                                fontFamily = michromaFont,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp)
                            )
                        }
                        // Arrow Indicator
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
            // Main card divided into 3 columns - driver images fill each column
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                race.results.take(3).forEachIndexed { index, result ->
                    DriverColumn(
                        result = result,
                        position = index + 1,
                        michromaFont = michromaFont,
                        brigendsFont = brigendsFont,
                        modifier = Modifier.weight(1f)
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverColumn(
    result: RaceResult,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RectangleShape) // Clip to prevent overflow into adjacent columns
    ) {
        // Bold team color background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(teamColor.copy(alpha = 0.35f)) // Solid bold team color
        )
        
        // Driver number background image
        if (driverInfo?.headshotNumberUrl != null) {
            AsyncImage(
                model = driverInfo.headshotNumberUrl,
                contentDescription = "Driver number",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(0.8f) // Zoom out the number to 80%
                    .alpha(0.4f), // Semi-transparent number
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        }
        
        // Driver image fills the entire column (2x zoom, no top clipping)
        if (driverInfo?.headshotF1 != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape), // Ensure image stays within column bounds
                contentAlignment = Alignment.TopCenter
            ) {
                AsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = result.driver.familyName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(2.2f) // Increased scale
                        .offset(y = 40.dp), // Adjusted offset
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter // Top edge aligned, no top clipping
                )
            }
        }
        
        // Content overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Position number overlay - positioned bottom left, above the bottom strip
                        Text(
                            text = position.toString(),
                            fontFamily = michromaFont,
                fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = when (position) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> Color.White
                },
                    modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 42.dp) // Shifted down slightly
            )
            
            // Bottom strip: Driver code, team logo, and team abbreviation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Driver short code
                    Text(
                        text = result.driver.code,
                        fontFamily = brigendsFont,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Team abbreviation only
                        Text(
                            text = teamInfo?.abbreviation ?: "",
                            fontFamily = michromaFont,
                        fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                }
            }
        }
    }
}

@Composable
private fun DriverCard(
    result: RaceResult,
    position: Int,
    michromaFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background with team color gradient and pattern
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            teamColor.copy(alpha = 0.25f),
                            teamColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Diagonal stripes pattern overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.02f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Position badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when (position) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            // Driver headshot (fills the entire card like the example)
            if (driverInfo?.headshotF1 != null) {
                AsyncImage(
                    model = driverInfo.headshotF1,
                    contentDescription = result.driver.familyName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter // Show upper body and face
                )
            }
            
            // Driver info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Driver code
                Text(
                    text = result.driver.code,
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                // Team logo
                if (teamInfo?.symbolUrl != null) {
                    AsyncImage(
                        model = teamInfo.symbolUrl,
                        contentDescription = teamInfo.displayName,
                        modifier = Modifier
                            .height(12.dp)
                            .padding(top = 2.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverResultRow(
    result: RaceResult,
    michromaFont: FontFamily
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position and Driver
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Position badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (result.position) {
                            "1" -> Color(0xFFFFD700) // Gold
                            "2" -> Color(0xFFC0C0C0) // Silver
                            "3" -> Color(0xFFCD7F32) // Bronze
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = result.position,
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.position.toInt() <= 3) Color.Black else Color.White
                )
            }
            
            // Driver headshot (cropped to show only upper part)
            if (driverInfo?.headshotF1 != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    AsyncImage(
                        model = driverInfo.headshotF1,
                        contentDescription = "${result.driver.givenName} ${result.driver.familyName}",
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = (-8).dp), // Offset to show upper part only
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Driver name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${result.driver.givenName} ${result.driver.familyName}",
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
        
        // Team logo (instead of colored circle)
        if (teamInfo?.symbolUrl != null) {
            AsyncImage(
                model = teamInfo.symbolUrl,
                contentDescription = teamInfo.displayName,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback to colored circle
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getTeamColor(result.constructor.constructorId))
            )
        }
    }
}

@Composable
private fun getTeamColor(constructorId: String): Color {
    // Read color from F1DataProvider (loaded from JSON)
    val colorHex = F1DataProvider.getTeamColorHex(constructorId)
    return if (colorHex != null) {
        try {
            Color(android.graphics.Color.parseColor("#$colorHex"))
        } catch (e: Exception) {
            Color.Gray
        }
    } else {
        Color.Gray
    }
}

private fun getCountryFlag(country: String): String {
    return when (country.lowercase()) {
        "bahrain" -> "🇧🇭"
        "saudi arabia" -> "🇸🇦"
        "australia" -> "🇦🇺"
        "japan" -> "🇯🇵"
        "china" -> "🇨🇳"
        "usa", "united states" -> "🇺🇸"
        "italy" -> "🇮🇹"
        "monaco" -> "🇲🇨"
        "spain" -> "🇪🇸"
        "canada" -> "🇨🇦"
        "austria" -> "🇦🇹"
        "uk", "united kingdom", "great britain" -> "🇬🇧"
        "hungary" -> "🇭🇺"
        "belgium" -> "🇧🇪"
        "netherlands" -> "🇳🇱"
        "azerbaijan" -> "🇦🇿"
        "singapore" -> "🇸🇬"
        "mexico" -> "🇲🇽"
        "brazil" -> "🇧🇷"
        "uae", "united arab emirates" -> "🇦🇪"
        "qatar" -> "🇶🇦"
        else -> "🏁"
    }
}

private fun getCountryCodeForFlag(country: String): String {
    return when (country.lowercase()) {
        "bahrain" -> "bh"
        "saudi arabia" -> "sa"
        "australia" -> "au"
        "japan" -> "jp"
        "china" -> "cn"
        "usa", "united states" -> "us"
        "italy" -> "it"
        "monaco" -> "mc"
        "spain" -> "es"
        "canada" -> "ca"
        "austria" -> "at"
        "uk", "united kingdom", "great britain" -> "gb"
        "hungary" -> "hu"
        "belgium" -> "be"
        "netherlands" -> "nl"
        "azerbaijan" -> "az"
        "singapore" -> "sg"
        "mexico" -> "mx"
        "brazil" -> "br"
        "uae", "united arab emirates" -> "ae"
        "qatar" -> "qa"
        else -> "un" // Generic flag for unknown
    }
}

