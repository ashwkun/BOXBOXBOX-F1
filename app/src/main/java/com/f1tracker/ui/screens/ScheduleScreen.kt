package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.Race
import com.f1tracker.ui.viewmodels.RaceViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    viewModel: RaceViewModel = hiltViewModel(),
    onRaceClick: (Race) -> Unit = {}
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))

    val upcomingRaces by viewModel.upcomingRaces.collectAsState()
    val completedRaces by viewModel.completedRaces.collectAsState()
    
    // Pager state for swipe navigation
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = selectedTab) { 2 }
    
    // Sync external selectedTab with Pager
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    
    // Sync Pager change to external onTabChange
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            onTabChange(pagerState.currentPage)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tab Selector
        com.f1tracker.ui.components.TabSelector(
            tabs = listOf("UPCOMING", "COMPLETED"),
            selectedTab = selectedTab,
            onTabSelected = { index ->
                onTabChange(index)
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        
        // Horizontal Pager for swipeable tabs
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val displayRaces = if (page == 0) upcomingRaces else completedRaces
            
            if (displayRaces.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (page == 0) "No upcoming races" else "No completed races",
                        fontFamily = michromaFont,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayRaces) { race ->
                        if (page == 0) {
                            UpcomingRaceCard(
                                race = race,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont,
                                onClick = { onRaceClick(race) }
                            )
                        } else {
                            // Use CompletedRaceCard for completed races (if available) or fallback to UpcomingRaceCard
                            // For now reusing UpcomingRaceCard but logic might differ
                            UpcomingRaceCard(
                                race = race,
                                michromaFont = michromaFont,
                                brigendsFont = brigendsFont,
                                onClick = { onRaceClick(race) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingRaceCard(
    race: Race,
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onClick: () -> Unit
) {
    // Calculate time until race
    val nowUTC = LocalDateTime.now(ZoneId.of("UTC"))
    val raceDateTime = try {
        LocalDateTime.parse("${race.date}T${race.time}", DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        null
    }

    val daysUntil = raceDateTime?.let { ChronoUnit.DAYS.between(nowUTC, it) } ?: 0
    val isCompleted = raceDateTime?.isBefore(nowUTC) ?: false

    // Get flag colors
    val flagColors = getFlagColors(race.circuit.location.country)

    // Check if sprint weekend
    val isSprint = race.sprint != null

    // Calculate race week dates
    val firstSessionDate = race.firstPractice?.date
        ?: race.sprintQualifying?.date
        ?: race.qualifying?.date
        ?: race.date

    val weekDatesText = try {
        val start = LocalDateTime.parse("${firstSessionDate}T00:00:00")
        val end = LocalDateTime.parse("${race.date}T00:00:00")
        val startDay = start.format(DateTimeFormatter.ofPattern("dd"))
        val endDay = end.format(DateTimeFormatter.ofPattern("dd"))
        val month = end.format(DateTimeFormatter.ofPattern("MMM"))
        "$startDay-$endDay $month"
    } catch (e: Exception) {
        ""
    }

    val raceTimeIST = formatTimeIST(race.date, race.time)
    val accent = Color(0xFFFF0080)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        flagColors.first.copy(alpha = 0.35f),
                        flagColors.second.copy(alpha = 0.25f),
                        Color(0xFF0B0B0B)
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1200f
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top: Round, Sprint, Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R${race.round}",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    
                    if (isSprint) {
                        Text(
                            text = "SPRINT",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier
                                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                
                AsyncImage(
                    model = "https://flagcdn.com/w80/${getCountryCode(race.circuit.location.country)}.png",
                    contentDescription = race.circuit.location.country,
                    modifier = Modifier
                        .width(36.dp)
                        .height(27.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Race name
            Text(
                text = race.raceName.uppercase(),
                fontFamily = brigendsFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp,
                lineHeight = 17.sp,
                maxLines = 2
            )

            // Country • Circuit
            Text(
                text = "${race.circuit.location.country.uppercase()} • ${race.circuit.circuitName}",
                fontFamily = michromaFont,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Bottom: Time info or Podium
            if (isCompleted && race.results?.isNotEmpty() == true) {
                // Show podium for completed races
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    race.results?.take(3)?.forEachIndexed { index, result ->
                        val driverInfo = com.f1tracker.data.local.F1DataProvider.getDriverByApiId(result.driver.driverId)
                        val position = index + 1
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Position
                                Text(
                                    text = "P$position",
                                    fontFamily = michromaFont,
                                    fontSize = 8.sp,
                                    color = when (position) {
                                        1 -> Color(0xFFFFD700)
                                        2 -> Color(0xFFC0C0C0)
                                        3 -> Color(0xFFCD7F32)
                                        else -> Color.White
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Driver code
                                Text(
                                    text = result.driver.code,
                                    fontFamily = brigendsFont,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (!isCompleted) {
                // Show time info for upcoming races
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = getTimeUntilText(daysUntil),
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            letterSpacing = 0.3.sp
                        )
                        if (weekDatesText.isNotEmpty()) {
                            Text(
                                text = weekDatesText,
                                fontFamily = michromaFont,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "RACE TIME",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = raceTimeIST.ifEmpty { race.time },
                            fontFamily = michromaFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}


// Helper functions
private fun getTimeUntilText(days: Long): String {
    return when {
        days == 0L -> "TODAY"
        days == 1L -> "TOMORROW"
        days < 30 -> "IN $days DAYS"
        else -> "IN ${days / 30} MONTHS"
    }
}

private fun getFlagColors(country: String): Pair<Color, Color> {
    return when (country.lowercase()) {
        "australia" -> Pair(Color(0xFF00008B), Color(0xFFFFD700))
        "bahrain" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "china" -> Pair(Color(0xFFDE2910), Color(0xFFFFDE00))
        "monaco" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "spain" -> Pair(Color(0xFFC60B1E), Color(0xFFFFC400))
        "canada" -> Pair(Color(0xFFFF0000), Color(0xFFFFFFFF))
        "austria" -> Pair(Color(0xFFED2939), Color(0xFFFFFFFF))
        "uk", "great britain" -> Pair(Color(0xFF012169), Color(0xFFC8102E))
        "hungary" -> Pair(Color(0xFFCD2A3E), Color(0xFF436F4D))
        "belgium" -> Pair(Color(0xFF000000), Color(0xFFFDDA24))
        "italy" -> Pair(Color(0xFF009246), Color(0xFFCE2B37))
        "singapore" -> Pair(Color(0xFFEF3340), Color(0xFFFFFFFF))
        "japan" -> Pair(Color(0xFFBC002D), Color(0xFFFFFFFF))
        "usa", "united states" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        "mexico" -> Pair(Color(0xFF006847), Color(0xFFCE1126))
        "brazil" -> Pair(Color(0xFF009B3A), Color(0xFFFEDF00))
        "qatar" -> Pair(Color(0xFF8A1538), Color(0xFFFFFFFF))
        "uae", "abu dhabi" -> Pair(Color(0xFFFF0000), Color(0xFF00732F))
        "saudi arabia" -> Pair(Color(0xFF165B33), Color(0xFFFFFFFF))
        "netherlands" -> Pair(Color(0xFF21468B), Color(0xFFAE1C28))
        "azerbaijan" -> Pair(Color(0xFF00B5E2), Color(0xFFEF3340))
        "miami" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        else -> Pair(Color(0xFF1A0033), Color(0xFFFF0080))
    }
}

private fun getCircuitImage(circuitId: String): Int {
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

private fun getCountryCode(country: String): String {
    return when (country.lowercase()) {
        "australia" -> "au"
        "bahrain" -> "bh"
        "china" -> "cn"
        "monaco" -> "mc"
        "spain" -> "es"
        "canada" -> "ca"
        "austria" -> "at"
        "uk", "great britain" -> "gb"
        "hungary" -> "hu"
        "belgium" -> "be"
        "italy" -> "it"
        "singapore" -> "sg"
        "japan" -> "jp"
        "usa", "united states" -> "us"
        "mexico" -> "mx"
        "brazil" -> "br"
        "qatar" -> "qa"
        "uae", "abu dhabi" -> "ae"
        "saudi arabia" -> "sa"
        "netherlands" -> "nl"
        "azerbaijan" -> "az"
        else -> "un"
    }
}

private fun formatTimeIST(dateString: String, timeString: String): String {
    return try {
        val dateTimeString = "${dateString}T${timeString}"
        val utcDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        val istDateTime = utcDateTime.atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))

        val hour = istDateTime.hour
        val minute = istDateTime.minute

        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        String.format("%02d:%02d %s", displayHour, minute, period)
    } catch (e: Exception) {
        "TIME ERROR"
    }
}
