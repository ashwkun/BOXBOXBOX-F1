package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import kotlinx.coroutines.delay
import kotlin.random.Random

data class LiveDriver(
    val driverId: String,
    val position: Int,
    val gap: String,
    val teamColor: Color
)

@Composable
fun LiveScreen() {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    // Initialize drivers with mock data
    var drivers by remember { mutableStateOf(generateMockDrivers()) }
    
    // Animate position changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // Update every 3 seconds
            drivers = updateDriverPositions(drivers)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Coming Soon Banner - Improved styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE TIMING",
                        fontFamily = brigendsFont,
                        fontSize = 14.sp,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Preview Mode",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Coming Soon Badge
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFE6007E).copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE6007E).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "COMING SOON",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color(0xFFE6007E),
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.05f))
        )
        
        // Header Row - Improved spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "POS",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(35.dp)
            )
            Text(
                text = "DRIVER",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "GAP",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(70.dp)
            )
        }
        
        // Driver List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = drivers,
                key = { _, driver -> driver.driverId }
            ) { _, driver ->
                AnimatedDriverRow(
                    driver = driver,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont
                )
            }
            
            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AnimatedDriverRow(
    driver: LiveDriver,
    brigendsFont: FontFamily,
    michromaFont: FontFamily
) {
    val driverInfo = F1DataProvider.getDriverByApiId(driver.driverId)
    val teamInfo = driverInfo?.let { F1DataProvider.getTeamByApiId(it.team) }
    
    // Animate gap changes
    var displayGap by remember { mutableStateOf(driver.gap) }
    
    LaunchedEffect(driver.gap) {
        displayGap = driver.gap
    }
    
    // Position indicator color
    val positionColor = when (driver.position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFFE6007E) // Pink accent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF0F0F0F),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position with colored indicator
        Row(
            modifier = Modifier.width(35.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                positionColor.copy(alpha = 0.8f),
                                positionColor.copy(alpha = 0.3f)
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${driver.position}",
                fontFamily = michromaFont,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Driver Info with team color accent
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle team color indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        driver.teamColor.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = driver.teamColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column {
                Text(
                    text = driverInfo?.fullName?.uppercase() ?: driver.driverId.uppercase(),
                    fontFamily = brigendsFont,
                    fontSize = 11.sp,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = teamInfo?.name?.uppercase() ?: "UNKNOWN TEAM",
                    fontFamily = michromaFont,
                    fontSize = 7.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        // Gap with improved styling
        Box(
            modifier = Modifier
                .width(70.dp)
                .background(
                    if (driver.position == 1) 
                        Color(0xFFFFD700).copy(alpha = 0.1f) 
                    else 
                        Color.White.copy(alpha = 0.03f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = displayGap,
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = if (driver.position == 1) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

private fun generateMockDrivers(): List<LiveDriver> {
    val driverIds = listOf(
        "max_verstappen",
        "perez",
        "leclerc",
        "sainz",
        "hamilton",
        "russell",
        "norris",
        "piastri",
        "alonso",
        "stroll",
        "gasly",
        "ocon",
        "albon",
        "sargeant",
        "bottas",
        "zhou",
        "hulkenberg",
        "magnussen",
        "tsunoda",
        "ricciardo"
    )
    
    return driverIds.mapIndexed { index, driverId ->
        val driverInfo = F1DataProvider.getDriverByApiId(driverId)
        val teamInfo = driverInfo?.let { F1DataProvider.getTeamByApiId(it.team) }
        val teamColor = teamInfo?.color?.let { 
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                Color(0xFFFF0080)
            }
        } ?: Color(0xFFFF0080)
        
        LiveDriver(
            driverId = driverId,
            position = index + 1,
            gap = if (index == 0) "LEADER" else "+${String.format("%.3f", (index * 2.5) + Random.nextDouble(-0.5, 0.5))}",
            teamColor = teamColor
        )
    }
}

private fun updateDriverPositions(currentDrivers: List<LiveDriver>): List<LiveDriver> {
    val mutableDrivers = currentDrivers.toMutableList()
    
    // Randomly swap 2-3 positions
    val swapCount = Random.nextInt(1, 3)
    repeat(swapCount) {
        if (mutableDrivers.size > 1) {
            // Don't swap position 1 too often
            val idx1 = Random.nextInt(2, mutableDrivers.size.coerceAtLeast(3))
            val idx2 = (idx1 + Random.nextInt(1, 3)).coerceAtMost(mutableDrivers.size - 1)
            
            if (idx1 < mutableDrivers.size && idx2 < mutableDrivers.size && idx1 != idx2) {
                val temp = mutableDrivers[idx1]
                mutableDrivers[idx1] = mutableDrivers[idx2]
                mutableDrivers[idx2] = temp
            }
        }
    }
    
    // Update positions and gaps
    return mutableDrivers.mapIndexed { index, driver ->
        driver.copy(
            position = index + 1,
            gap = if (index == 0) "LEADER" else {
                val baseGap = index * 2.5
                val variation = Random.nextDouble(-0.3, 0.3)
                "+${String.format("%.3f", baseGap + variation)}"
            }
        )
    }
}
