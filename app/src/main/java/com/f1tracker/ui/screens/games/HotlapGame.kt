package com.f1tracker.ui.screens.games

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.f1tracker.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

enum class HotlapState { NAME_ENTRY, READY, COUNTDOWN, PLAYING, DNF, FINISHED }

data class TrackData(
    val name: String,
    val location: String,
    val points: List<Offset>
)

data class GhostFrame(
    val x: Float,
    val y: Float,
    val angle: Float,
    val timeMs: Long
)

data class LeaderboardEntry(
    val name: String = "",
    val time: Long = 0,
    val timestamp: Long = 0
)

private val GAME_SCREEN_SIZE = 300.dp

@Composable
fun HotlapGame(
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
    
    val prefs = remember { context.getSharedPreferences("hotlap_game", Context.MODE_PRIVATE) }
    
    // Firebase Database reference
    val database = remember { FirebaseDatabase.getInstance() }
    val leaderboardRef = remember { database.getReference("hotlap/leaderboard") }
    val ghostsRef = remember { database.getReference("hotlap/ghosts") }
    
    // Current track ID for storage keys
    val trackId = "masters-circuit"
    
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    var trackData by remember { mutableStateOf<TrackData?>(null) }
    var gameState by remember { mutableStateOf(
        if (playerName.isEmpty()) HotlapState.NAME_ENTRY else HotlapState.READY
    ) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var lapTimeMs by remember { mutableLongStateOf(0L) }
    var bestLapTimeMs by remember { mutableLongStateOf(prefs.getLong("best_time_$trackId", Long.MAX_VALUE)) }
    
    var carPosition by remember { mutableStateOf(Offset.Zero) }
    var carAngle by remember { mutableFloatStateOf(0f) }
    var steerDirection by remember { mutableIntStateOf(0) }
    var carPathIndex by remember { mutableFloatStateOf(0f) }
    
    // Ghost data - track specific
    var myGhostEnabled by remember { mutableStateOf(false) }
    var myGhostData by remember { mutableStateOf<List<GhostFrame>>(loadGhostData(prefs, trackId)) }
    var worldGhostEnabled by remember { mutableStateOf(false) }
    var wrGhostData by remember { mutableStateOf<List<GhostFrame>>(emptyList()) }
    var currentGhostFrame by remember { mutableIntStateOf(0) }
    var currentLapRecording by remember { mutableStateOf<MutableList<GhostFrame>>(mutableListOf()) }
    
    // Leaderboard state
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var globalBestTime by remember { mutableLongStateOf(Long.MAX_VALUE) }
    var worldRecordHolder by remember { mutableStateOf("---") }
    var yourRank by remember { mutableIntStateOf(0) }
    
    // Name entry dialog state
    var nameInput by remember { mutableStateOf("") }
    
    val carSpeed = 0.005f
    val steerSpeed = 0.06f
    val trackWidth = 0.06f
    
    // Fetch leaderboard from Firebase
    LaunchedEffect(trackId) {
        leaderboardRef.child(trackId).orderByChild("time").limitToFirst(100)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = mutableListOf<LeaderboardEntry>()
                    snapshot.children.forEach { child ->
                        child.getValue(LeaderboardEntry::class.java)?.let { entries.add(it) }
                    }
                    leaderboard = entries.sortedBy { it.time }
                    val topEntry = leaderboard.firstOrNull()
                    globalBestTime = topEntry?.time ?: Long.MAX_VALUE
                    val newWrHolder = topEntry?.name ?: "---"
                    
                    // Fetch WR ghost if holder changed
                    if (newWrHolder != worldRecordHolder && newWrHolder != "---") {
                        ghostsRef.child(trackId).child(newWrHolder)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(ghostSnapshot: DataSnapshot) {
                                    val frames = mutableListOf<GhostFrame>()
                                    ghostSnapshot.children.forEach { frame ->
                                        val x = frame.child("x").getValue(Double::class.java)?.toFloat() ?: 0f
                                        val y = frame.child("y").getValue(Double::class.java)?.toFloat() ?: 0f
                                        val angle = frame.child("a").getValue(Double::class.java)?.toFloat() ?: 0f
                                        val time = frame.child("t").getValue(Long::class.java) ?: 0L
                                        frames.add(GhostFrame(x, y, angle, time))
                                    }
                                    wrGhostData = frames.sortedBy { it.timeMs }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    worldRecordHolder = newWrHolder
                    
                    yourRank = entries.indexOfFirst { it.name == playerName && it.time == bestLapTimeMs } + 1
                    if (yourRank == 0 && bestLapTimeMs < Long.MAX_VALUE) {
                        yourRank = entries.count { it.time < bestLapTimeMs } + 1
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    // Save best time with track-specific key
    LaunchedEffect(bestLapTimeMs) {
        if (bestLapTimeMs < Long.MAX_VALUE) {
            prefs.edit().putLong("best_time_$trackId", bestLapTimeMs).apply()
        }
    }
    
    LaunchedEffect(Unit) {
        trackData = loadRandomTrack(context)
        trackData?.let {
            if (it.points.isNotEmpty()) {
                carPosition = it.points[0]
                if (it.points.size > 1) {
                    carAngle = atan2(it.points[1].y - it.points[0].y, it.points[1].x - it.points[0].x)
                }
            }
        }
    }
    
    LaunchedEffect(gameState) {
        if (gameState == HotlapState.COUNTDOWN) {
            currentLapRecording = mutableListOf()
            currentGhostFrame = 0
            countdownValue = 3
            delay(500L)
            countdownValue = 2
            delay(500L)
            countdownValue = 1
            delay(500L)
            gameState = HotlapState.PLAYING
        }
    }
    
    LaunchedEffect(gameState, trackData) {
        if (gameState == HotlapState.PLAYING && trackData != null) {
            val startTime = System.currentTimeMillis()
            val track = trackData!!
            
            steerDirection = 0
            
            if (track.points.isNotEmpty()) {
                carPosition = track.points[0]
                if (track.points.size > 1) {
                    carAngle = atan2(track.points[1].y - track.points[0].y, track.points[1].x - track.points[0].x)
                }
            }
            carPathIndex = 0f
            
            var hasLeftStart = false
            
            while (gameState == HotlapState.PLAYING) {
                delay(16L)
                lapTimeMs = System.currentTimeMillis() - startTime
                
                carAngle += steerDirection * steerSpeed
                carPosition = Offset(
                    carPosition.x + cos(carAngle) * carSpeed,
                    carPosition.y + sin(carAngle) * carSpeed
                )
                
                // Record ghost frame
                currentLapRecording.add(GhostFrame(carPosition.x, carPosition.y, carAngle, lapTimeMs))
                
                // Update ghost playback
                if (myGhostEnabled && myGhostData.isNotEmpty()) {
                    while (currentGhostFrame < myGhostData.size - 1 && 
                           myGhostData[currentGhostFrame].timeMs < lapTimeMs) {
                        currentGhostFrame++
                    }
                }
                
                val distToTrack = distanceToTrack(carPosition, track.points)
                if (distToTrack > trackWidth) {
                    gameState = HotlapState.DNF
                    break
                }
                
                val distToStart = distance(carPosition, track.points[0])
                
                if (distToStart > 0.1f) {
                    hasLeftStart = true
                }
                
                if (hasLeftStart && lapTimeMs > 3000 && distToStart < 0.08f) {
                    gameState = HotlapState.FINISHED
                    if (lapTimeMs < bestLapTimeMs) {
                        bestLapTimeMs = lapTimeMs
                        myGhostData = currentLapRecording.toList()
                        saveGhostData(prefs, trackId, myGhostData)
                        
                        // Submit to Firebase leaderboard
                        val entry = mapOf(
                            "name" to playerName,
                            "time" to lapTimeMs,
                            "timestamp" to System.currentTimeMillis()
                        )
                        leaderboardRef.child(trackId).child(playerName).setValue(entry)
                        
                        // Upload ghost data to Firebase (sampled for smaller size)
                        val ghostFrames = mutableMapOf<String, Any>()
                        myGhostData.filterIndexed { i, _ -> i % 5 == 0 }.forEachIndexed { i, frame ->
                            ghostFrames[i.toString()] = mapOf(
                                "x" to frame.x,
                                "y" to frame.y,
                                "a" to frame.angle,
                                "t" to frame.timeMs
                            )
                        }
                        ghostsRef.child(trackId).child(playerName).setValue(ghostFrames)
                    }
                    break
                }
            }
        }
    }
    
    // Name Entry Dialog
    if (gameState == HotlapState.NAME_ENTRY) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTER NAME", fontFamily = pixelFont, fontSize = 14.sp, color = Color.Black)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 10) nameInput = it.uppercase() },
                        singleLine = true,
                        modifier = Modifier.width(200.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00AA00), RoundedCornerShape(4.dp))
                            .clickable {
                                if (nameInput.isNotEmpty()) {
                                    playerName = nameInput
                                    prefs.edit().putString("player_name", playerName).apply()
                                    gameState = HotlapState.READY
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("OK", fontFamily = pixelFont, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName.ifEmpty { "HOTLAP" },
                    fontFamily = pixelFont,
                    fontSize = 8.sp,
                    color = Color.Gray
                )
                Text(
                    text = trackData?.name?.uppercase() ?: "LOADING...",
                    fontFamily = pixelFont,
                    fontSize = 10.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
            
            Box(
                modifier = Modifier
                    .background(
                        when (gameState) {
                            HotlapState.DNF -> Color.Red
                            HotlapState.FINISHED -> Color(0xFF00AA00)
                            else -> Color.White
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatLapTime(lapTimeMs),
                    fontFamily = pixelFont,
                    fontSize = 10.sp,
                    color = when (gameState) {
                        HotlapState.DNF, HotlapState.FINISHED -> Color.White
                        else -> Color.Black
                    }
                )
            }
        }
        
        // Stats chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Your Best Time chip
            if (bestLapTimeMs < Long.MAX_VALUE) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFFD700), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("YOUR BEST", fontFamily = pixelFont, fontSize = 5.sp, color = Color.Black)
                        Text(formatLapTime(bestLapTimeMs), fontFamily = pixelFont, fontSize = 7.sp, color = Color.Black)
                    }
                }
            }
            
            // Global Best chip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF00AAFF), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(worldRecordHolder, fontFamily = pixelFont, fontSize = 5.sp, color = Color.White)
                    Text(
                        if (globalBestTime < Long.MAX_VALUE) formatLapTime(globalBestTime) else "---",
                        fontFamily = pixelFont, fontSize = 7.sp, color = Color.White
                    )
                }
            }
            
            // Your Rank chip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF9933FF), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOUR RANK", fontFamily = pixelFont, fontSize = 5.sp, color = Color.White)
                    Text(
                        if (yourRank > 0) "#$yourRank/${leaderboard.size}" else "-/${leaderboard.size}",
                        fontFamily = pixelFont, fontSize = 7.sp, color = Color.White
                    )
                }
            }
        }
        
        Spacer(Modifier.weight(0.3f))
        
        // CARD CONTAINER
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(GAME_SCREEN_SIZE)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                    .border(4.dp, Color.Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (trackData != null) {
                    GameCanvas(
                        track = trackData!!,
                        carPosition = carPosition,
                        carAngle = carAngle,
                        gameState = gameState,
                        trackWidth = trackWidth,
                        ghostEnabled = myGhostEnabled,
                        ghostData = myGhostData,
                        wrGhostEnabled = worldGhostEnabled,
                        wrGhostData = wrGhostData,
                        lapTimeMs = lapTimeMs
                    )
                }
                
                if (gameState == HotlapState.COUNTDOWN) {
                    Text(countdownValue.toString(), fontFamily = pixelFont, fontSize = 64.sp, color = Color.Black)
                }
                
                if (gameState == HotlapState.DNF) {
                    Text("DNF", fontFamily = pixelFont, fontSize = 32.sp, color = Color.Red)
                }
                
                if (gameState == HotlapState.FINISHED) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FINISH", fontFamily = pixelFont, fontSize = 16.sp, color = Color(0xFF00AA00))
                        Spacer(Modifier.height(8.dp))
                        Text(formatLapTime(lapTimeMs), fontFamily = pixelFont, fontSize = 20.sp, color = Color.Black)
                    }
                }
                
                // Leaderboard overlay
                if (showLeaderboard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "LEADERBOARD",
                                fontFamily = pixelFont,
                                fontSize = 10.sp,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            if (leaderboard.isEmpty()) {
                                Text(
                                    text = "NO TIMES YET",
                                    fontFamily = pixelFont,
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(leaderboard.take(10)) { index, entry ->
                                        val isYou = entry.name == playerName
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isYou) Color(0xFF00AA00).copy(alpha = 0.3f) else Color.Transparent
                                                )
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${entry.name}",
                                                fontFamily = pixelFont,
                                                fontSize = 7.sp,
                                                color = when (index) {
                                                    0 -> Color(0xFFFFD700)
                                                    1 -> Color(0xFFCCCCCC)
                                                    2 -> Color(0xFFCD7F32)
                                                    else -> Color.White
                                                }
                                            )
                                            Text(
                                                text = formatLapTime(entry.time),
                                                fontFamily = pixelFont,
                                                fontSize = 7.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(0.3f))
        
        // GHOST TOGGLES & LEADERBOARD - Above controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // My Ghost toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (myGhostEnabled) Color(0xFF00FF00) else Color(0xFF333333),
                        RoundedCornerShape(4.dp)
                    )
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                    .clickable { myGhostEnabled = !myGhostEnabled }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (myGhostEnabled) "ME:ON" else "ME:OFF",
                    fontFamily = pixelFont,
                    fontSize = 6.sp,
                    color = Color.White
                )
            }
            
            // World Record Ghost toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (worldGhostEnabled) Color(0xFF00AAFF) else Color(0xFF333333),
                        RoundedCornerShape(4.dp)
                    )
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                    .clickable { worldGhostEnabled = !worldGhostEnabled }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (worldGhostEnabled) "WR:ON" else "WR:OFF",
                    fontFamily = pixelFont,
                    fontSize = 6.sp,
                    color = Color.White
                )
            }
            
            // Leaderboard Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (showLeaderboard) Color(0xFFFF6600) else Color(0xFF9933FF),
                        RoundedCornerShape(4.dp)
                    )
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                    .clickable { showLeaderboard = !showLeaderboard }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showLeaderboard) "CLOSE" else "RANKS",
                    fontFamily = pixelFont,
                    fontSize = 6.sp,
                    color = Color.White
                )
            }
        }
        
        // CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(
                        if (gameState == HotlapState.PLAYING) Color.White else Color.Gray,
                        RoundedCornerShape(4.dp)
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(4.dp))
                    .pointerInput(gameState) {
                        if (gameState == HotlapState.PLAYING) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    steerDirection = if (event.changes.any { it.pressed }) -1 else 0
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("◀", fontSize = 32.sp, color = if (gameState == HotlapState.PLAYING) Color.Black else Color.DarkGray)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(
                        when (gameState) {
                            HotlapState.READY -> Color(0xFF00AA00)
                            HotlapState.DNF -> Color(0xFFFFAA00)
                            HotlapState.FINISHED -> Color(0xFF00AA00)
                            else -> Color.Gray
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(4.dp))
                    .pointerInput(gameState) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed }) {
                                    when (gameState) {
                                        HotlapState.READY -> gameState = HotlapState.COUNTDOWN
                                        HotlapState.DNF, HotlapState.FINISHED -> {
                                            carPathIndex = 0f
                                            lapTimeMs = 0L
                                            gameState = HotlapState.COUNTDOWN
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (gameState) {
                        HotlapState.READY -> "START"
                        HotlapState.DNF, HotlapState.FINISHED -> "RETRY"
                        else -> "GO"
                    },
                    fontFamily = pixelFont,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(
                        if (gameState == HotlapState.PLAYING) Color.White else Color.Gray,
                        RoundedCornerShape(4.dp)
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(4.dp))
                    .pointerInput(gameState) {
                        if (gameState == HotlapState.PLAYING) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    steerDirection = if (event.changes.any { it.pressed }) 1 else 0
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 32.sp, color = if (gameState == HotlapState.PLAYING) Color.Black else Color.DarkGray)
            }
        }
    }
}

