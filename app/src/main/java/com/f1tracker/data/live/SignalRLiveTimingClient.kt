package com.f1tracker.data.live

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Public Data Models (UI) ---

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class LiveDriver(
    val position: String? = null,
    val positionDisplay: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val tla: String? = null, // 3-letter code from API (e.g., "VER", "HAM")
    val externalTeam: String? = null,
    val teamColour: String? = null, // Hex color from API (e.g., "4781D7")
    val raceTime: String? = null,
    val gap: String? = null,
    val interval: String? = null,
    val laps: String? = null,
    val pits: String? = null,
    val sectors: List<String> = emptyList(),
    val sectorColors: List<Int> = emptyList(),
    val miniSectors: List<List<Int>> = emptyList(), // Per-sector segment statuses for live progress [S1[segs], S2[segs], S3[segs]]
    val status: Int = 0,
    val inPit: Boolean = false,
    val pitOut: Boolean = false,
    val tyreCompound: String? = null, // SOFT, MEDIUM, HARD, INTERMEDIATE, WET
    val tyreAge: Int? = null,
    val gridPos: String? = null,
    val knockedOut: Boolean = false, // Qualifying: eliminated
    val cutoff: Boolean = false, // At cutoff line
    val bestLapTimes: List<String> = emptyList() // Per Q-session best laps [Q1, Q2, Q3]
)

// --- Internal F1 Data Models (JSON Parsing) ---
data class F1NegotiateResponse(
    @SerializedName("Url") val url: String,
    @SerializedName("ConnectionToken") val connectionToken: String,
    @SerializedName("ConnectionId") val connectionId: String,
    @SerializedName("KeepAliveTimeout") val keepAliveTimeout: Double?,
    @SerializedName("DisconnectTimeout") val disconnectTimeout: Double?,
    @SerializedName("TryWebSockets") val tryWebSockets: Boolean?,
    @SerializedName("ProtocolVersion") val protocolVersion: String?,
    @SerializedName("TransportConnectTimeout") val transportConnectTimeout: Double?,
    @SerializedName("LongPollDelay") val longPollDelay: Double?
)

data class F1SignalRMessage(
    @SerializedName("C") val c: String? = null, // Message ID
    @SerializedName("M") val m: List<F1HubMessage>? = null, // Messages
    @SerializedName("I") val i: String? = null, // Keepalive index
    @SerializedName("R") val r: JsonElement? = null // Initial response (full state on subscribe)
)

data class F1HubMessage(
    @SerializedName("H") val hub: String,
    @SerializedName("M") val method: String,
    @SerializedName("A") val args: List<JsonElement>  // Changed from JsonObject to JsonElement to handle primitives
)

// --- Driver Database (reads from F1DataProvider, falls back to API DriverList) ---
object DriverDatabase {
    data class DriverInfo(val firstName: String, val lastName: String, val team: String, val tla: String? = null, val teamColour: String? = null)

    // API DriverList data - populated at runtime from live timing feed
    // Key: racing number, Value: driver info from API
    val apiDriverInfo = mutableMapOf<String, DriverInfo>()

    /**
     * Get driver by racing number. 
     * Priority: 1) API DriverList (always current) 2) Local JSON (may be outdated)
     */
    fun getDriver(number: String): DriverInfo {
        // First try API data (most current, includes mid-season changes)
        apiDriverInfo[number]?.let { return it }
        
        // Fallback to local JSON
        val driver = com.f1tracker.data.local.F1DataProvider.getDriverByRacingNumber(number)
        return if (driver != null) {
            val teamDisplayName = com.f1tracker.data.local.F1DataProvider.getTeamByApiId(driver.team)?.displayName 
                ?: driver.team.replace("_", " ").replaceFirstChar { it.uppercase() }
            DriverInfo(driver.givenName, driver.familyName, teamDisplayName, driver.code)
        } else {
            DriverInfo("Driver", "#$number", "Unknown Team")
        }
    }
}

class SignalRLiveTimingClient {

