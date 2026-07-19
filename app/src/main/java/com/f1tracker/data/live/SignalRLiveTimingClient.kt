package com.f1tracker.data.live

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import java.util.concurrent.atomic.AtomicBoolean

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

/**
 * Simplified live timing row model. Populated from PlanetF1's
 * LeaderBoardLiveUpdates hub (official F1 /signalr is auth-gated).
 */
data class LiveDriver(
    val position: String? = null,
    val positionDisplay: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val tla: String? = null,
    val externalTeam: String? = null,
    val teamColour: String? = null,
    val raceTime: String? = null,
    val gap: String? = null,
    val interval: String? = null,
    val laps: String? = null,
    val pits: String? = null,
    val sectors: List<String> = emptyList(),
    val sectorColors: List<Int> = emptyList(),
    val miniSectors: List<List<Int>> = emptyList(),
    val status: Int = 0,
    val inPit: Boolean = false,
    val pitOut: Boolean = false,
    val tyreCompound: String? = null,
    val tyreAge: Int? = null,
    val gridPos: String? = null,
    val knockedOut: Boolean = false,
    val cutoff: Boolean = false,
    val bestLapTimes: List<String> = emptyList()
)

private data class PlanetF1LiveUpdate(
    @SerializedName("selectedSessionId") val selectedSessionId: Int? = null,
    @SerializedName("raceName") val raceName: String? = null,
    @SerializedName("liveRaceDataList") val liveRaceDataList: List<PlanetF1DriverEntry>? = null
)

private data class PlanetF1DriverEntry(
    @SerializedName("positionDisplay") val positionDisplay: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("externalTeam") val externalTeam: String? = null,
    @SerializedName("raceTime") val raceTime: String? = null,
    @SerializedName("gap") val gap: String? = null,
    @SerializedName("pits") val pits: JsonElement? = null,
    @SerializedName("laps") val laps: JsonElement? = null,
    @SerializedName("countryId") val countryId: Int? = null,
    @SerializedName("raceName") val raceName: String? = null
)

/**
 * Live timing client backed by PlanetF1's public SignalR Core hub.
 *
 * Official F1 livetiming.formula1.com/signalr now returns 401 (SignalR Core + account auth).
 * PlanetF1 redistributes a simplified leaderboard via:
 *   https://live.planetf1.com/api/planetF1LiveUpdate?groupType=liveRace
 */
class SignalRLiveTimingClient {

    private val TAG = "SignalRLiveTiming"
    private val HUB_URL =
        "https://live.planetf1.com/api/planetF1LiveUpdate?groupType=liveRace"

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var hubConnection: HubConnection? = null
    private var connectJob: Job? = null
    private var heartbeatJob: Job? = null
    private val connectInFlight = AtomicBoolean(false)

    private val _liveDrivers = MutableStateFlow<List<LiveDriver>>(emptyList())
    val liveDrivers: StateFlow<List<LiveDriver>> = _liveDrivers

    private val _sessionName = MutableStateFlow("")
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

    private val _feedSource = MutableStateFlow("PlanetF1")
    val feedSource: StateFlow<String> = _feedSource

    private var reconnectAttempts = 0
    private var isReconnecting = false
    private var lastMessageTime = System.currentTimeMillis()
    private var lastEnsureConnectedTime = 0L
    private var intentionalDisconnect = false

    fun connect() {
        if (!connectInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "connect() already in flight — skipping")
            return
        }
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                intentionalDisconnect = false
                val hasData = _liveDrivers.value.isNotEmpty()
                if (!hasData) {
                    _isConnected.value = false
                    _connectionStatus.value =
                        if (isReconnecting) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING
                }
                _connectionError.value = null

                teardownHub()

                Log.d(TAG, "Connecting to PlanetF1 hub…")
                val connection = HubConnectionBuilder.create(HUB_URL)
                    .withTransport(TransportEnum.WEBSOCKETS)
                    .shouldSkipNegotiate(true)
                    .withHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .build()

                connection.serverTimeout = 3_600_000
                connection.keepAliveInterval = 30_000

                connection.on(
                    "LeaderBoardLiveUpdates",
                    { payload: Any? ->
                        lastMessageTime = System.currentTimeMillis()
                        handleLeaderboardUpdate(payload)
                    },
                    Any::class.java
                )

                connection.onClosed { error ->
                    Log.w(TAG, "PlanetF1 hub closed: ${error?.message}")
                    _isConnected.value = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    if (error != null) {
                        _connectionError.value = error.message
                        com.f1tracker.util.AnalyticsLogger.liveTimingDisconnected(
                            error.message ?: "closed"
                        )
                    }
                    if (!intentionalDisconnect) {
                        scheduleReconnect()
                    }
                }

                hubConnection = connection
                connection.start().await()

