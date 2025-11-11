package com.f1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.Race
import com.f1tracker.ui.components.AnimatedHeader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RaceDetailScreen(
    race: Race
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        AnimatedHeader()
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Race Header Card
            item {
                RaceHeaderCard(race, michromaFont, brigendsFont)
            }
            
            // Results
            if (race.results != null && race.results.isNotEmpty()) {
                item {
                    Text(
                        text = "RACE RESULTS",
                        fontFamily = brigendsFont,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                itemsIndexed(race.results) { index, result ->
                    ResultCard(result, index + 1, michromaFont)
                }
            }
        }
    }
}

@Composable
private fun RaceHeaderCard(
    race: Race,
    michromaFont: FontFamily,
    brigendsFont: FontFamily
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Circuit background
            val circuitImage = getCircuitImageForDetail(race.circuit.circuitId)
            AsyncImage(
                model = circuitImage,
                contentDescription = race.circuit.circuitName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Round
                Text(
                    text = "ROUND ${race.round}",
                    fontFamily = michromaFont,
                    fontSize = 9.sp,
                    color = Color(0xFFFF0080),
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Race Name
                Text(
                    text = race.raceName,
                    fontFamily = michromaFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Location
                Text(
                    text = "${race.circuit.location.locality}, ${race.circuit.location.country}",
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Circuit
                Text(
                    text = race.circuit.circuitName,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date
                Text(
                    text = formatDateDetail(race.date),
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: com.f1tracker.data.models.RaceResult,
    position: Int,
    michromaFont: FontFamily
) {
    val driverInfo = F1DataProvider.getDriverByApiId(result.driver.driverId)
    val teamInfo = F1DataProvider.getTeamByApiId(result.constructor.constructorId)
    val teamColor = teamInfo?.color?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: Color.Gray
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(teamColor.copy(alpha = 0.08f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Position
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (position <= 3) Color(0xFFFF0080) else teamColor.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        fontFamily = michromaFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Driver headshot
                driverInfo?.headshotF1?.let { headshot ->
                    AsyncImage(
                        model = headshot,
                        contentDescription = result.driver.code,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(teamColor.copy(alpha = 0.2f)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Driver info
                Column {
                    Text(
                        text = result.driver.code,
                        fontFamily = michromaFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = teamInfo?.abbreviation ?: result.constructor.name,
                        fontFamily = michromaFont,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Points
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = result.points,
                    fontFamily = michromaFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF0080)
                )
                Text(
                    text = "PTS",
                    fontFamily = michromaFont,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun getCircuitImageForDetail(circuitId: String): Int {
    return when (circuitId) {
        "albert_park" -> R.drawable.australia_circuit
        "bahrain" -> R.drawable.bahrain_circuit
        "shanghai" -> R.drawable.china_circuit
        "monaco" -> R.drawable.monaco_circuit
        "catalunya" -> R.drawable.spain_circuit
        "villeneuve" -> R.drawable.canada_circuit
        "red_bull_ring" -> R.drawable.austria_circuit
        "silverstone" -> R.drawable.great_britain_circuit
        "hungaroring" -> R.drawable.hungary_circuit
        "spa" -> R.drawable.belgium_circuit
        "monza" -> R.drawable.italy_circuit
        "marina_bay" -> R.drawable.singapore_circuit
        "suzuka" -> R.drawable.japan_circuit
        "americas" -> R.drawable.usa_circuit
        "rodriguez" -> R.drawable.mexico_circuit
        "interlagos" -> R.drawable.brazil_circuit
        "vegas" -> R.drawable.las_vegas_circuit
        "losail" -> R.drawable.qatar_circuit
        "yas_marina" -> R.drawable.abu_dhabi_circuit
        "jeddah" -> R.drawable.saudi_arabia_circuit
        "miami" -> R.drawable.miami_circuit
        "imola" -> R.drawable.emilia_romagna_circuit
        "baku" -> R.drawable.azerbaijan_circuit
        "zandvoort" -> R.drawable.netherlands_circuit
        else -> R.drawable.bahrain_circuit
    }
}

private fun formatDateDetail(dateString: String): String {
    return try {
        val date = LocalDateTime.parse("${dateString}T00:00:00")
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