    private val TAG = "SignalRLiveTiming"
    private val BASE_URL = "https://livetiming.formula1.com/signalr"
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val okHttpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _liveDrivers = MutableStateFlow<List<LiveDriver>>(emptyList())
    val liveDrivers: StateFlow<List<LiveDriver>> = _liveDrivers

    private val _sessionName = MutableStateFlow<String>("")
    val sessionName: StateFlow<String> = _sessionName

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    private val _currentLap = MutableStateFlow(0)
    val currentLap: StateFlow<Int> = _currentLap

    private val _totalLaps = MutableStateFlow(0)
    val totalLaps: StateFlow<Int> = _totalLaps

    private val _rawData = MutableStateFlow<String>("")
    val rawData: StateFlow<String> = _rawData
    
    // Map to track drivers by racing number for incremental updates
    private val driverMap = mutableMapOf<String, LiveDriver>()
    
    // Reconnection state
    private var reconnectAttempts = 0
    private var isReconnecting = false
    private var lastMessageTime = System.currentTimeMillis()

    fun connect() {
        scope.launch {
            try {
                Log.d(TAG, "🔌 Starting negotiation with F1 SignalR...")
                // Only show connecting UI if we don't have data yet
                // If we have data, reconnect silently in background
                val hasData = driverMap.isNotEmpty()
                if (!hasData) {
                    _isConnected.value = false
                    _connectionStatus.value = if (isReconnecting) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING
                }
                _connectionError.value = null

                // 1. Negotiate
                val negotiateUrl = "$BASE_URL/negotiate?clientProtocol=1.5&connectionData=%5B%7B%22name%22%3A%22Streaming%22%7D%5D"
                val request = Request.Builder()
                    .url(negotiateUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Negotiation failed: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty negotiation response")
                val negotiateData = gson.fromJson(responseBody, F1NegotiateResponse::class.java)
                val token = negotiateData.connectionToken
                
                // Extract cookies
                val cookies = response.headers("Set-Cookie")
                val cookieHeader = cookies.joinToString("; ") { it.substringBefore(";") }

                Log.d(TAG, "✅ Negotiation successful. Token: ${token.take(20)}...")

                // 2. Connect WebSocket
                val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                val encodedData = URLEncoder.encode("[{\"name\":\"Streaming\"}]", StandardCharsets.UTF_8.toString())
                val wsUrl = "$BASE_URL/connect?transport=webSockets&clientProtocol=1.5&connectionToken=$encodedToken&connectionData=$encodedData"

                val wsRequest = Request.Builder()
                    .url(wsUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Cookie", cookieHeader)
                    .build()

                webSocket = okHttpClient.newWebSocket(wsRequest, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "✅ WebSocket opened")
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        reconnectAttempts = 0
                        isReconnecting = false
                        lastMessageTime = System.currentTimeMillis()
                        
                        // 3. Subscribe
                        val subscribeCmd = """{"H":"Streaming","M":"Subscribe","A":[["Heartbeat","TimingData","TimingAppData","DriverList","CarData.z","Position.z","SessionInfo","TrackStatus","LapCount","ExtrapolatedClock"]],"I":1}"""
                        webSocket.send(subscribeCmd)
                        Log.d(TAG, "📡 Sent Subscribe command")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        lastMessageTime = System.currentTimeMillis()
                        handleMessage(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closing: $code - $reason")
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        scheduleReconnect()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "WebSocket error: ${t.message}", t)
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _connectionError.value = t.message
                        scheduleReconnect()
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionError.value = e.message
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                scheduleReconnect()
            }
        }
    }
    
    private fun scheduleReconnect() {
        if (isReconnecting) {
            Log.d(TAG, "🔄 Already reconnecting, skipping duplicate scheduleReconnect")
            return
        }
        
        reconnectAttempts++
        isReconnecting = true
        // Exponential backoff capped at 30s — never give up, always auto-retry
        val delayMs = minOf(2000L * (1L shl (minOf(reconnectAttempts - 1, 4))), 30000L) // 2s, 4s, 8s, 16s, 30s max
        Log.d(TAG, "🔄 Reconnect attempt $reconnectAttempts in ${delayMs}ms")
        // Only show reconnecting status if we have no data
        if (driverMap.isEmpty()) {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            _connectionError.value = "Reconnecting... (attempt $reconnectAttempts)"
        }
        
        scope.launch {
            kotlinx.coroutines.delay(delayMs)
            // Close old socket cleanly
            try { webSocket?.close(1000, "Reconnecting") } catch (_: Exception) {}
            webSocket = null
            connect()
        }
    }
    
    // Called when app resumes from background — checks if connection is stale
    private var lastEnsureConnectedTime = 0L
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    
    fun ensureConnected() {
        val now = System.currentTimeMillis()
        
        // Cooldown: don't re-enter within 5 seconds
        if (now - lastEnsureConnectedTime < 5_000) return
        // Don't interfere if already reconnecting
        if (isReconnecting) return
        // Don't interfere if currently connecting
        if (_connectionStatus.value == ConnectionStatus.CONNECTING || _connectionStatus.value == ConnectionStatus.RECONNECTING) return
        
        lastEnsureConnectedTime = now
        
        val timeSinceLastMessage = now - lastMessageTime
        val isStale = timeSinceLastMessage > 30_000 // No messages for 30 seconds = stale
        
        if (!_isConnected.value || isStale) {
            Log.d(TAG, "🔄 ensureConnected: connected=${_isConnected.value}, stale=$isStale (${timeSinceLastMessage}ms since last msg)")
            reconnectAttempts = 0 // Reset attempts for manual/lifecycle reconnect
            isReconnecting = true
            try { webSocket?.close(1000, "Reconnecting") } catch (_: Exception) {}
            webSocket = null
            connect()
        }
        
        // Start a periodic heartbeat check if not already running
        startHeartbeat()
    }
    
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000) // Check every 15 seconds
                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
                if (timeSinceLastMessage > 30_000 && _isConnected.value && !isReconnecting) {
                    Log.d(TAG, "💓 Heartbeat: stale connection detected (${timeSinceLastMessage}ms). Reconnecting...")
                    reconnectAttempts = 0
                    isReconnecting = true
                    try { webSocket?.close(1000, "Stale") } catch (_: Exception) {}
                    webSocket = null
                    connect()
                }
            }
        }
    }

    private fun handleMessage(message: String) {
        try {
            val f1Msg = gson.fromJson(message, F1SignalRMessage::class.java)
            
            // Handle initial response (R field) - contains full state on subscribe
            if (f1Msg.r != null && f1Msg.r.isJsonObject) {
                Log.d(TAG, "📦 Received initial state response")
                processInitialState(f1Msg.r.asJsonObject)
            }
            
            // Handle Keepalive (empty message usually)
            if (f1Msg.m == null) {
                return
            }

            f1Msg.m.forEach { hubMsg ->
                if (hubMsg.method == "feed") {
                    val topicElement = hubMsg.args.getOrNull(0)
                    val dataElement = hubMsg.args.getOrNull(1)
                    
                    // Topic should be a string primitive
                    val topic = if (topicElement?.isJsonPrimitive == true) {
                        topicElement.asString
                    } else {
                        Log.w(TAG, "Topic is not a primitive: $topicElement")
                        null
                    }
                    
                    Log.d(TAG, "📥 Feed topic: $topic")

                    // Data should be a JsonObject for most topics
                    if (topic != null && dataElement != null && dataElement.isJsonObject) {
                        val data = dataElement.asJsonObject
                        
                        when (topic) {
                            "TimingData" -> {
                                Log.d(TAG, "⏱ Processing TimingData")
                                processTimingData(data)
                            }
                            "SessionInfo" -> {
                                // Use SessionInfo.Name for session type (e.g., "Practice 1", "Qualifying", "Race")
                                // NOT Meeting.Name which is the GP name (e.g., "Australian Grand Prix")
                                val sessionName = data.get("Name")?.asString
                                if (sessionName != null) {
                                    Log.d(TAG, "📋 Session name: $sessionName")
                                    _sessionName.value = sessionName
                                }
                            }
                            "LapCount" -> {
                                val current = data.get("CurrentLap")?.asInt
                                val total = data.get("TotalLaps")?.asInt
                                if (current != null) _currentLap.value = current
                                if (total != null) _totalLaps.value = total
                                Log.d(TAG, "🏁 Lap: $current/$total")
                            }
                            "DriverList" -> {
                                // DriverList contains position (Line) updates
                                processDriverList(data)
                            }
                            "TimingAppData" -> {
                                // TimingAppData contains tyre/stint info
                                processTimingAppData(data)
                            }
                        }
                    } else if (topic != null) {
                        Log.d(TAG, "⚠ Topic $topic data is not JsonObject: ${dataElement?.javaClass?.simpleName}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}")
        }
    }
    
    private fun processInitialState(response: JsonObject) {
        try {
            // The R response is an object where keys are topic names in subscribed order
            // e.g., {"Heartbeat": {...}, "TimingData": {...}, "DriverList": {...}, ...}
            Log.d(TAG, "📦 Initial state keys: ${response.keySet()}")
            
            // Process DriverList first (contains Line/position for all drivers)
            response.get("DriverList")?.let { driverListElement ->
                if (driverListElement.isJsonObject) {
                    val driverList = driverListElement.asJsonObject
                    Log.d(TAG, "📦 DriverList has ${driverList.entrySet().size} drivers")
                    processDriverList(driverList)
                }
            }
            
            // Then process TimingData (contains gaps, sectors, etc.)
            response.get("TimingData")?.let { timingElement ->
                if (timingElement.isJsonObject) {
                    val timingData = timingElement.asJsonObject
                    Log.d(TAG, "📦 TimingData initial state")
                    processTimingData(timingData)
                }
            }
            
            // Process TimingAppData (contains tyre compound and stint info)
            response.get("TimingAppData")?.let { appElement ->
                if (appElement.isJsonObject) {
                    val appData = appElement.asJsonObject
                    Log.d(TAG, "📦 TimingAppData initial state")
                    processTimingAppData(appData)
                }
            }
            
            // Process SessionInfo - use Name for session type (e.g., "Practice 1", "Qualifying", "Race")
            // NOT Meeting.Name which is the GP name (e.g., "Australian Grand Prix")
            response.get("SessionInfo")?.let { sessionElement ->
                if (sessionElement.isJsonObject) {
                    val sessionName = sessionElement.asJsonObject
                        .get("Name")?.asString
                    if (sessionName != null) {
                        Log.d(TAG, "📦 Session: $sessionName")
                        _sessionName.value = sessionName
                    }
                }
            }
            
            // Process LapCount
            response.get("LapCount")?.let { lapElement ->
                if (lapElement.isJsonObject) {
                    val current = lapElement.asJsonObject.get("CurrentLap")?.asInt
                    val total = lapElement.asJsonObject.get("TotalLaps")?.asInt
                    if (current != null) _currentLap.value = current
                    if (total != null) _totalLaps.value = total
                    Log.d(TAG, "📦 Lap: $current/$total")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing initial state: ${e.message}")
        }
    }

    private fun processTimingData(data: JsonObject) {
        try {
            val lines = data.getAsJsonObject("Lines")
            if (lines == null) {
                Log.d(TAG, "❌ No 'Lines' object in TimingData. Keys: ${data.keySet()}")
                return
            }
            
            var updated = false

            // Process each driver update (incremental/delta updates)
            lines.entrySet().forEach { entry ->
                val number = entry.key
                val lineData = entry.value.asJsonObject
                
                val driverInfo = DriverDatabase.getDriver(number)
                
                // Log fields in this update for debugging
                if (lineData.keySet().contains("Position") || driverMap[number]?.position == null) {
                    Log.d(TAG, "📋 Driver $number fields: ${lineData.keySet()}")
                }
                
                // Get existing driver or create new one
                val existing = driverMap[number] ?: LiveDriver(
                    firstName = driverInfo.firstName,
                    lastName = driverInfo.lastName,
                    tla = driverInfo.tla,
                    externalTeam = driverInfo.team,
                    teamColour = driverInfo.teamColour
                )
                
                // Extract fields - only update if present in this delta
                // Position can come as "Position" or "Line" field
                val positionFromData = lineData.get("Position")?.asString 
                    ?: lineData.get("Line")?.asString
                val position = positionFromData ?: existing.position
                val positionDisplay = position ?: existing.positionDisplay
                
                // Gaps - handle both object format (race) and array format (qualifying)
                // In qualifying, Stats is an array: [{Q1 stats}, {Q2 stats}, {Q3 stats}]
                // In race, Stats is an object: {TimeDiffToFastest: ..., TimeDifftoPositionAhead: ...}
                var gapToLeader = existing.gap
                var intervalToAhead = existing.interval
                
                val statsElement = lineData.get("Stats")
                if (statsElement != null) {
                    if (statsElement.isJsonObject) {
                        // Race format
                        gapToLeader = statsElement.asJsonObject.get("TimeDiffToFastest")?.asString ?: gapToLeader
                        intervalToAhead = statsElement.asJsonObject.get("TimeDifftoPositionAhead")?.asString ?: intervalToAhead
                    } else if (statsElement.isJsonArray) {
                        // Qualifying format - use the last non-empty Q session
                        val statsArray = statsElement.asJsonArray
                        for (i in statsArray.size() - 1 downTo 0) {
                            val qStats = statsArray[i]
                            if (qStats.isJsonObject) {
                                val gap = qStats.asJsonObject.get("TimeDiffToFastest")?.asString
                                val int = qStats.asJsonObject.get("TimeDifftoPositionAhead")?.asString
                                if (!gap.isNullOrEmpty()) {
                                    gapToLeader = gap
                                    intervalToAhead = int ?: intervalToAhead
                                    break
                                }
                            }
                        }
                    }
                } else {
                    // Direct field access (delta updates)
                    lineData.get("GapToLeader")?.asString?.let { gapToLeader = it }
                    lineData.get("IntervalToPositionAhead")?.let { intElement ->
                        if (intElement.isJsonObject) {
                            intElement.asJsonObject.get("Value")?.asString?.let { intervalToAhead = it }
                        }
                    }
                }

                // Best lap time - handle both object and array format
                var bestLap: String? = null
                var bestLapTimes = existing.bestLapTimes.toMutableList()
                val bestLapTimeElement = lineData.get("BestLapTime")
                if (bestLapTimeElement != null && bestLapTimeElement.isJsonObject) {
                    bestLap = bestLapTimeElement.asJsonObject.get("Value")?.asString
                }
                // BestLapTimes array (qualifying: per Q session)
                val bestLapTimesElement = lineData.get("BestLapTimes")
                if (bestLapTimesElement != null && bestLapTimesElement.isJsonArray) {
                    bestLapTimes = mutableListOf()
                    bestLapTimesElement.asJsonArray.forEach { elem ->
                        if (elem.isJsonObject) {
                            bestLapTimes.add(elem.asJsonObject.get("Value")?.asString ?: "")
                        } else {
                            bestLapTimes.add("")
                        }
                    }
                    // Use the best time from the latest Q session as display
                    if (bestLap == null) {
                        bestLap = bestLapTimes.lastOrNull { it.isNotEmpty() }
                    }
                }
                
                val lastLap = lineData.get("LastLapTime")?.asJsonObject?.get("Value")?.asString
                // Priority: BestLapTime > existing time > LastLapTime (only if no time set yet)
                // This prevents out-lap times (e.g. "10:23.163") from overwriting valid best laps
                val timeDisplay = if (!bestLap.isNullOrEmpty()) {
                    bestLap
                } else if (existing.raceTime != null) {
                    existing.raceTime
                } else if (!lastLap.isNullOrEmpty() && !lastLap.contains(":") || (lastLap?.substringBefore(":")?.toIntOrNull() ?: 99) < 3) {
                    // Only use LastLapTime if it looks like a valid lap (under 3 minutes)
                    lastLap
                } else {
                    null
                }
                
                val laps = lineData.get("NumberOfLaps")?.let { 
                    if (it.isJsonPrimitive) it.asString else null 
                } ?: existing.laps
                val pits = lineData.get("NumberOfPitStops")?.let {
                    if (it.isJsonPrimitive) it.asString else null
                } ?: existing.pits
                
                // KnockedOut/Cutoff flags (qualifying)
                val knockedOut = lineData.get("KnockedOut")?.asBoolean ?: existing.knockedOut
                val cutoff = lineData.get("Cutoff")?.asBoolean ?: existing.cutoff
                
                // Pit status
                val driverInPit = lineData.get("InPit")?.asBoolean ?: existing.inPit
                val driverPitOut = lineData.get("PitOut")?.asBoolean ?: existing.pitOut
                
                // Sectors - handle both object (delta) and array (initial state) format
                var sectorsList = existing.sectors.toMutableList()
                var sectorColorsList = existing.sectorColors.toMutableList()
                var miniSectorsList = existing.miniSectors.toMutableList()
                
                val sectorsElement = lineData.get("Sectors")
                if (sectorsElement != null) {
                    val sectorEntries = if (sectorsElement.isJsonObject) {
                        // Delta update format: {"0": {...}, "1": {...}}
                        sectorsElement.asJsonObject.entrySet().sortedBy { it.key.toIntOrNull() ?: 0 }
                    } else if (sectorsElement.isJsonArray) {
                        // Initial state format: [{...}, {...}, {...}]
                        sectorsElement.asJsonArray.mapIndexed { index, elem -> 
                            java.util.AbstractMap.SimpleEntry(index.toString(), elem) 
                        }
                    } else null
                    
                    sectorEntries?.forEach { sectorEntry ->
                        val sectorIndex = sectorEntry.key.toIntOrNull() ?: return@forEach
                        if (!sectorEntry.value.isJsonObject) return@forEach
                        val sectorObj = sectorEntry.value.asJsonObject
                        
                        // Get sector time value
                        val value = sectorObj.get("Value")?.asString ?: ""
                        
                        // Ensure lists are large enough
                        while (sectorsList.size <= sectorIndex) {
                            sectorsList.add("")
                            sectorColorsList.add(0)
                            miniSectorsList.add(emptyList())
                        }
                        
                        // Only update sector time if we have a non-empty value
                        if (value.isNotEmpty()) {
                            sectorsList[sectorIndex] = value
                        }
                        
                        // Sector color
                        var color = 0
                        if (sectorObj.get("OverallFastest")?.asBoolean == true) color = 2
                        else if (sectorObj.get("PersonalFastest")?.asBoolean == true) color = 1
                        sectorColorsList[sectorIndex] = color
                        
                        // Extract mini-sector Segments for live progress visualization
                        val segmentsElement = sectorObj.get("Segments")
                        if (segmentsElement != null) {
                            val segments = mutableListOf<Int>()
                            if (segmentsElement.isJsonArray) {
                                segmentsElement.asJsonArray.forEach { seg ->
                                    if (seg.isJsonObject) {
                                        segments.add(seg.asJsonObject.get("Status")?.asInt ?: 0)
                                    }
                                }
                            } else if (segmentsElement.isJsonObject) {
                                // Delta update: {"3": {"Status": 2048}}
                                val existingSegs = miniSectorsList.getOrNull(sectorIndex)?.toMutableList() ?: mutableListOf()
                                segmentsElement.asJsonObject.entrySet().forEach { segEntry ->
                                    val segIdx = segEntry.key.toIntOrNull() ?: return@forEach
                                    val segStatus = segEntry.value.asJsonObject?.get("Status")?.asInt ?: 0
                                    while (existingSegs.size <= segIdx) existingSegs.add(0)
                                    existingSegs[segIdx] = segStatus
                                }
                                segments.addAll(existingSegs)
                            }
                            if (segments.isNotEmpty()) {
                                miniSectorsList[sectorIndex] = segments
                            }
                        }
                    }
                }
                
                // Status (for race/general)
                val retired = lineData.get("Retired")?.asBoolean ?: (existing.status == 2)
                val stopped = lineData.get("Stopped")?.asBoolean ?: (existing.status == 3)
                
                var status = existing.status
                if (retired) status = 2
                else if (stopped) status = 3
                else if (driverInPit) status = 1
                else if (lineData.has("InPit") || lineData.has("Retired") || lineData.has("Stopped")) status = 0

                // Tyres (Stints)
                var compound = existing.tyreCompound
                var age = existing.tyreAge
                val stintsElement = lineData.get("Stints")
                if (stintsElement != null) {
                    val stintEntries = if (stintsElement.isJsonObject) {
                        stintsElement.asJsonObject.entrySet().sortedByDescending { it.key.toIntOrNull() ?: 0 }
                    } else if (stintsElement.isJsonArray && stintsElement.asJsonArray.size() > 0) {
                        listOf(java.util.AbstractMap.SimpleEntry("0", stintsElement.asJsonArray.last()))
                    } else null
                    
                    stintEntries?.firstOrNull()?.let { lastEntry ->
                        if (lastEntry.value.isJsonObject) {
                            val lastStint = lastEntry.value.asJsonObject
                            compound = lastStint.get("Compound")?.asString ?: compound
                            age = lastStint.get("TotalLaps")?.asInt ?: lastStint.get("Laps")?.asInt ?: age
                        }
                    }
                }

                val updatedDriver = LiveDriver(
                    position = position,
                    positionDisplay = positionDisplay,
                    firstName = driverInfo.firstName,
                    lastName = driverInfo.lastName,
                    tla = driverInfo.tla ?: existing.tla,
                    externalTeam = driverInfo.team,
                    teamColour = driverInfo.teamColour ?: existing.teamColour,
                    raceTime = timeDisplay,
                    gap = gapToLeader,
                    interval = intervalToAhead,
                    laps = laps,
                    pits = pits,
                    sectors = sectorsList,
                    sectorColors = sectorColorsList,
                    miniSectors = miniSectorsList,
                    status = status,
                    inPit = driverInPit,
                    pitOut = driverPitOut,
                    tyreCompound = compound,
                    tyreAge = age,
                    knockedOut = knockedOut,
                    cutoff = cutoff,
                    bestLapTimes = bestLapTimes
                )
                
                driverMap[number] = updatedDriver
                updated = true
            }

            // Convert map to sorted list and update UI
            if (updated) {
                val sortedList = driverMap.values
                    .filter { it.position != null }  // Only show drivers with known positions
                    .sortedBy { it.position?.toIntOrNull() ?: 999 }
                
                _liveDrivers.value = sortedList
                Log.d(TAG, "✅ liveDrivers updated: ${sortedList.size} drivers with positions (${driverMap.size} total tracked)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing timing data: ${e.message}", e)
        }
    }
    
    private fun processDriverList(data: JsonObject) {
        try {
            var updated = false
            
            // DriverList contains driver number -> {Line: position, Tla, BroadcastName, TeamName, TeamColour, ...}
            data.entrySet().forEach { entry ->
                val number = entry.key
                val driverData = entry.value
                
                if (driverData.isJsonObject) {
                    val driverObj = driverData.asJsonObject
                    
                    // Extract API driver info and store it for lookups
                    val tla = driverObj.get("Tla")?.asString
                    val firstName = driverObj.get("FirstName")?.asString
                    val lastName = driverObj.get("LastName")?.asString
                    val teamName = driverObj.get("TeamName")?.asString
                    val teamColour = driverObj.get("TeamColour")?.asString
                    
                    // Store in DriverDatabase for fallback lookups
                    if (firstName != null && lastName != null && teamName != null) {
                        DriverDatabase.apiDriverInfo[number] = DriverDatabase.DriverInfo(
                            firstName = firstName,
                            lastName = lastName,
                            team = teamName,
                            tla = tla,
                            teamColour = teamColour
                        )
                        Log.d(TAG, "📋 API Driver #$number: $tla ($firstName $lastName) - $teamName")
                    }
                    
                    val lineValue = driverObj.get("Line")?.let {
                        if (it.isJsonPrimitive) it.asString else null
                    }
                    if (lineValue != null) {
                        val existing = driverMap[number]
                        val driverInfo = DriverDatabase.getDriver(number)
                        if (existing != null) {
                            // Update position and any new API info
                            driverMap[number] = existing.copy(
                                position = lineValue,
                                positionDisplay = lineValue,
                                firstName = driverInfo.firstName,
                                lastName = driverInfo.lastName,
                                tla = driverInfo.tla ?: existing.tla,
                                externalTeam = driverInfo.team,
                                teamColour = driverInfo.teamColour ?: existing.teamColour
                            )
                            updated = true
                        } else {
                            // Create new driver with position and API info
                            driverMap[number] = LiveDriver(
                                position = lineValue,
                                positionDisplay = lineValue,
                                firstName = driverInfo.firstName,
                                lastName = driverInfo.lastName,
                                tla = driverInfo.tla,
                                externalTeam = driverInfo.team,
                                teamColour = driverInfo.teamColour
                            )
                            updated = true
                            Log.d(TAG, "📍 New driver: $number (${driverInfo.tla ?: driverInfo.lastName}) -> P$lineValue")
                        }
                    }
                }
            }
            
            if (updated) {
                val sortedList = driverMap.values
                    .filter { it.position != null }
                    .sortedBy { it.position?.toIntOrNull() ?: 999 }
                _liveDrivers.value = sortedList
                Log.d(TAG, "✅ DriverList: ${sortedList.size} drivers with positions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing DriverList: ${e.message}")
        }
    }
    
    private fun processTimingAppData(data: JsonObject) {
        try {
            val lines = data.getAsJsonObject("Lines") ?: data
            var updated = false
            
            lines.entrySet().forEach { entry ->
                val number = entry.key
                val lineData = entry.value
                
                if (lineData.isJsonObject) {
                    val driverData = lineData.asJsonObject
                    val existing = driverMap[number] ?: return@forEach
                    
                    // GridPos
                    var gridPos = existing.gridPos
                    driverData.get("GridPos")?.asString?.let {
                         gridPos = it
                    }
                    
                    // Process Stints for tyre info
                    val stintsElement = driverData.get("Stints")
                    if (stintsElement != null) {
                        var compound: String? = existing.tyreCompound
                        var age: Int? = existing.tyreAge
                        
                        // Stints can be an array or object with numeric keys
                        val lastStint = if (stintsElement.isJsonArray && stintsElement.asJsonArray.size() > 0) {
                            stintsElement.asJsonArray.last().asJsonObject
                        } else if (stintsElement.isJsonObject) {
                            // Get the highest numbered stint (most recent)
                            val stintEntries = stintsElement.asJsonObject.entrySet()
                                .sortedByDescending { it.key.toIntOrNull() ?: 0 }
                            stintEntries.firstOrNull()?.value?.asJsonObject
                        } else null
                        
                        lastStint?.let { stint ->
                            stint.get("Compound")?.asString?.let { c ->
                                compound = c
                            }
                            stint.get("TotalLaps")?.asInt?.let { l ->
                                age = l
                            }
                        }
                        
                        if (compound != existing.tyreCompound || age != existing.tyreAge || gridPos != existing.gridPos) {
                            driverMap[number] = existing.copy(
                                tyreCompound = compound,
                                tyreAge = age,
                                gridPos = gridPos
                            )
                            updated = true
                        }
                    } else if (gridPos != existing.gridPos) {
                        // Update if only GridPos changed
                        driverMap[number] = existing.copy(
                            gridPos = gridPos
                        )
                        updated = true
                    }
                }
            }
            
            if (updated) {
                val sortedList = driverMap.values
                    .filter { it.position != null }
                    .sortedBy { it.position?.toIntOrNull() ?: 999 }
                _liveDrivers.value = sortedList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing TimingAppData: ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _isConnected.value = false
    }

    companion object {
        @Volatile
        private var instance: SignalRLiveTimingClient? = null

        fun getInstance(): SignalRLiveTimingClient {
            return instance ?: synchronized(this) {
                instance ?: SignalRLiveTimingClient().also { instance = it }
            }
        }
    }
}
