package com.f1tracker.ui.screens.games

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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
    val points: List<Offset>,
    val totalDistance: Float = 0f,    // B5 Fix: Total path length
    val pointDistances: List<Float> = emptyList() // B5 Fix: Cumulative distance at each point
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
    val timestamp: Long = 0,
    val uid: String = ""
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotlapGame(
    michromaFont: FontFamily,
    brigendsFont: FontFamily,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
    
    val prefs = remember { context.getSharedPreferences("hotlap_game", Context.MODE_PRIVATE) }
    
    // Sound system
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var engineSoundEnabled by remember { mutableStateOf(prefs.getBoolean("engine_sound_enabled", true)) }
    var engineStreamId by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        android.util.Log.d("HotlapGame", "Sound enabled: $soundEnabled, Engine enabled: $engineSoundEnabled")
    }
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(8)  // Increased for engine loop
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val soundIds = remember {
        mapOf(
            "countdown" to soundPool.load(context, R.raw.beep_countdown, 1),
            "go" to soundPool.load(context, R.raw.beep_go, 1),
            "sector_wr" to soundPool.load(context, R.raw.sector_wr, 1),
            "sector_pb" to soundPool.load(context, R.raw.sector_pb, 1),
            "sector_slow" to soundPool.load(context, R.raw.sector_slow, 1),
            "dnf" to soundPool.load(context, R.raw.sound_dnf, 1),
            "finish" to soundPool.load(context, R.raw.finish_normal, 1),
            "finish_pb" to soundPool.load(context, R.raw.finish_pb, 1),
            "finish_wr" to soundPool.load(context, R.raw.finish_wr, 1),
            "engine" to soundPool.load(context, R.raw.loop_5, 1),
            "screech" to soundPool.load(context, R.raw.screech_loop, 1)
        )
    }
    
    fun playSound(name: String) {
        if (soundEnabled) {
            soundIds[name]?.let { soundPool.play(it, 1f, 1f, 0, 0, 1f) }
        }
    }
    
    // Firebase Database reference
    val database = remember { FirebaseDatabase.getInstance() }
    val leaderboardRef = remember { database.getReference("hotlap/leaderboard") }
    val ghostsRef = remember { database.getReference("hotlap/ghosts") }
    val sectorTimesRef = remember { database.getReference("hotlap/sector_times") }
    
    // Current track ID for storage keys
    // Current track ID for storage keys
    var trackId by remember { mutableStateOf(prefs.getString("last_track_id", "track_masters") ?: "track_masters") }
    // Difficulty Filter State (Null = All, 1=Easy, 3=Med, 5=Hard)
    var difficultyFilter by remember { mutableStateOf<Int?>(null) }

    
    // Settings visibility
    var showSettings by remember { mutableStateOf(false) }
    
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    val playerUuid = remember {
        var id = prefs.getString("player_uuid", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("player_uuid", id).apply()
        }
        id!!
    }
    // Load track from Factory instead of GeoJSON
    // Use derived state or effect to reload when trackId changes
    var trackData by remember(trackId) { mutableStateOf(TrackFactory.getTrack(trackId)) }
    
    // Reload Best Time when trackId changes
    var bestLapTimeMs by remember(trackId) { mutableStateOf(prefs.getLong("best_time_$trackId", Long.MAX_VALUE)) }

    var gameState by remember { mutableStateOf(
        if (playerName.isEmpty()) HotlapState.NAME_ENTRY else HotlapState.READY
    ) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var startTime by remember { mutableLongStateOf(0L) }
    var lapTimeMs by remember { mutableLongStateOf(0L) }
    var isNewPB by remember { mutableStateOf(false) }  // Track if last finish was a PB
    var isNewWR by remember { mutableStateOf(false) }  // Track if last finish was a WR
    
    // Sector timing state (3 sectors: 0-33%, 33-66%, 66-100%)
    var currentSector by remember { mutableIntStateOf(0) }  // Current sector (0, 1, 2)
    var lastSectorTime by remember { mutableLongStateOf(0L) }  // Time when entered current sector
    var sectorTimes by remember { mutableStateOf(listOf(0L, 0L, 0L)) }  // Current lap sector times
    var bestSectorTimes by remember(trackId) { mutableStateOf(listOf(
        prefs.getLong("best_sector_0_$trackId", Long.MAX_VALUE),
        prefs.getLong("best_sector_1_$trackId", Long.MAX_VALUE),
        prefs.getLong("best_sector_2_$trackId", Long.MAX_VALUE)
    )) }  // PB sector times
    var wrSectorTimes by remember { mutableStateOf(listOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)) }  // WR sector times
    var sectorTimesLoaded by remember { mutableStateOf(false) }  // B7 Fix: Track when Firebase sector times are loaded
    // Sector results: -1=pending, 0=yellow(slower), 1=green(PB), 2=purple(WR)
    var sectorResults by remember { mutableStateOf(listOf(-1, -1, -1)) }
    // Original WR sector times at lap start (for delta display - these don't get updated mid-lap)
    var originalWrSectorTimes by remember { mutableStateOf(listOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)) }
    // Original PB sector times at lap start (for delta fallback)
    var originalBestSectorTimes by remember { mutableStateOf(listOf(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)) }
    // Pending WR sector updates (to apply after lap completion)
    var pendingWrSectorUpdates by remember { mutableStateOf(mutableMapOf<Int, Long>()) }
    // Pending PB sector updates (Strict Mode: only apply on finish)
    var pendingPbSectorUpdates by remember { mutableStateOf(mutableMapOf<Int, Long>()) }
    
    var carPosition by remember { mutableStateOf(Offset.Zero) }
    var carAngle by remember { mutableFloatStateOf(0f) }
    var steerDirection by remember { mutableIntStateOf(0) }
    var carPathIndex by remember { mutableFloatStateOf(0f) }
    
    // Steering duration for screech sound
    var steeringDuration by remember { mutableLongStateOf(0L) }
    var lastSteerTime by remember { mutableLongStateOf(0L) }
    var screechStreamId by remember { mutableIntStateOf(0) }
    
    // E2 Enhancement: Live Delta
    var liveDelta by remember { mutableStateOf<Long?>(null) }
    
    // Ghost data - track specific
    var myGhostEnabled by remember { mutableStateOf(false) }
    var myGhostData by remember(trackId) { mutableStateOf<List<GhostFrame>>(loadGhostData(prefs, trackId)) }
    var worldGhostEnabled by remember { mutableStateOf(false) }
    var wrGhostData by remember { mutableStateOf<List<GhostFrame>>(emptyList()) }
    var currentGhostFrame by remember { mutableIntStateOf(0) }
    var currentLapRecording by remember { mutableStateOf<MutableList<GhostFrame>>(mutableListOf()) }
    
    // Leaderboard state
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var showLeaderboard by remember { mutableStateOf(false) }

    var globalBestTime by remember { mutableLongStateOf(Long.MAX_VALUE) }
    var worldRecordHolder by remember { mutableStateOf("---") }
    var worldRecordHolderUid by remember { mutableStateOf("") } // Track WR Holder by UID
    var yourRank by remember { mutableIntStateOf(0) }
    
    // Name entry dialog state
    var nameInput by remember { mutableStateOf("") }
    
    val carSpeed = 0.30f  // units per second (was 0.005f per frame at 60fps)
    val steerSpeed = 3.6f  // radians per second (was 0.06f per frame at 60fps)
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
                    val newWrHolderName = topEntry?.name ?: "---"
                    val newWrHolderUid = topEntry?.uid ?: ""
                    
                    // Fetch WR ghost if holder changed (check by UID)
                    if (newWrHolderUid != worldRecordHolderUid && newWrHolderUid.isNotEmpty()) {
                        ghostsRef.child(trackId).child(newWrHolderUid) // Key by UID
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
                    worldRecordHolder = newWrHolderName
                    worldRecordHolderUid = newWrHolderUid
                    
                    // Rank by UID
                    yourRank = entries.indexOfFirst { it.uid == playerUuid && it.time == bestLapTimeMs } + 1
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
    
    // Fetch WR sector times from Firebase
    LaunchedEffect(trackId) {
        sectorTimesRef.child(trackId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val s1 = snapshot.child("s1").getValue(Long::class.java) ?: Long.MAX_VALUE
                val s2 = snapshot.child("s2").getValue(Long::class.java) ?: Long.MAX_VALUE
                val s3 = snapshot.child("s3").getValue(Long::class.java) ?: Long.MAX_VALUE
                wrSectorTimes = listOf(s1, s2, s3)
                sectorTimesLoaded = true  // B7 Fix: Mark as loaded when Firebase responds
            }
            override fun onCancelled(error: DatabaseError) {
                sectorTimesLoaded = true  // B7 Fix: Also mark loaded on error to not block forever
            }
        })
    }
    
    LaunchedEffect(Unit) {
        // No longer need async loading for TrackFactory, it is synchronous.
        // But if we want to simulate loading or ensure state:
        if (trackData == null) {
            trackData = TrackFactory.getTrack(trackId)
        }
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
            showLeaderboard = false  // Auto-close leaderboard when game starts
            showSettings = false     // Auto-close settings when game starts
            currentLapRecording = mutableListOf()
            currentGhostFrame = 0
            countdownValue = 3
            playSound("countdown")
            delay(500L)
            countdownValue = 2
            playSound("countdown")
            delay(500L)
            countdownValue = 1
            playSound("countdown")
            delay(500L)
            playSound("go")
            gameState = HotlapState.PLAYING
            startTime = System.currentTimeMillis() // Set start time when game begins
        }
    }
    
    // Engine sound loop management
    LaunchedEffect(gameState, soundEnabled, engineSoundEnabled) {
        if (gameState == HotlapState.PLAYING && soundEnabled && engineSoundEnabled) {
            if (engineStreamId == 0) {
                soundIds["engine"]?.let { id ->
                    // Increased volume to 100% (1.0f)
                    engineStreamId = soundPool.play(id, 1.0f, 1.0f, 1, -1, 1.0f)
                }
            }
        } else {
            if (engineStreamId != 0) {
                soundPool.stop(engineStreamId)
                engineStreamId = 0
            }
        }
    }
    
    // Tire screech management
    LaunchedEffect(steerDirection, gameState, soundEnabled) {
        if (gameState == HotlapState.PLAYING && soundEnabled && steerDirection != 0) {
            val startSteerTime = System.currentTimeMillis()
            try {
                while (true) {
                    val duration = System.currentTimeMillis() - startSteerTime
                    // Threshold set to 250ms
                    if (duration > 250L && screechStreamId == 0) {
                        soundIds["screech"]?.let { id ->
                            // Volume 50% (0.5f)
                            screechStreamId = soundPool.play(id, 0.5f, 0.5f, 1, -1, 1.2f)
                        }
                    }
                    delay(100L)
                }
            } finally {
                // Stop sound when steering stops (coroutine cancelled)
                if (screechStreamId != 0) {
                    soundPool.stop(screechStreamId)
                    screechStreamId = 0
                }
            }
        } else {
            // Ensure sound is stopped if state changes or steering is 0
            if (screechStreamId != 0) {
                soundPool.stop(screechStreamId)
                screechStreamId = 0
            }
        }
    }
    
    // Engine pitch modulation based on speed
    LaunchedEffect(carSpeed, engineStreamId) {
        if (engineStreamId != 0) {
            // Speed ranges roughly 0 to 15.0
            // Pitch range: 0.8 (idle) to 1.3 (max revs) - reduced range to keep it low
            val pitch = 0.8f + (carSpeed / 12.0f).coerceIn(0f, 0.5f)
            soundPool.setRate(engineStreamId, pitch)
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
            
            // Reset sector data for new lap
            currentSector = 0
            lastSectorTime = 0L
            sectorTimes = listOf(0L, 0L, 0L)
            sectorResults = listOf(-1, -1, -1)
            pendingWrSectorUpdates = mutableMapOf()  // Clear pending updates
            pendingPbSectorUpdates = mutableMapOf()  // Clear pending PB updates
            
            var hasLeftStart = false
            
            var lastFrameTime = System.currentTimeMillis()
            
            while (gameState == HotlapState.PLAYING) {
                delay(16L)
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastFrameTime) / 1000f  // seconds
                lastFrameTime = currentTime
                lapTimeMs = currentTime - startTime
                
                carAngle += steerDirection * steerSpeed * deltaTime
                carPosition = Offset(
                    carPosition.x + cos(carAngle) * carSpeed * deltaTime,
                    carPosition.y + sin(carAngle) * carSpeed * deltaTime
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
                
                // Sector crossing detection (find closest track point to determine progress)
                var closestPointIndex = 0
                var closestDistance = Float.MAX_VALUE
                for (i in track.points.indices) {
                    val dist = distance(carPosition, track.points[i])
                    if (dist < closestDistance) {
                        closestDistance = dist
                        closestPointIndex = i
                    }
                }
                // B5 Fix: Use distance-based progress instead of index-based
                val trackProgress = if (track.totalDistance > 0) {
                    track.pointDistances[closestPointIndex] / track.totalDistance
                } else {
                    closestPointIndex.toFloat() / track.points.size.toFloat()
                }
                val newSector = when {
                    trackProgress < 0.33f -> 0
                    trackProgress < 0.66f -> 1
                    else -> 2
                }
                
                if (newSector != currentSector && newSector > currentSector) {
                    // Crossed into a new sector
                    val completedSector = currentSector
                    val sectorTime = lapTimeMs - lastSectorTime
                    
                    // Update sector times
                    val newSectorTimes = sectorTimes.toMutableList()
                    newSectorTimes[completedSector] = sectorTime
                    sectorTimes = newSectorTimes
                    
                    // Determine sector result (compare to ORIGINAL WR times, not updated ones)
                    val newResults = sectorResults.toMutableList()
                    val wrTime = originalWrSectorTimes[completedSector]  // Use original, not current
                    val pbTime = bestSectorTimes[completedSector]
                    val isWRSector = sectorTime < wrTime
                    val isPBSector = sectorTime < pbTime
                    
                    android.util.Log.d("SectorDebug", "Sector $completedSector: Time=$sectorTime, WR=$wrTime, PB=$pbTime")
                    android.util.Log.d("SectorDebug", "Result: isWR=$isWRSector, isPB=$isPBSector")

                    newResults[completedSector] = when {
                        isWRSector -> 2  // Purple - WR
                        isPBSector -> 1  // Green - PB
                        else -> 0  // Yellow - slower
                    }
                    sectorResults = newResults
                    
                    // Play sector crossing sound
                    when (newResults[completedSector]) {
                        2 -> playSound("sector_wr")
                        1 -> playSound("sector_pb")
                        else -> playSound("sector_slow")
                    }
                    
                    // Update best sector times if this was a PB
                    if (sectorTime < pbTime) {
                         // Strict Mode: Queue update, don't apply until finish
                         pendingPbSectorUpdates[completedSector] = sectorTime
                    }
                    
                    // Queue WR sector update for Firebase (don't update local state mid-lap)
                    if (isWRSector) {
                        // B6 Fix: Don't upload immediately! Only queue.
                        // val sectorKey = "s${completedSector + 1}"
                        // sectorTimesRef.child(trackId).child(sectorKey).setValue(sectorTime)
                        pendingWrSectorUpdates[completedSector] = sectorTime
                    }
                    
                    lastSectorTime = lapTimeMs
                    currentSector = newSector
                }
                
                val distToTrack = distanceToTrack(carPosition, track.points)
                // Added 20% tolerance for kerbs/wheels-on-track
                if (distToTrack > trackWidth * 1.2f) {
                    gameState = HotlapState.DNF
                    playSound("dnf")
                    break
                }
                
                // E2 Enhancement: Calculate Live Delta
                if (lapTimeMs > 0) {
                   val refGhost = if (wrSectorTimes[0] < Long.MAX_VALUE && wrGhostData.isNotEmpty()) wrGhostData else if (myGhostData.isNotEmpty()) myGhostData else null
                   if (refGhost != null) {
                       // Find timestamp where ghost was at this position
                       var minEqDist = 1000f // Squared distance
                       var ghostTimeAtPos = -1L
                       
                       // Simple linear search is fast enough for ~1000 points
                       refGhost.forEach { frame ->
                           val dx = frame.x - carPosition.x
                           val dy = frame.y - carPosition.y
                           val d2 = dx*dx + dy*dy
                           if (d2 < minEqDist) {
                               minEqDist = d2
                               ghostTimeAtPos = frame.timeMs
                           }
                       }
                       
                       // Only show if we found a reasonably close point (approx 5% of screen width)
                       if (minEqDist < 0.0025f) {
                           liveDelta = lapTimeMs - ghostTimeAtPos
                       }
                   }
                }
                
                val distToStart = distance(carPosition, track.points[0])
                
                if (distToStart > 0.1f) {
                    hasLeftStart = true
                }
                
                if (hasLeftStart && lapTimeMs > 3000 && distToStart < 0.08f) {
                    gameState = HotlapState.FINISHED
                    
                    // Process sector 3 before finish (it wouldn't trigger normally since we stop at finish line)
                    if (currentSector == 2) {
                        val sector3Time = lapTimeMs - lastSectorTime
                        val newSectorTimes = sectorTimes.toMutableList()
                        newSectorTimes[2] = sector3Time
                        sectorTimes = newSectorTimes
                        
                        val newResults = sectorResults.toMutableList()
                        val wrTime = originalWrSectorTimes[2]  // Use original, not updated
                        val pbTime = bestSectorTimes[2]
                        val isWRSector = sector3Time < wrTime
                        val isPBSector = sector3Time < pbTime
                        
                        android.util.Log.d("SectorDebug", "Sector 2 (S3): Time=$sector3Time, WR=$wrTime, PB=$pbTime")
                        android.util.Log.d("SectorDebug", "Result: isWR=$isWRSector, isPB=$isPBSector")

                        newResults[2] = when {
                            isWRSector -> 2  // Purple - WR
                            isPBSector -> 1  // Green - PB
                            else -> 0  // Yellow - slower
                        }
                        sectorResults = newResults
                        
                        // Play sector 3 crossing sound
                        when (newResults[2]) {
                            2 -> playSound("sector_wr")
                            1 -> playSound("sector_pb")
                            else -> playSound("sector_slow")
                        }
                        
                        if (sector3Time < pbTime) {
                            // Strict Mode: Queue update
                            pendingPbSectorUpdates[2] = sector3Time
                        }
                        
                        // Queue WR sector update (don't update local state mid-lap)
                        if (isWRSector) {
                            // B6 Fix: Don't upload immediately! Only queue.
                            // sectorTimesRef.child(trackId).child("s3").setValue(sector3Time)
                            pendingWrSectorUpdates[2] = sector3Time
                        }
                    }
                    
                    // Apply pending WR sector updates NOW (lap is complete)
                    if (pendingWrSectorUpdates.isNotEmpty()) {
                        val newWrSectors = wrSectorTimes.toMutableList()
                        pendingWrSectorUpdates.forEach { (sector, time) ->
                            newWrSectors[sector] = time
                            // B6 Fix: Upload to Firebase NOW that lap is finished
                            val sectorKey = "s${sector + 1}"
                            android.util.Log.d("SectorDebug", "Uploading WR Sector $sectorKey: $time")
                            sectorTimesRef.child(trackId).child(sectorKey).setValue(time)
                                .addOnFailureListener { e ->
                                    android.util.Log.e("SectorDebug", "Upload Failed for $sectorKey: ${e.message}")
                                }
                                .addOnSuccessListener {
                                    android.util.Log.d("SectorDebug", "Upload Success for $sectorKey")
                                }
                        }
                        wrSectorTimes = newWrSectors
                        pendingWrSectorUpdates.clear()
                    }
                    
                    // Apply pending PB sector updates (Strict Mode)
                    if (pendingPbSectorUpdates.isNotEmpty()) {
                        val newBestSectors = bestSectorTimes.toMutableList()
                        pendingPbSectorUpdates.forEach { (sector, time) ->
                            newBestSectors[sector] = time
                            prefs.edit().putLong("best_sector_${sector}_$trackId", time).apply()
                        }
                        bestSectorTimes = newBestSectors
                        pendingPbSectorUpdates.clear()
                    }
                    
                    // Check for PB/WR BEFORE updating values
                    val wasNewWR = lapTimeMs < globalBestTime
                    val wasNewPB = lapTimeMs < bestLapTimeMs
                    isNewWR = wasNewWR
                    isNewPB = wasNewPB
                    
                    // Play finish sound
                    when {
                        wasNewWR -> playSound("finish_wr")
                        wasNewPB -> playSound("finish_pb")
                        else -> playSound("finish")
                    }
                    
                    if (wasNewPB) {
                        bestLapTimeMs = lapTimeMs
                        myGhostData = currentLapRecording.toList()
                        saveGhostData(prefs, trackId, myGhostData)
                        
                        // Submit to Firebase leaderboard
                        val entry = mapOf(
                            "name" to playerName,
                            "time" to lapTimeMs,
                            "timestamp" to System.currentTimeMillis(),
                            "uid" to playerUuid
                        )
                        leaderboardRef.child(trackId).child(playerUuid).setValue(entry)
                            .addOnSuccessListener { android.util.Log.d("Firebase", "Leaderboard updated for $playerName ($playerUuid)") }
                            .addOnFailureListener { e -> android.util.Log.e("Firebase", "Leaderboard update failed", e) }
                        
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
                        ghostsRef.child(trackId).child(playerUuid).setValue(ghostFrames)
                            .addOnSuccessListener { android.util.Log.d("Firebase", "Ghost data updated for $playerName") }
                            .addOnFailureListener { e -> android.util.Log.e("Firebase", "Ghost data update failed", e) }
                        
                        // Optimistic update: If user beat WR, immediately use their ghost as WR ghost
                        if (wasNewWR) {
                            wrGhostData = myGhostData.filterIndexed { i, _ -> i % 5 == 0 }
                            globalBestTime = lapTimeMs
                            worldRecordHolder = playerName
                        }
                    }
                    break
                }
            }
        }
    }
    


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
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                nameInput = input.filter { it.isLetterOrDigit() }.uppercase()
                            }
                        },
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
        
        // Stats Header (PB + WR + Rank)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Best Time
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (bestLapTimeMs < Long.MAX_VALUE) "PB ${formatLapTime(bestLapTimeMs)}" else "PB ---",
                    fontFamily = pixelFont,
                    fontSize = 8.sp,
                    color = Color.Black
                )
            }
            
            // WR Time
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF00AAFF), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (globalBestTime < Long.MAX_VALUE) "WR ${formatLapTime(globalBestTime)}" else "WR ---",
                    fontFamily = pixelFont,
                    fontSize = 8.sp,
                    color = Color.White
                )
            }
            
            // Rank
            Box(
                modifier = Modifier
                    .background(
                        if (showLeaderboard) Color(0xFFFF6600) else Color(0xFF9933FF),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { 
                        showSettings = false  // Close settings when opening leaderboard
                        showLeaderboard = !showLeaderboard 
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Rank: ${if (yourRank > 0) yourRank else "-"}/${leaderboard.size} >",
                    fontFamily = pixelFont,
                    fontSize = 8.sp,
                    color = Color.White
                )
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
                    .fillMaxWidth()
                    .aspectRatio(1f)
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
                        lapTimeMs = lapTimeMs,
                        sectorResults = sectorResults
                    )
                }
                
                if (gameState == HotlapState.COUNTDOWN) {
                    Text(countdownValue.toString(), fontFamily = pixelFont, fontSize = 64.sp, color = Color.Black)
                }
                
                if (gameState == HotlapState.DNF) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text("DNF", fontFamily = pixelFont, fontSize = 32.sp, color = Color.White)
                    }
                }
                
                if (gameState == HotlapState.FINISHED) {
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    isNewWR -> Color(0xFFFFD700).copy(alpha = 0.95f)  // Gold for WR
                                    isNewPB -> Color(0xFF00FF00).copy(alpha = 0.95f)  // Green for PB
                                    else -> Color(0xFF00AA00).copy(alpha = 0.9f)  // Normal finish
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when {
                                    isNewWR -> "NEW WORLD RECORD!"
                                    isNewPB -> "NEW PERSONAL BEST!"
                                    else -> "FINISH"
                                },
                                fontFamily = pixelFont,
                                fontSize = 12.sp,
                                color = if (isNewWR || isNewPB) Color.Black else Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatLapTime(lapTimeMs),
                                fontFamily = pixelFont,
                                fontSize = 20.sp,
                                color = if (isNewWR || isNewPB) Color.Black else Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            // Sector summary row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 3) {
                                    val sectorTime = sectorTimes[i]
                                    val wrTime = originalWrSectorTimes[i]  // Use original WR times for delta
                                    val pbTime = originalBestSectorTimes[i] // Use original PB times for fallback
                                    val result = sectorResults[i]
                                    
                                    // Calculate delta comparison reference (Priority: WR -> PB)
                                    val hasValidSectorTime = sectorTime > 0
                                    val hasValidWrTime = wrTime < Long.MAX_VALUE
                                    val hasValidPbTime = pbTime < Long.MAX_VALUE
                                    
                                    val comparisonTime = if (hasValidWrTime) wrTime else if (hasValidPbTime) pbTime else Long.MAX_VALUE
                                    val hasValidReference = comparisonTime < Long.MAX_VALUE
                                    
                                    val delta = if (hasValidSectorTime && hasValidReference) {
                                        sectorTime - comparisonTime
                                    } else null
                                    
                                    val sectorColor = when (result) {
                                        2 -> Color(0xFF9933FF)  // Purple - WR
                                        1 -> Color(0xFF00FF00)  // Green - PB
                                        else -> Color(0xFFFFAA00)  // Yellow - slower
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .background(sectorColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "S${i + 1}",
                                            fontFamily = pixelFont,
                                            fontSize = 7.sp,
                                            color = if (isNewWR || isNewPB) Color.Black else Color.White
                                        )
                                        Text(
                                            text = when {
                                                !hasValidSectorTime -> "---"  // Sector not completed
                                                !hasValidReference -> "NEW"  // No reference (WR or PB) existed
                                                delta != null && delta > 0 -> "+${String.format("%.2f", delta / 1000.0)}"
                                                delta != null && delta < 0 -> String.format("%.2f", delta / 1000.0)
                                                else -> "0.00"  // Exactly equal (rare)
                                            },
                                            fontFamily = pixelFont,
                                            fontSize = 8.sp,
                                            color = if (isNewWR || isNewPB) Color.Black else sectorColor
                                        )
                                    }
                                }
                            }
                            
                            // E1 Enhancement: Theoretical Best
                            val canCalcTheoretical = bestSectorTimes.none { it == Long.MAX_VALUE }
                            if (canCalcTheoretical) {
                                val theoreticalBest = bestSectorTimes.sum()
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "THEORETICAL BEST: ${formatLapTime(theoreticalBest)}",
                                    fontFamily = pixelFont,
                                    fontSize = 10.sp,
                                    color = if (isNewWR) Color.Black else Color.Gray
                                )
                            }
                        }
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
                
                // Settings overlay (empty for now)
                if (showSettings) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SETTINGS",
                                fontFamily = pixelFont,
                                fontSize = 12.sp,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(Modifier.height(16.dp))

                            // Difficulty Filter
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filters = listOf("ALL" to null, "EASY" to 1, "MED" to 3, "HARD" to 5)
                                filters.forEach { (label, diff) ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (difficultyFilter == diff) Color.White else Color.DarkGray,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { difficultyFilter = diff }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(label, fontFamily = pixelFont, fontSize = 8.sp, color = if (difficultyFilter == diff) Color.Black else Color.White)
                                    }
                                }
                            }

                            // Track Selection List
                            Text("SELECT TRACK", fontFamily = pixelFont, fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                val filteredTracks = TrackFactory.tracks.filter { track ->
                                    when (difficultyFilter) {
                                        null -> true
                                        1 -> track.difficulty <= 2
                                        3 -> track.difficulty == 3
                                        5 -> track.difficulty >= 4
                                        else -> true
                                    }
                                }
                                
                                itemsIndexed(filteredTracks) { _, track ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (track.id == trackId) Color.White else Color(0xFF333333),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                trackId = track.id
                                                prefs.edit().putString("last_track_id", track.id).apply()
                                                // Reset Game State on track change
                                                gameState = HotlapState.READY
                                                sectorResults = listOf(-1, -1, -1)
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = track.name,
                                                    fontFamily = pixelFont,
                                                    fontSize = 8.sp,
                                                    color = if (track.id == trackId) Color.Black else Color.White
                                                )
                                                Text(
                                                    text = "*".repeat(track.difficulty),
                                                    fontSize = 8.sp,
                                                    color = if (track.id == trackId) Color.DarkGray else Color.Gray
                                                )
                                            }
                                            if (track.id == trackId) {
                                                Text("SELECTED", fontSize = 8.sp, color = Color(0xFF00AA00), fontFamily = pixelFont)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Sound Toggle
                            Spacer(Modifier.height(16.dp))
                            
                            // Sound Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (soundEnabled) Color(0xFF00AA00) else Color(0xFF666666),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { 
                                        soundEnabled = !soundEnabled
                                        prefs.edit().putBoolean("sound_enabled", soundEnabled).apply()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "SFX: ${if (soundEnabled) "ON" else "OFF"}",
                                    fontFamily = pixelFont,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                            
                            // Engine Sound Toggle (Sub-menu)
                            if (soundEnabled) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            if (engineSoundEnabled) Color(0xFF008800) else Color(0xFF555555),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { 
                                            engineSoundEnabled = !engineSoundEnabled
                                            prefs.edit().putBoolean("engine_sound_enabled", engineSoundEnabled).apply()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "ENGINE: ${if (engineSoundEnabled) "ON" else "OFF"}",
                                        fontFamily = pixelFont,
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(0.3f))
        
        // Action Bar: Ghost Toggles + Rank/Leaderboard + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GHOSTS Container with ME/WR toggles inside
            Row(
                modifier = Modifier
                    .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GHOSTS",
                    fontFamily = pixelFont,
                    fontSize = 7.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                // ME toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (myGhostEnabled) Color(0xFF00FF00) else Color(0xFF555555),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { myGhostEnabled = !myGhostEnabled }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PB",
                        fontFamily = pixelFont,
                        fontSize = 8.sp,
                        color = Color.White
                    )
                }
                
                // WR toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (worldGhostEnabled) Color(0xFFFFD700) else Color(0xFF555555),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { worldGhostEnabled = !worldGhostEnabled }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WR",
                        fontFamily = pixelFont,
                        fontSize = 8.sp,
                        color = if (worldGhostEnabled) Color.Black else Color.White
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Settings Button
            Box(
                modifier = Modifier
                    .background(
                        if (showSettings) Color(0xFFFF6600) else Color(0xFF444444),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { 
                        showLeaderboard = false  // Close leaderboard when opening settings
                        showSettings = !showSettings 
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SETTINGS", fontFamily = pixelFont, fontSize = 8.sp, color = Color.White)
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
                        when {
                            !sectorTimesLoaded -> Color.Gray  // B7 Fix: Gray while loading
                            gameState == HotlapState.READY -> Color(0xFF00AA00)
                            gameState == HotlapState.PLAYING -> Color.Black // Delta Display BG
                            gameState == HotlapState.DNF -> Color(0xFFFFAA00)
                            gameState == HotlapState.FINISHED -> Color(0xFF00AA00)
                            else -> Color.Gray
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(4.dp))
                    .pointerInput(gameState, sectorTimesLoaded) {  // B7 Fix: Also depend on sectorTimesLoaded
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed }) {
                                    when (gameState) {
                                        HotlapState.READY -> {
                                            if (sectorTimesLoaded) {  // B7 Fix: Only start if data is loaded
                                                // Capture reference times imperatively
                                                originalWrSectorTimes = wrSectorTimes.toList()
                                                originalBestSectorTimes = bestSectorTimes.toList()
                                                gameState = HotlapState.COUNTDOWN
                                            }
                                        }
                                        HotlapState.DNF, HotlapState.FINISHED -> {
                                            if (sectorTimesLoaded) {  // B7 Fix: Only retry if data is loaded
                                                carPathIndex = 0f
                                                lapTimeMs = 0L
                                                // Capture reference times imperatively
                                                originalWrSectorTimes = wrSectorTimes.toList()
                                                originalBestSectorTimes = bestSectorTimes.toList()
                                                gameState = HotlapState.COUNTDOWN
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (gameState == HotlapState.PLAYING) {
                     val d = liveDelta ?: 0L
                     val sign = if (d > 0) "+" else ""
                     val deltaStr = "$sign${String.format("%.2f", d / 1000.0)}"
                     val deltaColor = if (d < 0) Color(0xFF00E676) else Color(0xFFFF5252)
                     
                     Column(
                         horizontalAlignment = Alignment.CenterHorizontally,
                         verticalArrangement = Arrangement.Center
                     ) {
                         Text(
                             text = "DELTA",
                             fontFamily = pixelFont,
                             fontSize = 8.sp,
                             color = Color.Gray
                         )
                         if (liveDelta != null) {
                             Text(
                                 text = deltaStr,
                                 fontFamily = pixelFont,
                                 fontSize = 16.sp,
                                 color = deltaColor
                             )
                         } else {
                             Text(
                                 text = "GO",
                                 fontFamily = pixelFont,
                                 fontSize = 16.sp,
                                 color = Color.DarkGray
                             )
                         }
                     }
                } else {
                    Text(
                        text = when {
                            !sectorTimesLoaded -> "LOADING..."
                            gameState == HotlapState.READY -> "START"
                            gameState == HotlapState.DNF || gameState == HotlapState.FINISHED -> "RETRY"
                            else -> "GO"
                        },
                        fontFamily = pixelFont,
                        fontSize = if (!sectorTimesLoaded) 8.sp else 10.sp,
                        color = Color.White
                    )
                }
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
    lapTimeMs: Long,
    sectorResults: List<Int>  // -1=pending, 0=yellow, 1=green, 2=purple
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val padding = 20.dp.toPx()
        val maxCanvasSize = 800.dp.toPx()  // Cap for consistent visual speed across devices
        val canvasSize = minOf(size.minDimension - padding * 2, maxCanvasSize)
        
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
        
        // Draw colored sector overlays
        // B5 Fix: Calculate indices based on distance
        val s1End = if (track.totalDistance > 0 && track.pointDistances.isNotEmpty()) 
            track.pointDistances.indexOfFirst { it >= track.totalDistance * 0.33f }.let { if (it == -1) screenPoints.size / 3 else it }
            else screenPoints.size / 3
            
        val s2End = if (track.totalDistance > 0 && track.pointDistances.isNotEmpty())
            track.pointDistances.indexOfFirst { it >= track.totalDistance * 0.66f }.let { if (it == -1) (screenPoints.size / 3) * 2 else it }
            else (screenPoints.size / 3) * 2

        for (sector in 0 until 3) {
            val result = sectorResults[sector]
            if (result >= 0) {  // Only draw if sector is complete
                val sectorColor = when (result) {
                    2 -> Color(0xFF9933FF).copy(alpha = 0.4f)  // Purple - WR
                    1 -> Color(0xFF00FF00).copy(alpha = 0.4f)  // Green - PB
                    else -> Color(0xFFFFAA00).copy(alpha = 0.4f)  // Yellow - slower
                }
                
                val startIdx = when(sector) { 0 -> 0; 1 -> s1End; else -> s2End }
                val endIdx = when(sector) { 0 -> s1End; 1 -> s2End; else -> screenPoints.size - 1 }
                
                if (startIdx < screenPoints.size && endIdx > startIdx) {
                    val sectorPath = Path().apply {
                        moveTo(screenPoints[startIdx].x, screenPoints[startIdx].y)
                        for (i in (startIdx + 1)..endIdx) {
                            if (i < screenPoints.size) {
                                lineTo(screenPoints[i].x, screenPoints[i].y)
                            }
                        }
                        // Close the loop for the last sector
                        if (sector == 2 && screenPoints.isNotEmpty()) {
                            lineTo(screenPoints[0].x, screenPoints[0].y)
                        }
                    }
                    drawPath(sectorPath, sectorColor, 
                        style = Stroke(width = trackWidth * canvasSize * 1.8f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
                }
            }
        }
        
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
                    
                    // Front Wing
                    drawRect(Color(0x4400AAFF), Offset(12 * px, 0 * px), androidx.compose.ui.geometry.Size(40 * px, 4 * px))
                    
                    // Front Suspension
                    drawLine(Color(0x660066DD), Offset(20 * px, 12 * px), Offset(28 * px, 14 * px), strokeWidth = 1 * px)
                    drawLine(Color(0x660066DD), Offset(44 * px, 12 * px), Offset(36 * px, 14 * px), strokeWidth = 1 * px)
                    
                    // Front Wheels
                    drawRect(Color(0x660066DD), Offset(12 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    drawRect(Color(0x660066DD), Offset(44 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    
                    // Nose & Cockpit
                    drawRect(Color(0x550088FF), Offset(28 * px, 4 * px), androidx.compose.ui.geometry.Size(8 * px, 20 * px))
                    drawRect(Color(0x550088FF), Offset(26 * px, 24 * px), androidx.compose.ui.geometry.Size(12 * px, 10 * px))
                    
                    // Sidepods
                    drawRect(Color(0x550088FF), Offset(18 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    drawRect(Color(0x550088FF), Offset(38 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    
                    // Engine Cover
                    drawRect(Color(0x550066DD), Offset(28 * px, 34 * px), androidx.compose.ui.geometry.Size(8 * px, 18 * px))
                    
                    // Driver Helmet
                    drawCircle(Color(0x55FFFF00), radius = 3 * px, center = Offset(32 * px, 28 * px))
                    
                    // Rear Suspension
                    drawLine(Color(0x660066DD), Offset(18 * px, 46 * px), Offset(28 * px, 44 * px), strokeWidth = 1 * px)
                    drawLine(Color(0x660066DD), Offset(46 * px, 46 * px), Offset(36 * px, 44 * px), strokeWidth = 1 * px)
                    
                    // Rear Wheels
                    drawRect(Color(0x660066DD), Offset(10 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    drawRect(Color(0x660066DD), Offset(44 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    
                    // Rear Wing
                    drawRect(Color(0x4400AAFF), Offset(12 * px, 56 * px), androidx.compose.ui.geometry.Size(40 * px, 6 * px))
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
                    
                    // Front Wing
                    drawRect(Color(0x44FFD700), Offset(12 * px, 0 * px), androidx.compose.ui.geometry.Size(40 * px, 4 * px))
                    
                    // Front Suspension
                    drawLine(Color(0x66FF8800), Offset(20 * px, 12 * px), Offset(28 * px, 14 * px), strokeWidth = 1 * px)
                    drawLine(Color(0x66FF8800), Offset(44 * px, 12 * px), Offset(36 * px, 14 * px), strokeWidth = 1 * px)
                    
                    // Front Wheels
                    drawRect(Color(0x66FF8800), Offset(12 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    drawRect(Color(0x66FF8800), Offset(44 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    
                    // Nose & Cockpit
                    drawRect(Color(0x55FFAA00), Offset(28 * px, 4 * px), androidx.compose.ui.geometry.Size(8 * px, 20 * px))
                    drawRect(Color(0x55FFAA00), Offset(26 * px, 24 * px), androidx.compose.ui.geometry.Size(12 * px, 10 * px))
                    
                    // Sidepods
                    drawRect(Color(0x55FFAA00), Offset(18 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    drawRect(Color(0x55FFAA00), Offset(38 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    
                    // Engine Cover
                    drawRect(Color(0x55FF8800), Offset(28 * px, 34 * px), androidx.compose.ui.geometry.Size(8 * px, 18 * px))
                    
                    // Driver Helmet
                    drawCircle(Color(0x55FFFF00), radius = 3 * px, center = Offset(32 * px, 28 * px))
                    
                    // Rear Suspension
                    drawLine(Color(0x66FF8800), Offset(18 * px, 46 * px), Offset(28 * px, 44 * px), strokeWidth = 1 * px)
                    drawLine(Color(0x66FF8800), Offset(46 * px, 46 * px), Offset(36 * px, 44 * px), strokeWidth = 1 * px)
                    
                    // Rear Wheels
                    drawRect(Color(0x66FF8800), Offset(10 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    drawRect(Color(0x66FF8800), Offset(44 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    
                    // Rear Wing
                    drawRect(Color(0x44FFD700), Offset(12 * px, 56 * px), androidx.compose.ui.geometry.Size(40 * px, 6 * px))
                }
            }
        }
        
        // Player car
        if (gameState == HotlapState.PLAYING || gameState == HotlapState.COUNTDOWN) {
            val screenCar = Offset(padding + carPosition.x * canvasSize, padding + carPosition.y * canvasSize)
            val px = 0.4f.dp.toPx()
            
            rotate(degrees = (carAngle * 180f / kotlin.math.PI.toFloat()) + 90f, pivot = screenCar) {
                translate(left = screenCar.x - 32 * px, top = screenCar.y - 32 * px) {
                    // Front Wing
                    drawRect(Color.White, Offset(12 * px, 0 * px), androidx.compose.ui.geometry.Size(40 * px, 4 * px))
                    
                    // Front Suspension
                    drawLine(Color.Black, Offset(20 * px, 12 * px), Offset(28 * px, 14 * px), strokeWidth = 1 * px)
                    drawLine(Color.Black, Offset(44 * px, 12 * px), Offset(36 * px, 14 * px), strokeWidth = 1 * px)
                    
                    // Front Wheels
                    drawRect(Color.Black, Offset(12 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    drawRect(Color.Black, Offset(44 * px, 6 * px), androidx.compose.ui.geometry.Size(8 * px, 12 * px))
                    
                    // Nose & Cockpit
                    drawRect(Color(0xFFE74C3C), Offset(28 * px, 4 * px), androidx.compose.ui.geometry.Size(8 * px, 20 * px))
                    drawRect(Color(0xFFE74C3C), Offset(26 * px, 24 * px), androidx.compose.ui.geometry.Size(12 * px, 10 * px))
                    
                    // Sidepods
                    drawRect(Color(0xFFE74C3C), Offset(18 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    drawRect(Color(0xFFE74C3C), Offset(38 * px, 22 * px), androidx.compose.ui.geometry.Size(8 * px, 24 * px))
                    
                    // Engine Cover
                    drawRect(Color(0xFFC0392B), Offset(28 * px, 34 * px), androidx.compose.ui.geometry.Size(8 * px, 18 * px))
                    
                    // Driver Helmet
                    drawCircle(Color.Yellow, radius = 3 * px, center = Offset(32 * px, 28 * px))
                    
                    // Rear Suspension
                    drawLine(Color.Black, Offset(18 * px, 46 * px), Offset(28 * px, 44 * px), strokeWidth = 1 * px)
                    drawLine(Color.Black, Offset(46 * px, 46 * px), Offset(36 * px, 44 * px), strokeWidth = 1 * px)
                    
                    // Rear Wheels
                    drawRect(Color.Black, Offset(10 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    drawRect(Color.Black, Offset(44 * px, 40 * px), androidx.compose.ui.geometry.Size(10 * px, 14 * px))
                    
                    // Rear Wing
                    drawRect(Color.White, Offset(12 * px, 56 * px), androidx.compose.ui.geometry.Size(40 * px, 6 * px))
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
    // Sample every 5th frame to reduce storage - B3 Fix: Match Firebase upload rate (5)
    data.filterIndexed { index, _ -> index % 5 == 0 }.forEach { frame ->
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
        
        // B5 Fix: Calculate cumulative distances
        val pointDistances = mutableListOf<Float>()
        var currentDist = 0f
        pointDistances.add(0f)
        
        for (i in 0 until normalizedPoints.size - 1) {
            val dist = distance(normalizedPoints[i], normalizedPoints[i+1])
            currentDist += dist
            pointDistances.add(currentDist)
        }
        // Add closing segment for total distance
        if (normalizedPoints.isNotEmpty()) {
            currentDist += distance(normalizedPoints.last(), normalizedPoints.first())
        }
        
        TrackData(
            name = properties.optString("Name", "Unknown"),
            location = properties.optString("Location", ""),
            points = normalizedPoints,
            totalDistance = currentDist,
            pointDistances = pointDistances
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun formatLapTime(ms: Long): String {
    val totalSeconds = ms / 1000.0
    return String.format("%.2fs", totalSeconds)
}