@Composable
private fun GameCanvas(
    track: TrackData,
    carPosition: Offset,
    carAngle: Float,
    gameState: HotlapState,
    trackWidth: Float,
    ghostEnabled: Boolean,
    ghostData: List<GhostFrame>,
    wrGhostEnabled: Boolean,
    wrGhostData: List<GhostFrame>,
    lapTimeMs: Long
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val padding = 20.dp.toPx()
        val canvasSize = size.minDimension - padding * 2
        
        val screenPoints = track.points.map { point ->
            Offset(padding + point.x * canvasSize, padding + point.y * canvasSize)
        }
        
        val trackPath = Path().apply {
            if (screenPoints.isNotEmpty()) {
                moveTo(screenPoints[0].x, screenPoints[0].y)
                for (i in 1 until screenPoints.size) {
                    lineTo(screenPoints[i].x, screenPoints[i].y)
                }
            }
        }
        
        drawPath(trackPath, Color.Black, 
            style = Stroke(width = trackWidth * canvasSize * 2.4f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
        drawPath(trackPath, Color.White, 
            style = Stroke(width = trackWidth * canvasSize * 2f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
        drawPath(trackPath, Color.Black, 
            style = Stroke(width = trackWidth * canvasSize * 0.2f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
        
        if (screenPoints.size > 1) {
            val start = screenPoints[0]
            val next = screenPoints[1]
            val angle = atan2(next.y - start.y, next.x - start.x)
            val perpAngle = angle + kotlin.math.PI.toFloat() / 2f
            val checkSize = 5.dp.toPx()
            
            for (row in -4..4) {
                for (col in -1..1) {
                    val isBlack = (row + col) % 2 == 0
                    val offsetX = cos(perpAngle) * row * checkSize + cos(angle) * col * checkSize
                    val offsetY = sin(perpAngle) * row * checkSize + sin(angle) * col * checkSize
                    drawRect(
                        color = if (isBlack) Color.Black else Color.White,
                        topLeft = Offset(start.x + offsetX - checkSize/2, start.y + offsetY - checkSize/2),
                        size = androidx.compose.ui.geometry.Size(checkSize, checkSize)
                    )
                }
            }
        }
        
        // Ghost car (transparent blue) - find frame based on time with interpolation
        if (ghostEnabled && ghostData.isNotEmpty() && lapTimeMs > 0) {
            // Find the ghost frames for interpolation
            var frameA = 0
            var frameB = 0
            for (i in ghostData.indices) {
                if (ghostData[i].timeMs <= lapTimeMs) {
                    frameA = i
                    frameB = minOf(i + 1, ghostData.size - 1)
                } else {
                    break
                }
            }
            
            val ghostA = ghostData[frameA]
            val ghostB = ghostData[frameB]
            
            // Interpolate between frames for smooth movement
            val t = if (frameA != frameB && ghostB.timeMs != ghostA.timeMs) {
                ((lapTimeMs - ghostA.timeMs).toFloat() / (ghostB.timeMs - ghostA.timeMs)).coerceIn(0f, 1f)
            } else 0f
            
            val ghostX = ghostA.x + (ghostB.x - ghostA.x) * t
            val ghostY = ghostA.y + (ghostB.y - ghostA.y) * t
            val ghostAngle = ghostA.angle + (ghostB.angle - ghostA.angle) * t
            
            val screenGhost = Offset(padding + ghostX * canvasSize, padding + ghostY * canvasSize)
            val px = 0.4f.dp.toPx()
            
            rotate(degrees = (ghostAngle * 180f / kotlin.math.PI.toFloat()) + 90f, pivot = screenGhost) {
                translate(left = screenGhost.x - 32 * px, top = screenGhost.y - 32 * px) {
                    // Ghost car in semi-transparent blue
                    drawRect(Color(0x8800AAFF), Offset(18 * px, 2 * px), androidx.compose.ui.geometry.Size(28 * px, 4 * px))
                    drawRect(Color(0x8800AAFF), Offset(18 * px, 54 * px), androidx.compose.ui.geometry.Size(28 * px, 6 * px))
                    drawRect(Color(0xAA0088FF), Offset(28 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xAA0088FF), Offset(26 * px, 16 * px), androidx.compose.ui.geometry.Size(12 * px, 38 * px))
                    drawRect(Color(0xBB0066DD), Offset(16 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xBB0066DD), Offset(40 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xBB0066DD), Offset(14 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                    drawRect(Color(0xBB0066DD), Offset(40 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                }
            }
        }
        
        // WR Ghost car (gold/yellow) - find frame based on time with interpolation
        if (wrGhostEnabled && wrGhostData.isNotEmpty() && lapTimeMs > 0) {
            var frameA = 0
            var frameB = 0
            for (i in wrGhostData.indices) {
                if (wrGhostData[i].timeMs <= lapTimeMs) {
                    frameA = i
                    frameB = minOf(i + 1, wrGhostData.size - 1)
                } else {
                    break
                }
            }
            
            val ghostA = wrGhostData[frameA]
            val ghostB = wrGhostData[frameB]
            
            val t = if (frameA != frameB && ghostB.timeMs != ghostA.timeMs) {
                ((lapTimeMs - ghostA.timeMs).toFloat() / (ghostB.timeMs - ghostA.timeMs)).coerceIn(0f, 1f)
            } else 0f
            
            val ghostX = ghostA.x + (ghostB.x - ghostA.x) * t
            val ghostY = ghostA.y + (ghostB.y - ghostA.y) * t
            val ghostAngle = ghostA.angle + (ghostB.angle - ghostA.angle) * t
            
            val screenGhost = Offset(padding + ghostX * canvasSize, padding + ghostY * canvasSize)
            val px = 0.4f.dp.toPx()
            
            rotate(degrees = (ghostAngle * 180f / kotlin.math.PI.toFloat()) + 90f, pivot = screenGhost) {
                translate(left = screenGhost.x - 32 * px, top = screenGhost.y - 32 * px) {
                    // WR Ghost car in semi-transparent gold
                    drawRect(Color(0x88FFD700), Offset(18 * px, 2 * px), androidx.compose.ui.geometry.Size(28 * px, 4 * px))
                    drawRect(Color(0x88FFD700), Offset(18 * px, 54 * px), androidx.compose.ui.geometry.Size(28 * px, 6 * px))
                    drawRect(Color(0xAAFFAA00), Offset(28 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xAAFFAA00), Offset(26 * px, 16 * px), androidx.compose.ui.geometry.Size(12 * px, 38 * px))
                    drawRect(Color(0xBBFF8800), Offset(16 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xBBFF8800), Offset(40 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xBBFF8800), Offset(14 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                    drawRect(Color(0xBBFF8800), Offset(40 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                }
            }
        }
        
        // Player car
        if (gameState == HotlapState.PLAYING || gameState == HotlapState.COUNTDOWN) {
            val screenCar = Offset(padding + carPosition.x * canvasSize, padding + carPosition.y * canvasSize)
            val px = 0.4f.dp.toPx()
            
            rotate(degrees = (carAngle * 180f / kotlin.math.PI.toFloat()) + 90f, pivot = screenCar) {
                translate(left = screenCar.x - 32 * px, top = screenCar.y - 32 * px) {
                    drawRect(Color.White, Offset(18 * px, 2 * px), androidx.compose.ui.geometry.Size(28 * px, 4 * px))
                    drawRect(Color.White, Offset(18 * px, 54 * px), androidx.compose.ui.geometry.Size(28 * px, 6 * px))
                    drawRect(Color(0xFFE74C3C), Offset(28 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color(0xFFE74C3C), Offset(26 * px, 16 * px), androidx.compose.ui.geometry.Size(12 * px, 38 * px))
                    drawRect(Color.Black, Offset(16 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color.Black, Offset(40 * px, 10 * px), androidx.compose.ui.geometry.Size(8 * px, 10 * px))
                    drawRect(Color.Black, Offset(14 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                    drawRect(Color.Black, Offset(40 * px, 42 * px), androidx.compose.ui.geometry.Size(10 * px, 12 * px))
                }
            }
        }
    }
}

private fun loadGhostData(prefs: android.content.SharedPreferences, trackId: String): List<GhostFrame> {
    val json = prefs.getString("ghost_data_$trackId", null) ?: return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            GhostFrame(
                obj.getDouble("x").toFloat(),
                obj.getDouble("y").toFloat(),
                obj.getDouble("angle").toFloat(),
                obj.getLong("time")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveGhostData(prefs: android.content.SharedPreferences, trackId: String, data: List<GhostFrame>) {
    val array = JSONArray()
    // Sample every 3rd frame to reduce storage
    data.filterIndexed { index, _ -> index % 3 == 0 }.forEach { frame ->
        val obj = JSONObject()
        obj.put("x", frame.x)
        obj.put("y", frame.y)
        obj.put("angle", frame.angle)
        obj.put("time", frame.timeMs)
        array.put(obj)
    }
    prefs.edit().putString("ghost_data_$trackId", array.toString()).apply()
}

private fun distanceToTrack(point: Offset, trackPoints: List<Offset>): Float {
    var minDist = Float.MAX_VALUE
    for (i in 0 until trackPoints.size - 1) {
        val dist = pointToSegmentDistance(point, trackPoints[i], trackPoints[i + 1])
        if (dist < minDist) minDist = dist
    }
    return minDist
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = Offset(b.x - a.x, b.y - a.y)
    val ap = Offset(p.x - a.x, p.y - a.y)
    val abLen2 = ab.x * ab.x + ab.y * ab.y
    if (abLen2 == 0f) return distance(p, a)
    val t = ((ap.x * ab.x + ap.y * ab.y) / abLen2).coerceIn(0f, 1f)
    val closest = Offset(a.x + t * ab.x, a.y + t * ab.y)
    return distance(p, closest)
}
private fun loadRandomTrack(context: Context): TrackData? {
    return try {
        val selectedFile = "masters-circuit.geojson"
        
        val jsonString = context.assets.open("circuits/$selectedFile").bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)
        val features = json.getJSONArray("features")
        val feature = features.getJSONObject(0)
        val properties = feature.getJSONObject("properties")
        val geometry = feature.getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")
        
        val rawPoints = mutableListOf<Offset>()
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            rawPoints.add(Offset(coord.getDouble(0).toFloat(), coord.getDouble(1).toFloat()))
        }
        
        val minX = rawPoints.minOf { it.x }
        val maxX = rawPoints.maxOf { it.x }
        val minY = rawPoints.minOf { it.y }
        val maxY = rawPoints.maxOf { it.y }
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        val maxRange = maxOf(rangeX, rangeY)
        
        val normalizedPoints = rawPoints.map { point ->
            Offset((point.x - minX) / maxRange, 1f - (point.y - minY) / maxRange)
        }
        
        TrackData(
            name = properties.optString("Name", "Unknown"),
            location = properties.optString("Location", ""),
            points = normalizedPoints
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun formatLapTime(ms: Long): String {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return String.format("%d:%02d.%03d", minutes, seconds, millis)
}
