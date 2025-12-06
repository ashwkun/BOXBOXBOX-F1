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
    val tyreAge: Int? = null
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
    @SerializedName("I") val i: String? = null // Keepalive index
)

data class F1HubMessage(
    @SerializedName("H") val hub: String,
    @SerializedName("M") val method: String,
    @SerializedName("A") val args: List<JsonObject>
)

// --- Driver Database (Hardcoded Map) ---
object DriverDatabase {
    data class DriverInfo(val firstName: String, val lastName: String, val team: String)

    private val drivers = mapOf(
        "1" to DriverInfo("Max", "Verstappen", "Red Bull Racing"),
        "11" to DriverInfo("Sergio", "Perez", "Red Bull Racing"),
        "16" to DriverInfo("Charles", "Leclerc", "Ferrari"),
        "55" to DriverInfo("Carlos", "Sainz", "Ferrari"),
        "63" to DriverInfo("George", "Russell", "Mercedes"),
        "44" to DriverInfo("Lewis", "Hamilton", "Mercedes"),
        "31" to DriverInfo("Esteban", "Ocon", "Alpine"),
        "10" to DriverInfo("Pierre", "Gasly", "Alpine"),
        "81" to DriverInfo("Oscar", "Piastri", "McLaren"),
        "4" to DriverInfo("Lando", "Norris", "McLaren"),
        "77" to DriverInfo("Valtteri", "Bottas", "Kick Sauber"),
        "24" to DriverInfo("Guanyu", "Zhou", "Kick Sauber"),
        "18" to DriverInfo("Lance", "Stroll", "Aston Martin"),
        "14" to DriverInfo("Fernando", "Alonso", "Aston Martin"),
        "20" to DriverInfo("Kevin", "Magnussen", "Haas F1 Team"),
        "27" to DriverInfo("Nico", "Hulkenberg", "Haas F1 Team"),
        "23" to DriverInfo("Alex", "Albon", "Williams"),
        "43" to DriverInfo("Franco", "Colapinto", "Williams"),
        "22" to DriverInfo("Yuki", "Tsunoda", "RB"),
        "30" to DriverInfo("Liam", "Lawson", "RB"),
        "12" to DriverInfo("Andrea Kimi", "Antonelli", "Mercedes"), // 2025
        "87" to DriverInfo("Oliver", "Bearman", "Haas F1 Team"), // 2025
        "5" to DriverInfo("Jack", "Doohan", "Alpine"), // 2025 (Rumored/Confirmed number)
        "6" to DriverInfo("Gabriel", "Bortoleto", "Kick Sauber"), // 2025
        "7" to DriverInfo("Isack", "Hadjar", "RB") // 2025
    )

