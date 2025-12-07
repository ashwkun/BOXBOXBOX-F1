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
data class LiveDriver(
    val position: String? = null,
    val positionDisplay: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val externalTeam: String? = null,
    val raceTime: String? = null,
    val gap: String? = null,
    val interval: String? = null,
    val laps: String? = null,
    val pits: String? = null,
    val sectors: List<String> = emptyList(),
    val sectorColors: List<Int> = emptyList(),
    val status: Int = 0,
    val tyreCompound: String? = null, // SOFT, MEDIUM, HARD, INTERMEDIATE, WET
    val tyreAge: Int? = null,
    val gridPos: String? = null
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

// --- Driver Database (2025 Season Grid) ---
object DriverDatabase {
    data class DriverInfo(val firstName: String, val lastName: String, val team: String)

    private val drivers = mapOf(
        // Red Bull Racing
        "1" to DriverInfo("Max", "Verstappen", "Red Bull Racing"),
        "22" to DriverInfo("Yuki", "Tsunoda", "Red Bull Racing"),
        // Ferrari
        "16" to DriverInfo("Charles", "Leclerc", "Ferrari"),
        "44" to DriverInfo("Lewis", "Hamilton", "Ferrari"),
        // Mercedes
        "63" to DriverInfo("George", "Russell", "Mercedes"),
        "12" to DriverInfo("Andrea Kimi", "Antonelli", "Mercedes"),
        // McLaren
        "4" to DriverInfo("Lando", "Norris", "McLaren"),
        "81" to DriverInfo("Oscar", "Piastri", "McLaren"),
        // Aston Martin
        "14" to DriverInfo("Fernando", "Alonso", "Aston Martin"),
        "18" to DriverInfo("Lance", "Stroll", "Aston Martin"),
        // Alpine
        "10" to DriverInfo("Pierre", "Gasly", "Alpine"),
        "43" to DriverInfo("Franco", "Colapinto", "Alpine"),
        // Williams
        "23" to DriverInfo("Alex", "Albon", "Williams"),
        "55" to DriverInfo("Carlos", "Sainz", "Williams"),
        // RB (VCARB)
        "30" to DriverInfo("Liam", "Lawson", "RB"),
        "6" to DriverInfo("Isack", "Hadjar", "RB"),
        // Kick Sauber (Audi)
        "27" to DriverInfo("Nico", "Hulkenberg", "Kick Sauber"),
        "5" to DriverInfo("Gabriel", "Bortoleto", "Kick Sauber"),
        // Haas F1 Team
        "31" to DriverInfo("Esteban", "Ocon", "Haas F1 Team"),
        "87" to DriverInfo("Oliver", "Bearman", "Haas F1 Team")
    )

    fun getDriver(number: String): DriverInfo {
        return drivers[number] ?: DriverInfo("Driver", "#$number", "Unknown Team")
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

    fun connect() {
        scope.launch {
            try {
                Log.d(TAG, "🔌 Starting negotiation with F1 SignalR...")
                _isConnected.value = false
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
                        
                        // 3. Subscribe - include DriverList for position data, TimingAppData for tyres
                        val subscribeCmd = """{"H":"Streaming","M":"Subscribe","A":[["Heartbeat","TimingData","TimingAppData","DriverList","CarData.z","Position.z","SessionInfo","TrackStatus","LapCount","ExtrapolatedClock"]],"I":1}"""
                        webSocket.send(subscribeCmd)
                        Log.d(TAG, "📡 Sent Subscribe command")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closing: $code - $reason")
                        _isConnected.value = false
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "WebSocket error: ${t.message}", t)
                        _isConnected.value = false
                        _connectionError.value = t.message
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionError.value = e.message
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
                                val sessionName = data.getAsJsonObject("Meeting")?.get("Name")?.asString
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
            
            // Process SessionInfo
            response.get("SessionInfo")?.let { sessionElement ->
                if (sessionElement.isJsonObject) {
                    val sessionName = sessionElement.asJsonObject
                        .getAsJsonObject("Meeting")?.get("Name")?.asString
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
                    externalTeam = driverInfo.team
                )
                
                // Extract fields - only update if present in this delta
                // Position can come as "Position" or "Line" field
                val positionFromData = lineData.get("Position")?.asString 
                    ?: lineData.get("Line")?.asString
                val position = positionFromData ?: existing.position
                val positionDisplay = position ?: existing.positionDisplay
                
                // Gaps
                val gapToLeader = lineData.get("Stats")?.asJsonObject?.get("TimeDiffToFastest")?.asString 
                    ?: lineData.get("GapToLeader")?.asString
                    ?: existing.gap
                
                val intervalToAhead = lineData.get("Stats")?.asJsonObject?.get("TimeDifftoPositionAhead")?.asString
                    ?: lineData.get("IntervalToPositionAhead")?.asJsonObject?.get("Value")?.asString
                    ?: existing.interval

                // Laps
                val bestLap = lineData.get("BestLapTime")?.asJsonObject?.get("Value")?.asString
                val lastLap = lineData.get("LastLapTime")?.asJsonObject?.get("Value")?.asString
                val timeDisplay = bestLap ?: lastLap ?: existing.raceTime
                
                val laps = lineData.get("NumberOfLaps")?.asString ?: existing.laps
                val pits = lineData.get("NumberOfPitStops")?.asString ?: existing.pits
                
                // Sectors - F1 API returns this as an object with keys "0", "1", "2" etc.
                var sectorsList = existing.sectors.toMutableList()
                var sectorColorsList = existing.sectorColors.toMutableList()
                
                val sectorsElement = lineData.get("Sectors")
                if (sectorsElement != null) {
                    val sectorEntries = if (sectorsElement.isJsonObject) {
                        sectorsElement.asJsonObject.entrySet().sortedBy { it.key.toIntOrNull() ?: 0 }
                    } else if (sectorsElement.isJsonArray) {
                        sectorsElement.asJsonArray.mapIndexed { index, elem -> 
                            java.util.AbstractMap.SimpleEntry(index.toString(), elem) 
                        }
                    } else null
                    
                    sectorEntries?.forEach { sectorEntry ->
                        val sectorIndex = sectorEntry.key.toIntOrNull() ?: return@forEach
                        val sectorObj = sectorEntry.value.asJsonObject
                        val value = sectorObj.get("Value")?.asString ?: ""
                        
                        // Ensure list is large enough
                        while (sectorsList.size <= sectorIndex) {
                            sectorsList.add("")
                            sectorColorsList.add(0)
                        }
                        
                        sectorsList[sectorIndex] = value
                        
                        var color = 0
                        if (sectorObj.get("OverallFastest")?.asBoolean == true) color = 2
                        else if (sectorObj.get("PersonalFastest")?.asBoolean == true) color = 1
                        sectorColorsList[sectorIndex] = color
                    }
                }
                
                // Status
                val inPit = lineData.get("InPit")?.asBoolean ?: (existing.status == 1)
                val retired = lineData.get("Retired")?.asBoolean ?: (existing.status == 2)
                val stopped = lineData.get("Stopped")?.asBoolean ?: (existing.status == 3)
                
                var status = existing.status
                if (retired) status = 2
                else if (stopped) status = 3
                else if (inPit) status = 1
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
                        val lastStint = lastEntry.value.asJsonObject
                        compound = lastStint.get("Compound")?.asString ?: compound
                        age = lastStint.get("TotalLaps")?.asInt ?: lastStint.get("Laps")?.asInt ?: age
                    }
                }

                // Update driver in map
                val updatedDriver = LiveDriver(
                    position = position,
                    positionDisplay = positionDisplay,
                    firstName = driverInfo.firstName,
                    lastName = driverInfo.lastName,
                    externalTeam = driverInfo.team,
                    raceTime = timeDisplay,
                    gap = gapToLeader,
                    interval = intervalToAhead,
                    laps = laps,
                    pits = pits,
                    sectors = sectorsList,
                    sectorColors = sectorColorsList,
                    status = status,
                    tyreCompound = compound,
                    tyreAge = age
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
            Log.e(TAG, "Error processing timing data: ${e.message}")
        }
    }
    
    private fun processDriverList(data: JsonObject) {
        try {
            var updated = false
            
            // DriverList contains driver number -> {Line: position} mappings
            data.entrySet().forEach { entry ->
                val number = entry.key
                val driverData = entry.value
                
                if (driverData.isJsonObject) {
                    val lineValue = driverData.asJsonObject.get("Line")?.asString
                    if (lineValue != null) {
                        val existing = driverMap[number]
                        if (existing != null) {
                            // Update position if changed
                            if (existing.position != lineValue) {
                                driverMap[number] = existing.copy(
                                    position = lineValue,
                                    positionDisplay = lineValue
                                )
                                updated = true
                                Log.d(TAG, "📍 Position update: Driver $number -> P$lineValue")
                            }
                        } else {
                            // Create new driver with position
                            val driverInfo = DriverDatabase.getDriver(number)
                            driverMap[number] = LiveDriver(
                                position = lineValue,
                                positionDisplay = lineValue,
                                firstName = driverInfo.firstName,
                                lastName = driverInfo.lastName,
                                externalTeam = driverInfo.team
                            )
                            updated = true
                            Log.d(TAG, "📍 New driver: $number (${driverInfo.lastName}) -> P$lineValue")
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
