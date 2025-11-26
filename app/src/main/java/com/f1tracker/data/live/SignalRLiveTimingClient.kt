package com.f1tracker.data.live

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString

data class LiveDriver(
    @SerializedName("position") val position: String? = null,
    @SerializedName("positionDisplay") val positionDisplay: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("externalTeam") val externalTeam: String? = null,
    @SerializedName("raceTime") val raceTime: String? = null,
    @SerializedName("gap") val gap: String? = null,
    @SerializedName("laps") val laps: String? = null,
    @SerializedName("pits") val pits: String? = null
)

data class LiveRaceData(
    @SerializedName("selectedSessionId") val selectedSessionId: Int,
    @SerializedName("raceName") val raceName: String,
    @SerializedName("liveRaceDataList") val liveRaceDataList: List<LiveDriver>
)

data class SignalRMessage(
    @SerializedName("type") val type: Int,
    @SerializedName("target") val target: String? = null,
    @SerializedName("arguments") val arguments: List<LiveRaceData>? = null
)

class SignalRLiveTimingClient {
    
    private val TAG = "SignalRLiveTiming"
    private val SIGNALR_RECORD_SEPARATOR = "\u001e"
    private val WS_URL = "wss://live.planetf1.com/api/planetF1LiveUpdate?groupType=liveRace"
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val okHttpClient = OkHttpClient()
    
    private val _liveDrivers = MutableStateFlow<List<LiveDriver>>(emptyList())
    val liveDrivers: StateFlow<List<LiveDriver>> = _liveDrivers
    
    private val _sessionName = MutableStateFlow<String>("")
    val sessionName: StateFlow<String> = _sessionName
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError
    
    private val _rawData = MutableStateFlow<String>("")
    val rawData: StateFlow<String> = _rawData
    
    fun connect() {
        Log.d(TAG, "🔌 Attempting to connect to SignalR WebSocket...")
        Log.d(TAG, "📡 URL: $WS_URL")
        
        val request = Request.Builder()
            .url(WS_URL)
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket opened successfully")
                Log.d(TAG, "📊 Response: ${response.code} ${response.message}")
                _isConnected.value = true
                _connectionError.value = null
                
                // Send SignalR handshake
                val handshake = """{"protocol":"json","version":1}$SIGNALR_RECORD_SEPARATOR"""
                webSocket.send(handshake)
                Log.d(TAG, "🤝 Sent SignalR handshake: $handshake")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleMessage(bytes.utf8())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                _isConnected.value = false
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                _isConnected.value = false
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                _isConnected.value = false
                _connectionError.value = t.message ?: "Connection failed"
            }
        })
    }
    
    private fun handleMessage(message: String) {
        try {
            // Remove record separator if present
            val cleanMessage = message.replace(SIGNALR_RECORD_SEPARATOR, "").trim()
            
            if (cleanMessage.isBlank() || cleanMessage == "{}") {
                // Handshake acknowledgment or empty message
                Log.d(TAG, "🤝 Received handshake acknowledgment")
                return
            }
            
            // Log message length and preview
            val preview = if (cleanMessage.length > 300) "${cleanMessage.take(300)}..." else cleanMessage
            Log.d(TAG, "📨 Received message (${cleanMessage.length} chars): $preview")
            
            // Parse SignalR message
            val signalRMessage = gson.fromJson(cleanMessage, SignalRMessage::class.java)
            
            Log.d(TAG, "📦 Message Type: ${signalRMessage.type}, Target: ${signalRMessage.target}")
            
            when (signalRMessage.type) {
                1 -> {
                    // Data message
                    Log.d(TAG, "🏎️ Data message received!")
                    Log.d(TAG, "🎯 Target: ${signalRMessage.target}")
                    Log.d(TAG, "📊 Arguments count: ${signalRMessage.arguments?.size ?: 0}")
                    
                    // Store raw data for detailed view
                    _rawData.value = cleanMessage
                    
                    // Try to parse regardless of target name
                    try {
                        signalRMessage.arguments?.firstOrNull()?.let { liveData ->
                            _sessionName.value = liveData.raceName
                            _liveDrivers.value = liveData.liveRaceDataList
                            Log.d(TAG, "✅ Successfully updated: ${liveData.liveRaceDataList.size} drivers in ${liveData.raceName}")
                            Log.d(TAG, "👤 First driver: ${liveData.liveRaceDataList.firstOrNull()?.let { "${it.firstName} ${it.lastName}" }}")
                        } ?: Log.w(TAG, "⚠️ No arguments found in data message")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing driver data: ${e.message}", e)
                        Log.e(TAG, "🔍 Stack trace:", e)
                    }
                }
                6 -> {
                    // Ping/keepalive - send pong back
                    webSocket?.send("""{"type":6}$SIGNALR_RECORD_SEPARATOR""")
                    Log.v(TAG, "💓 Keepalive ping received, pong sent")
                }
                else -> {
                    Log.d(TAG, "❓ Unknown message type: ${signalRMessage.type}")
                    Log.d(TAG, "📄 Full message: $cleanMessage")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing message: ${e.message}", e)
            Log.e(TAG, "🔍 Stack trace:", e)
            Log.e(TAG, "📄 Problematic message: $message")
        }
    }
    
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket...")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _isConnected.value = false
        _liveDrivers.value = emptyList()
        _sessionName.value = ""
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