    fun getDriver(number: String): DriverInfo {
        return drivers[number] ?: DriverInfo("Unknown", "Driver", "Unknown Team")
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
                        
                        // 3. Subscribe
                        val subscribeCmd = """{"H":"Streaming","M":"Subscribe","A":[["Heartbeat","TimingData","CarData.z","Position.z","SessionInfo","TrackStatus","ExtrapolatedClock"]],"I":1}"""
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
            
            // Handle Keepalive (empty message usually)
            if (f1Msg.m == null) {
                return
            }

            f1Msg.m.forEach { hubMsg ->
                if (hubMsg.method == "feed") {
                    val topic = hubMsg.args.getOrNull(0)?.asString
                    val data = hubMsg.args.getOrNull(1)

                    if (topic == "TimingData" && data != null) {
                        processTimingData(data.asJsonObject)
                    } else if (topic == "SessionInfo" && data != null) {
                         val sessionName = data.asJsonObject.getAsJsonObject("Meeting")?.get("Name")?.asString
                         if (sessionName != null) _sessionName.value = sessionName
                    } else if (topic == "LapCount" && data != null) {
                        val current = data.asJsonObject.get("CurrentLap")?.asInt
                        val total = data.asJsonObject.get("TotalLaps")?.asInt
                        if (current != null) _currentLap.value = current
                        if (total != null) _totalLaps.value = total
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}")
        }
    }

    private fun processTimingData(data: JsonObject) {
        try {
            val lines = data.getAsJsonObject("Lines") ?: return
            val currentList = _liveDrivers.value.toMutableList()
            val newDrivers = mutableListOf<LiveDriver>()

            // Convert Lines map to List<LiveDriver>
            lines.entrySet().forEach { entry ->
                val number = entry.key
                val lineData = entry.value.asJsonObject
                
                val driverInfo = DriverDatabase.getDriver(number)
                
                // Extract fields safely
                val position = lineData.get("Position")?.asString
                
                // Gaps
                val gapToLeader = lineData.get("Stats")?.asJsonObject?.get("TimeDiffToFastest")?.asString 
                    ?: lineData.get("GapToLeader")?.asString
                
                val intervalToAhead = lineData.get("Stats")?.asJsonObject?.get("TimeDifftoPositionAhead")?.asString
                    ?: lineData.get("IntervalToPositionAhead")?.asJsonObject?.get("Value")?.asString

                // Laps
                val bestLap = lineData.get("BestLapTime")?.asJsonObject?.get("Value")?.asString
                val lastLap = lineData.get("LastLapTime")?.asJsonObject?.get("Value")?.asString
                
                // Use BestLap for Quali/Practice default, LastLap for Race default (logic can be refined in UI)
                // For now, we pass both if available, or prioritize based on what's present
                val timeDisplay = bestLap ?: lastLap
                
                val laps = lineData.get("NumberOfLaps")?.asString
                val pits = lineData.get("NumberOfPitStops")?.asString
                
                // Sectors
                val sectorsList = mutableListOf<String>()
                val sectorColorsList = mutableListOf<Int>()
                
                val sectorsJson = lineData.getAsJsonArray("Sectors")
                if (sectorsJson != null) {
                    sectorsJson.forEach { s ->
                        val sectorObj = s.asJsonObject
                        val value = sectorObj.get("Value")?.asString ?: ""
                        sectorsList.add(value)
                        
                        // Simple color logic: PersonalFastest=Green(1), OverallFastest=Purple(2)
                        var color = 0
                        if (sectorObj.get("OverallFastest")?.asBoolean == true) color = 2
                        else if (sectorObj.get("PersonalFastest")?.asBoolean == true) color = 1
                        sectorColorsList.add(color)
                    }
                }
                
                // Status
                val inPit = lineData.get("InPit")?.asBoolean ?: false
                val retired = lineData.get("Retired")?.asBoolean ?: false
                val stopped = lineData.get("Stopped")?.asBoolean ?: false
                
                var status = 0
                if (retired) status = 2
                else if (stopped) status = 3
                else if (inPit) status = 1

                // Tyres (Stints)
                var compound: String? = null
                var age: Int? = null
                val stints = lineData.getAsJsonArray("Stints")
                if (stints != null && stints.size() > 0) {
                    val lastStint = stints.last().asJsonObject
                    compound = lastStint.get("Compound")?.asString
                    age = lastStint.get("Laps")?.asInt
                }

                // If we have a position, create/update driver
                if (position != null) {
                    newDrivers.add(
                        LiveDriver(
                            position = position,
                            positionDisplay = position,
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
                    )
                }
            }

            // Merge logic
            if (newDrivers.isNotEmpty()) {
                val mergedList = if (currentList.isEmpty()) {
                    newDrivers
                } else {
                    currentList.map { existing ->
                        val update = newDrivers.find { 
                            it.firstName == existing.firstName && it.lastName == existing.lastName 
                        }
                        // If update exists, use it. If not, keep existing but maybe stale?
                        // Actually, for partial updates, we should merge fields. 
                        // But for simplicity in this step, we'll just replace if update exists.
                        update?.copy(
                            // If update has null fields (partial update), keep existing?
                            // For now, we assume the update has what we need or we overwrite.
                            // A true partial update merge is complex. We'll stick to replacement for now.
                            // But for Tyres, if Stints is missing in update, we should keep existing.
                            tyreCompound = update.tyreCompound ?: existing.tyreCompound,
                            tyreAge = update.tyreAge ?: existing.tyreAge
                        ) ?: existing
                    } + newDrivers.filter { new -> 
                        currentList.none { it.firstName == new.firstName && it.lastName == new.lastName } 
                    }
                }
                _liveDrivers.value = mergedList.sortedBy { it.position?.toIntOrNull() ?: 999 }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing timing data: ${e.message}")
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