                _isConnected.value = true
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectionError.value = null
                reconnectAttempts = 0
                isReconnecting = false
                lastMessageTime = System.currentTimeMillis()
                com.f1tracker.util.AnalyticsLogger.liveTimingConnected()
                Log.d(TAG, "PlanetF1 hub connected")
                startHeartbeat()
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionError.value = e.message ?: "Connection failed"
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _isConnected.value = false
                scheduleReconnect()
            } finally {
                connectInFlight.set(false)
            }
        }
    }

    private fun handleLeaderboardUpdate(payload: Any?) {
        try {
            if (payload == null) return
            val json = gson.toJsonTree(payload)
            val update = gson.fromJson(json, PlanetF1LiveUpdate::class.java) ?: return

            val raceName = update.raceName?.takeIf { it.isNotBlank() }
            if (raceName != null && !raceName.equals("FastestLap", ignoreCase = true)) {
                _sessionName.value = raceName
            }

            val entries = update.liveRaceDataList.orEmpty()
                .filter { !it.raceName.equals("FastestLap", ignoreCase = true) }
            if (entries.isEmpty()) {
                Log.d(TAG, "LeaderBoardLiveUpdates empty / fastest-lap only")
                return
            }

            val drivers = entries.mapIndexed { index, entry ->
                val pos = entry.positionDisplay ?: entry.position ?: (index + 1).toString()
                val retired = pos.equals("R", ignoreCase = true) ||
                    pos.equals("DNF", ignoreCase = true) ||
                    pos.equals("DSQ", ignoreCase = true)
                LiveDriver(
                    position = pos,
                    positionDisplay = pos,
                    firstName = entry.firstName,
                    lastName = entry.lastName,
                    tla = generateTla(entry.firstName, entry.lastName),
                    externalTeam = entry.externalTeam,
                    raceTime = entry.raceTime?.toString()?.takeIf { it.isNotBlank() && it != "null" },
                    gap = entry.gap?.toString()?.takeIf { it.isNotBlank() && it != "null" },
                    laps = jsonElementAsString(entry.laps),
                    pits = jsonElementAsString(entry.pits),
                    status = if (retired) 2 else 0
                )
            }.sortedWith(
                compareBy(
                    { it.positionDisplay?.toIntOrNull() ?: if (it.status == 2) 999 else 998 },
                    { it.lastName ?: "" }
                )
            )

            _liveDrivers.value = drivers
            Log.d(TAG, "Leaderboard updated: ${drivers.size} drivers (${_sessionName.value})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LeaderBoardLiveUpdates: ${e.message}", e)
        }
    }

    private fun jsonElementAsString(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        return try {
            when {
                element.isJsonPrimitive -> element.asString
                else -> element.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun generateTla(firstName: String?, lastName: String?): String? {
        val last = lastName?.trim().orEmpty()
        if (last.length >= 3) return last.take(3).uppercase()
        val first = firstName?.trim().orEmpty()
        val combined = (last + first).filter { it.isLetter() }
        return combined.take(3).uppercase().ifBlank { null }
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect) return
        if (isReconnecting) {
            Log.d(TAG, "Already reconnecting — skip")
            return
        }
        reconnectAttempts++
        isReconnecting = true
        val delayMs = minOf(2000L * (1L shl minOf(reconnectAttempts - 1, 4)), 30_000L)
        Log.d(TAG, "Reconnect attempt $reconnectAttempts in ${delayMs}ms")
        if (_liveDrivers.value.isEmpty()) {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            _connectionError.value = "Reconnecting… (attempt $reconnectAttempts)"
        }
        scope.launch {
            delay(delayMs)
            isReconnecting = false
            connect()
        }
    }

    fun ensureConnected() {
        val now = System.currentTimeMillis()
        if (now - lastEnsureConnectedTime < 5_000) return
        if (isReconnecting) return
        if (_connectionStatus.value == ConnectionStatus.CONNECTING ||
            _connectionStatus.value == ConnectionStatus.RECONNECTING
        ) {
            return
        }
        lastEnsureConnectedTime = now

        val hub = hubConnection
        val connected = hub?.connectionState == HubConnectionState.CONNECTED
        val stale = now - lastMessageTime > 45_000
        if (!connected || stale) {
            Log.d(TAG, "ensureConnected: connected=$connected stale=$stale — reconnecting")
            reconnectAttempts = 0
            connect()
        }
        startHeartbeat()
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (true) {
                delay(15_000)
                val stale = System.currentTimeMillis() - lastMessageTime > 45_000
                val connected = hubConnection?.connectionState == HubConnectionState.CONNECTED
                if (stale && connected && !isReconnecting && !intentionalDisconnect) {
                    Log.d(TAG, "Heartbeat: stale hub — reconnecting")
                    reconnectAttempts = 0
                    connect()
                }
            }
        }
    }

    private fun teardownHub() {
        try {
            hubConnection?.stop()?.blockingAwait()
        } catch (_: Exception) {
        }
        hubConnection = null
    }

    fun disconnect() {
        intentionalDisconnect = true
        isReconnecting = false
        connectJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch {
            teardownHub()
            _isConnected.value = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            connectInFlight.set(false)
            Log.d(TAG, "Disconnected from PlanetF1 hub")
        }
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
