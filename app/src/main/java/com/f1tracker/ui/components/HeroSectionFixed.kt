package com.f1tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f1tracker.R
import com.f1tracker.data.models.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HeroSectionFixed(
    state: RaceWeekendState,
    getCountdown: (LocalDateTime) -> String,
    modifier: Modifier = Modifier,
    onRaceClick: (Race) -> Unit = {}
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val accentColor = Color(0xFFFF0080)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        when (state) {
            is RaceWeekendState.ComingUp -> {
                ComingUpHeroFixed(
                    state = state,
                    getCountdown = getCountdown,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont,
                    accentColor = accentColor,
                    onRaceClick = onRaceClick
                )
            }
            is RaceWeekendState.Active -> {
                ActiveWeekendHeroFixed(
                    state = state,
                    getCountdown = getCountdown,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont,
                    accentColor = accentColor,
                    onRaceClick = onRaceClick
                )
            }
            is RaceWeekendState.Loading -> {
                LoadingHeroFixed(brigendsFont = brigendsFont)
            }
            is RaceWeekendState.Error -> {
                ErrorHeroFixed(message = state.message, brigendsFont = brigendsFont)
            }
        }
    }
}

@Composable
private fun ComingUpHeroFixed(
    state: RaceWeekendState.ComingUp,
    getCountdown: (LocalDateTime) -> String,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color,
    onRaceClick: (Race) -> Unit
) {
    val targetDateTime = parseISTDateTimeFixed(state.nextMainEvent.date, state.nextMainEvent.time)
    var countdown by remember { mutableStateOf("") }
    
    LaunchedEffect(targetDateTime) {
        while (true) {
            countdown = getCountdown(targetDateTime)
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val countryCode = getCountryCodeFixed(state.race.circuit.location.country)
    val flagUrl = "https://flagcdn.com/w320/$countryCode.png"
    val flagColors = getFlagColorsFixed(state.race.circuit.location.country)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        flagColors.first.copy(alpha = 0.5f),
                        flagColors.second.copy(alpha = 0.35f),
                        Color(0xFF0B0B0B)
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1200f
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onRaceClick(state.race) }
    ) {
        // Circuit track layout as background (very subtle)
        val circuitDrawable = getCircuitDrawableById(state.race.circuit.circuitId)
        AsyncImage(
            model = circuitDrawable,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.02f),
            contentScale = ContentScale.Crop
        )
        
        // Subtle dark overlay for text readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF000000).copy(alpha = 0.3f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp), // Reduced padding
            verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing
        ) {
            // Race Header
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = flagUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp, 36.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = state.race.raceName.uppercase(),
                        fontFamily = brigendsFont,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatDateRange(state.race),
                        fontFamily = michromaFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = state.race.circuit.circuitName,
                        fontFamily = michromaFont,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Divider
            DividerFixed()
            
            // Next Main Event Section - Vertically Stacked
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
            ) {
                // 1. NEXT MAIN EVENT header
                Text(
                    text = "NEXT MAIN EVENT",
                    fontFamily = brigendsFont,
                    fontSize = 10.sp,
                    color = accentColor.copy(alpha = 0.8f),
                    letterSpacing = 2.sp
                )
                
                // 2. Event Name (QUALIFYING)
                Text(
                    text = state.nextMainEventType.displayName().uppercase(),
                    fontFamily = brigendsFont,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                // 3. Countdown
                CountdownBoxFixed(
                    countdown = countdown,
                    michromaFont = michromaFont,
                    accentColor = accentColor
                )
                
                // 4. Date & Time - Now below countdown
                Text(
                    text = formatFullDateTime(targetDateTime),
                    fontFamily = michromaFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                // 5. Weather
                val nextEventWeather = state.upcomingEvents.find { it.sessionType == state.nextMainEventType }?.weather
                if (nextEventWeather != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalWeatherIcon(
                            weatherIcon = nextEventWeather.weatherIcon,
                            size = 16.dp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${nextEventWeather.temperature}°C",
                            fontFamily = michromaFont,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = "Rain",
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${nextEventWeather.rainChance}%",
                            fontFamily = michromaFont,
                            fontSize = 11.sp,
                            color = Color(0xFF4FC3F7)
                        )
                    }
                }
            }
            
            // Divider
            DividerFixed()
            
            // Weekend Schedule - Horizontal Scroll with Priority
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
            ) {
                Text(
                    text = "WEEKEND SCHEDULE",
                    fontFamily = brigendsFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                
                HorizontalScrollableSchedule(
                    events = state.upcomingEvents.filter { !it.sessionType.isMainEvent() || it.sessionType != state.nextMainEventType },
                    michromaFont = michromaFont,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun ActiveWeekendHeroFixed(
    state: RaceWeekendState.Active,
    getCountdown: (LocalDateTime) -> String,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color,
    onRaceClick: (Race) -> Unit
) {
    val countryCode = getCountryCodeFixed(state.race.circuit.location.country)
    val flagUrl = "https://flagcdn.com/w320/$countryCode.png"
    
    var countdown by remember { mutableStateOf("") }
    
    LaunchedEffect(state.currentEvent?.endsAt) {
        while (true) {
            state.currentEvent?.endsAt?.let {
                countdown = getCountdown(it)
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onRaceClick(state.race) }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(150.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF0D0D0D)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Race Header
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = flagUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp, 36.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = state.race.raceName.uppercase(),
                        fontFamily = brigendsFont,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = state.race.circuit.circuitName,
                        fontFamily = michromaFont,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Live Event
            if (state.currentEvent != null) {
                LiveBannerFixed(
                    event = state.currentEvent,
                    countdown = countdown,
                    brigendsFont = brigendsFont,
                    michromaFont = michromaFont,
                    accentColor = accentColor
                )
            }
            
            // Completed
            if (state.completedEvents.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMPLETED",
                        fontFamily = brigendsFont,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    
                    state.completedEvents.forEach { completed ->
                        CompletedCard(
                            event = completed,
                            brigendsFont = brigendsFont,
                            michromaFont = michromaFont,
                            accentColor = accentColor
                        )
                    }
                }
            }
            
            // Upcoming
            if (state.upcomingEvents.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "UPCOMING",
                        fontFamily = brigendsFont,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    
                    HorizontalScrollableSchedule(
                        events = state.upcomingEvents,
                        michromaFont = michromaFont,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownBoxFixed(
    countdown: String,
    michromaFont: FontFamily,
    accentColor: Color
) {
    val parts = countdown.split(" ")
    val timeUnits = mutableListOf<Pair<String, String>>()
    
    parts.forEach { part ->
        when {
            part.endsWith("d") -> timeUnits.add(part.removeSuffix("d") to "D")
            part.endsWith("h") -> timeUnits.add(part.removeSuffix("h") to "H")
            part.endsWith("m") -> timeUnits.add(part.removeSuffix("m") to "M")
            part.endsWith("s") -> timeUnits.add(part.removeSuffix("s") to "S")
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        timeUnits.forEach { (value, unit) ->
            TimeCardFixed(
                value = value,
                unit = unit,
                michromaFont = michromaFont,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun TimeCardFixed(
    value: String,
    unit: String,
    michromaFont: FontFamily,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = value.padStart(2, '0'),
            fontFamily = michromaFont,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = unit,
            fontFamily = michromaFont,
            fontSize = 8.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
        )
    }
}

@Composable
private fun HorizontalScrollableSchedule(
    events: List<UpcomingEvent>,
    michromaFont: FontFamily,
    accentColor: Color
) {
    // Sort by priority
    val sortedEvents = events.sortedBy { it.sessionType.priority() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortedEvents.forEach { event ->
            PrioritySessionCard(
                event = event,
                michromaFont = michromaFont,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun PrioritySessionCard(
    event: UpcomingEvent,
    michromaFont: FontFamily,
    accentColor: Color
) {
    val eventDateTime = parseISTDateTimeFixed(event.sessionInfo.date, event.sessionInfo.time)
    val isHighPriority = event.sessionType.isMainEvent()
    
    // Wide card with 2 rows only
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighPriority) 
                    accentColor.copy(alpha = 0.12f)
                else 
                    Color.White.copy(alpha = 0.05f)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Session name and time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.sessionType.displayName(),
                fontFamily = michromaFont,
                fontSize = 13.sp,
                fontWeight = if (isHighPriority) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighPriority) accentColor else Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDayTime(eventDateTime),
                fontFamily = michromaFont,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        // Row 2: Weather info
        if (event.weather != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MinimalWeatherIcon(
                    weatherIcon = event.weather.weatherIcon,
                    size = 16.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${event.weather.temperature}°C",
                    fontFamily = michromaFont,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Rain",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${event.weather.rainChance}%",
                        fontFamily = michromaFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4FC3F7)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveBannerFixed(
    event: SessionEvent,
    countdown: String,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = dotPulse))
            )
            
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE",
                        fontFamily = brigendsFont,
                        fontSize = 12.sp,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Text(
                        text = event.sessionType.displayName().uppercase(),
                        fontFamily = brigendsFont,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "Ends in $countdown",
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CompletedCard(
    event: CompletedEvent,
    brigendsFont: FontFamily,
    michromaFont: FontFamily,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.sessionType.displayName(),
            fontFamily = michromaFont,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        if (event.topThree.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                event.topThree.forEach { result ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (result.position) {
                                    1 -> accentColor.copy(alpha = 0.25f)
                                    2 -> Color.White.copy(alpha = 0.12f)
                                    3 -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = result.driverCode,
                            fontFamily = brigendsFont,
                            fontSize = 11.sp,
                            color = if (result.position == 1) accentColor else Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            Text(
                text = "✓ Completed",
                fontFamily = michromaFont,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun DividerFixed() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun LoadingHeroFixed(brigendsFont: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LOADING...",
            fontFamily = brigendsFont,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun ErrorHeroFixed(message: String, brigendsFont: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ERROR",
                fontFamily = brigendsFont,
                fontSize = 14.sp,
                color = Color(0xFFFF0080),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

private fun parseISTDateTimeFixed(date: String, time: String): LocalDateTime {
    val dateTimeString = "${date}T${time}"
    val utcDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
    return utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDateTime()
}

private fun formatISTTimeFixed(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a")
    return dateTime.format(formatter)
}

private fun formatDayTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("EEE h:mm a")
    return dateTime.format(formatter)
}

private fun formatFullDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd EEE h:mm a")
    return dateTime.format(formatter).uppercase()
}

private fun formatDateRange(race: Race): String {
    // Get all session dates to find the range
    val dates = mutableListOf<LocalDateTime>()
    
    race.firstPractice?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    race.secondPractice?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    race.thirdPractice?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    race.sprintQualifying?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    race.qualifying?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    race.sprint?.let { dates.add(parseISTDateTimeFixed(it.date, it.time)) }
    dates.add(parseISTDateTimeFixed(race.date, race.time))
    
    if (dates.isEmpty()) return ""
    
    val startDate = dates.minOrNull() ?: return ""
    val endDate = dates.maxOrNull() ?: return ""
    
    val startFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val endFormatter = DateTimeFormatter.ofPattern("dd MMM")
    
    return if (startDate.toLocalDate() == endDate.toLocalDate()) {
        // Single day event
        startDate.format(startFormatter)
    } else {
        // Multi-day event
        "${startDate.format(startFormatter)} - ${endDate.format(endFormatter)}"
    }
}

private fun getCountryCodeFixed(country: String): String {
    return when (country.lowercase()) {
        "australia" -> "au"
        "china" -> "cn"
        "japan" -> "jp"
        "bahrain" -> "bh"
        "saudi arabia" -> "sa"
        "usa" -> "us"
        "italy" -> "it"
        "monaco" -> "mc"
        "spain" -> "es"
        "canada" -> "ca"
        "austria" -> "at"
        "uk", "united kingdom" -> "gb"
        "hungary" -> "hu"
        "belgium" -> "be"
        "netherlands" -> "nl"
        "singapore" -> "sg"
        "azerbaijan" -> "az"
        "mexico" -> "mx"
        "brazil" -> "br"
        "qatar" -> "qa"
        "uae", "united arab emirates" -> "ae"
        else -> "un"
    }
}

private fun getCircuitDrawableById(circuitId: String): Int {
    return when (circuitId.lowercase()) {
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
        "zandvoort" -> R.drawable.netherlands_circuit
        "baku" -> R.drawable.azerbaijan_circuit
        else -> R.drawable.bahrain_circuit // Default fallback
    }
}

private fun getFlagColorsFixed(country: String): Pair<Color, Color> {
    return when (country.lowercase()) {
        "australia" -> Pair(Color(0xFF00008B), Color(0xFFFFD700))
        "bahrain" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "china" -> Pair(Color(0xFFDE2910), Color(0xFFFFDE00))
        "monaco" -> Pair(Color(0xFFCE1126), Color(0xFFFFFFFF))
        "spain" -> Pair(Color(0xFFC60B1E), Color(0xFFFFC400))
        "canada" -> Pair(Color(0xFFFF0000), Color(0xFFFFFFFF))
        "austria" -> Pair(Color(0xFFED2939), Color(0xFFFFFFFF))
        "uk", "great britain", "united kingdom" -> Pair(Color(0xFF012169), Color(0xFFC8102E))
        "hungary" -> Pair(Color(0xFFCD2A3E), Color(0xFF436F4D))
        "belgium" -> Pair(Color(0xFF000000), Color(0xFFFDDA24))
        "italy" -> Pair(Color(0xFF009246), Color(0xFFCE2B37))
        "singapore" -> Pair(Color(0xFFEF3340), Color(0xFFFFFFFF))
        "japan" -> Pair(Color(0xFFBC002D), Color(0xFFFFFFFF))
        "usa", "united states" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        "mexico" -> Pair(Color(0xFF006847), Color(0xFFCE1126))
        "brazil" -> Pair(Color(0xFF009B3A), Color(0xFFFEDF00))
        "qatar" -> Pair(Color(0xFF8A1538), Color(0xFFFFFFFF))
        "uae", "abu dhabi", "united arab emirates" -> Pair(Color(0xFFFF0000), Color(0xFF00732F))
        "saudi arabia" -> Pair(Color(0xFF165B33), Color(0xFFFFFFFF))
        "netherlands" -> Pair(Color(0xFF21468B), Color(0xFFAE1C28))
        "azerbaijan" -> Pair(Color(0xFF00B5E2), Color(0xFFEF3340))
        "miami" -> Pair(Color(0xFF3C3B6E), Color(0xFFB22234))
        else -> Pair(Color(0xFF1A0033), Color(0xFFFF0080))
    }
}

