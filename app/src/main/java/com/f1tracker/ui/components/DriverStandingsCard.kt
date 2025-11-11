package com.f1tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.DriverStanding

@Composable
fun DriverStandingsCard(
    standings: List<DriverStanding>?,
    modifier: Modifier = Modifier
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded))
    val michromaFont = FontFamily(Font(R.font.michroma))
    
    Box(
        modifier = modifier
            .width(340.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (standings == null || standings.isEmpty()) {
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
                // Top strip with "DRIVER STANDINGS" spanning entire card width
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "DRIVER STANDINGS",
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                
                // Main card divided into 3 columns - driver images fill each column
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    standings.take(3).forEachIndexed { index, standing ->
                        DriverStandingColumn(
                            standing = standing,
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
private fun DriverStandingColumn(
    standing: DriverStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val driverInfo = F1DataProvider.getDriverByApiId(standing.driver.driverId)
    val teamInfo = standing.constructors.firstOrNull()?.let { 
        F1DataProvider.getTeamByApiId(it.constructorId) 
    }
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
                    contentDescription = standing.driver.familyName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(2f) // 2x zoom maintained
                        .offset(y = 36.dp), // Adjusted to 36dp
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter // Top edge aligned, no top clipping
                )
            }
        }
        
        // Content overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Points overlay - positioned bottom left, above the bottom strip
            Text(
                text = standing.points,
                style = TextStyle(
                    fontFamily = michromaFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 42.dp) // Shifted down slightly
            )
            
            // Bottom strip: Driver code and team abbreviation only
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
                        text = standing.driver.code,
                        fontFamily = brigendsFont,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Team abbreviation
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

