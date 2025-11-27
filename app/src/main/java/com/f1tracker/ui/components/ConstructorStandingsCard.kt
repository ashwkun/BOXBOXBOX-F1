package com.f1tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.ConstructorStanding

@Composable
fun ConstructorStandingsCard(
    standings: List<ConstructorStanding>?,
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
                // Top strip with "CONSTRUCTOR STANDINGS" spanning entire card width
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONSTRUCTOR STANDINGS",
                            fontFamily = michromaFont,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        // Arrow Indicator
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Main card divided into 3 rows - team cars fill each row
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    standings.take(3).forEachIndexed { index, standing ->
                        ConstructorRow(
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
private fun ConstructorRow(
    standing: ConstructorStanding,
    position: Int,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val teamInfo = F1DataProvider.getTeamByApiId(standing.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp) // Will be sized by weight
    ) {
        // Bold team color background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(teamColor.copy(alpha = 0.35f))
        )
        
        // TOP LAYER: All content together - car image, logo, and points
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Car image on the left (progressive clipping based on position)
            teamInfo?.carImageUrl?.let { carUrl ->
                // Position 1: less clipped (-100dp), Position 2: medium (-140dp), Position 3: more clipped (-180dp)
                val xOffset = when(position) {
                    1 -> (-100).dp
                    2 -> (-140).dp
                    else -> (-180).dp
                }
                
                AsyncImage(
                    model = carUrl,
                    contentDescription = "${teamInfo.displayName} car",
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .offset(x = xOffset),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.CenterStart
                )
            }
            
            // Logo and points centered/right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer for left side (where car image is)
                Spacer(modifier = Modifier.weight(0.3f))
                
                // Center: Big team logo from local folder (bigger size)
                val logoResource = getTeamBigLogo(standing.constructor.constructorId)
                if (logoResource != null) {
                    Image(
                        painter = painterResource(id = logoResource),
                        contentDescription = teamInfo?.displayName,
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Right side: Points
                Text(
                    text = standing.points,
                    fontFamily = michromaFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun getTeamBigLogo(constructorId: String): Int? {
    return when (constructorId) {
        "mclaren" -> R.drawable.mclaren
        "mercedes" -> R.drawable.mercedes
        "red_bull" -> R.drawable.red_bull
        "ferrari" -> R.drawable.ferrari
        "williams" -> R.drawable.williams
        "rb" -> R.drawable.racing_bulls
        "aston_martin" -> R.drawable.aston_martin
        "haas" -> R.drawable.haas
        "sauber" -> R.drawable.kick_sauber
        "alpine" -> R.drawable.alpine
        else -> null
    }
}

