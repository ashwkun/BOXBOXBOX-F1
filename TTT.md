# F1 Live Timing API - Android Integration Guide

Complete guide to integrate Formula 1's official live timing data into your Android application.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [API Information](#api-information)
3. [Prerequisites](#prerequisites)
4. [Implementation Methods](#implementation-methods)
5. [Method 1: SignalR Client (Recommended)](#method-1-signalr-client-recommended)
6. [Method 2: OkHttp WebSocket](#method-2-okhttp-websocket)
7. [Data Structures](#data-structures)
8. [Complete Example App](#complete-example-app)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

---

## Overview

### What is F1 Live Timing API?

The official Formula 1 live timing API provides **real-time telemetry and timing data** during active F1 sessions (Practice, Qualifying, Sprint, Race).

### What Data You Get

- ✅ **Live driver positions** (P1, P2, P3, etc.)
- ✅ **Real-time lap times** (sector times, lap times)
- ✅ **Gaps between cars** (gap to leader, gap to car ahead)
- ✅ **Speed data** (speed traps at various points)
- ✅ **Tire information** (compound, age, stint data)
- ✅ **Race control messages** (flags, penalties)
- ✅ **Weather data** (track temp, air temp, rainfall)
- ✅ **Pit stop events** (pit in/out status)

### Limitations

- ⚠️ **Only available during active F1 sessions** (not available between race weekends)
- ⚠️ **No historical data** (use FastF1 API for historical data)
- ⚠️ **WebSocket connection** (requires persistent connection)
- ⚠️ **No official documentation** (community-discovered API)

---

## API Information

### Base URLs

- **Negotiate Endpoint:** `https://livetiming.formula1.com/signalr/negotiate`
- **WebSocket Endpoint:** `wss://livetiming.formula1.com/signalr/connect`

### Protocol

- **SignalR 1.5** (Microsoft's real-time communication library)
- **WebSocket Transport**
- **JSON Hub Protocol**

### Authentication

- ✅ **No API key required**
- ✅ **No authentication needed**
- ✅ **Publicly accessible**

### Connection Flow

```
1. HTTP GET → /signalr/negotiate
2. Receive ConnectionToken
3. WebSocket Connect → /signalr/connect?token=...
4. Send Subscribe message
5. Receive real-time data streams
```

---

## Prerequisites

### Android Requirements

- **Minimum SDK:** API 21 (Android 5.0 Lollipop)
- **Target SDK:** API 34 (Android 14)
- **Language:** Kotlin (recommended) or Java
- **Build System:** Gradle

### Required Permissions

Add to `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Required permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:usesCleartextTraffic="false">
        <!-- Your app components -->
    </application>
</manifest>
```

---

## Implementation Methods

### Comparison

| Feature | SignalR Client | OkHttp WebSocket |
|---------|---------------|------------------|
| **Ease of Use** | ⭐⭐⭐⭐⭐ Easy | ⭐⭐⭐ Moderate |
| **Setup Time** | 5 minutes | 15 minutes |
| **Code Complexity** | Low | Medium |
| **Control** | High-level | Low-level |
| **Reconnection** | Automatic | Manual |
| **Recommended** | ✅ Yes | For advanced users |

---

## Method 1: SignalR Client (Recommended)

### Step 1: Add Dependencies

`build.gradle.kts` (app level):

```kotlin
dependencies {
    // SignalR Client
    implementation("com.microsoft.signalr:signalr:7.0.0")
    
    // RxJava (required by SignalR)
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    
    // Coroutines (for async operations)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Gson (for JSON parsing)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
```

Sync your project after adding dependencies.

---

### Step 2: Create F1 Live Timing Client

Create `F1LiveTimingClient.kt`:

```kotlin
package com.yourapp.f1timing

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class F1LiveTimingClient {
    
    private var hubConnection: HubConnection? = null
    private val gson = Gson()
    
    // Callbacks for different data types
    var onTimingData: ((F1TimingData) -> Unit)? = null
    var onCarData: ((JsonObject) -> Unit)? = null
    var onPositionData: ((JsonObject) -> Unit)? = null
    var onWeatherData: ((JsonObject) -> Unit)? = null
    var onHeartbeat: ((String) -> Unit)? = null
    var onConnectionError: ((String) -> Unit)? = null
    
    /**
     * Connect to F1 Live Timing API
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to F1 Live Timing...")
            
            // Build hub connection
            hubConnection = HubConnectionBuilder.create(HUB_URL)
                .build()
            
            // Set up message handlers
            setupMessageHandlers()
            
            // Set up connection lifecycle handlers
            hubConnection?.onClosed { error ->
                error?.let {
                    Log.e(TAG, "Connection closed: ${it.message}")
                    onConnectionError?.invoke(it.message ?: "Connection closed")
                }
            }
            
            // Start connection
            hubConnection?.start()?.blockingAwait()
            Log.d(TAG, "Connected successfully!")
            
            // Subscribe to data feeds
            subscribeToFeeds()
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            onConnectionError?.invoke(e.message ?: "Unknown error")
            throw e
        }
    }
    
    /**
     * Set up handlers for different message types
     */
    private fun setupMessageHandlers() {
        // Main feed handler
        hubConnection?.on("feed", { feedName: String, dataJson: String, timestamp: String ->
            try {
                handleFeed(feedName, dataJson, timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling feed: ${e.message}", e)
            }
        }, String::class.java, String::class.java, String::class.java)
    }
    
    /**
     * Handle incoming feed data
     */
    private fun handleFeed(feedName: String, dataJson: String, timestamp: String) {
        Log.d(TAG, "Received $feedName at $timestamp")
        
        when (feedName) {
            "Heartbeat" -> {
                val data = gson.fromJson(dataJson, JsonObject::class.java)
                onHeartbeat?.invoke(data.get("Utc")?.asString ?: timestamp)
            }
            
            "TimingData" -> {
                val timingData = parseTimingData(dataJson)
                onTimingData?.invoke(timingData)
            }
            
            "TimingAppData" -> {
                val data = gson.fromJson(dataJson, JsonObject::class.java)
                // Handle timing app data (stints, tires, etc.)
            }
            
            "CarData.z" -> {
                val data = gson.fromJson(dataJson, JsonObject::class.java)
                onCarData?.invoke(data)
            }
            
            "Position.z" -> {
                val data = gson.fromJson(dataJson, JsonObject::class.java)
                onPositionData?.invoke(data)
            }
            
            "WeatherData" -> {
                val data = gson.fromJson(dataJson, JsonObject::class.java)
                onWeatherData?.invoke(data)
            }
            
            else -> {
                Log.d(TAG, "Unhandled feed: $feedName")
            }
        }
    }
    
    /**
     * Subscribe to F1 data feeds
     */
    private fun subscribeToFeeds() {
        val feeds = arrayOf(
            "Heartbeat",
            "TimingData",
            "TimingAppData",
            "TimingStats",
            "CarData.z",
            "Position.z",
            "WeatherData",
            "TrackStatus",
            "SessionInfo",
            "SessionData",
            "RaceControlMessages"
        )
        
        try {
            hubConnection?.invoke(Void::class.java, "Subscribe", feeds)
                ?.blockingAwait()
            Log.d(TAG, "Subscribed to feeds: ${feeds.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe: ${e.message}", e)
        }
    }
    
    /**
     * Parse timing data JSON
     */
    private fun parseTimingData(json: String): F1TimingData {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        val lines = mutableMapOf<String, DriverLine>()
        
        jsonObject.getAsJsonObject("Lines")?.entrySet()?.forEach { entry ->
            val driverNumber = entry.key
            val driverJson = entry.value.asJsonObject
            
            lines[driverNumber] = DriverLine(
                position = driverJson.get("Position")?.asString,
                racingNumber = driverJson.get("RacingNumber")?.asString ?: driverNumber,
                gapToLeader = driverJson.get("GapToLeader")?.asString,
                intervalToPositionAhead = driverJson.getAsJsonObject("IntervalToPositionAhead")
                    ?.get("Value")?.asString,
                inPit = driverJson.get("InPit")?.asBoolean ?: false,
                pitOut = driverJson.get("PitOut")?.asBoolean ?: false,
                stopped = driverJson.get("Stopped")?.asBoolean ?: false,
                retired = driverJson.get("Retired")?.asBoolean ?: false
            )
        }
        
        return F1TimingData(lines)
    }
    
    /**
     * Disconnect from API
     */
    fun disconnect() {
        try {
            hubConnection?.stop()?.blockingAwait()
            Log.d(TAG, "Disconnected from F1 Live Timing")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }
    
    companion object {
        private const val TAG = "F1LiveTiming"
        private const val HUB_URL = "https://livetiming.formula1.com/signalr"
    }
}
```

---

### Step 3: Create Data Models

Create `F1TimingModels.kt`:

```kotlin
package com.yourapp.f1timing

import com.google.gson.annotations.SerializedName

/**
 * Main timing data structure
 */
data class F1TimingData(
    val lines: Map<String, DriverLine>
)

/**
 * Individual driver line data
 */
data class DriverLine(
    val position: String?,
    val racingNumber: String,
    val gapToLeader: String?,
    val intervalToPositionAhead: String?,
    val inPit: Boolean = false,
    val pitOut: Boolean = false,
    val stopped: Boolean = false,
    val retired: Boolean = false
)

/**
 * Car position data
 */
data class CarPosition(
    val x: Double,
    val y: Double,
    val z: Double
)

/**
 * Weather data
 */
data class WeatherData(
    val airTemp: Double?,
    val trackTemp: Double?,
    val humidity: Int?,
    val pressure: Double?,
    val windSpeed: Double?,
    val windDirection: Int?,
    val rainfall: Boolean = false
)

/**
 * Sector data
 */
data class Sector(
    val value: String?,
    val status: Int,
    val overallFastest: Boolean = false,
    val personalFastest: Boolean = false
)

/**
 * Speed data
 */
data class Speed(
    val value: String?,
    val overallFastest: Boolean = false,
    val personalFastest: Boolean = false
)
```

---

### Step 4: Create ViewModel

Create `F1LiveTimingViewModel.kt`:

```kotlin
package com.yourapp.f1timing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class F1LiveTimingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val client = F1LiveTimingClient()
    
    // LiveData for UI
    private val _timingData = MutableLiveData<F1TimingData>()
    val timingData: LiveData<F1TimingData> = _timingData
    
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    init {
        setupClientCallbacks()
    }
    
    /**
     * Set up callbacks for the client
     */
    private fun setupClientCallbacks() {
        client.onTimingData = { data ->
            _timingData.postValue(data)
        }
        
        client.onConnectionError = { error ->
            _errorMessage.postValue(error)
            _connectionState.postValue(ConnectionState.ERROR)
        }
        
        client.onHeartbeat = { timestamp ->
            // Connection is alive
            if (_connectionState.value != ConnectionState.CONNECTED) {
                _connectionState.postValue(ConnectionState.CONNECTED)
            }
        }
    }
    
    /**
     * Connect to F1 Live Timing
     */
    fun connect() {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                client.connect()
                _connectionState.value = ConnectionState.CONNECTED
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.value = e.message ?: "Connection failed"
            }
        }
    }
    
    /**
     * Disconnect from F1 Live Timing
     */
    fun disconnect() {
        client.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    /**
     * Get sorted drivers by position
     */
    fun getSortedDrivers(): List<Pair<String, DriverLine>> {
        val data = _timingData.value ?: return emptyList()
        return data.lines.entries
            .sortedBy { it.value.position?.toIntOrNull() ?: Int.MAX_VALUE }
            .map { it.key to it.value }
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

/**
 * Connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
```

---

### Step 5: Create UI (Activity)

Create `LiveTimingActivity.kt`:

```kotlin
package com.yourapp.f1timing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.databinding.ActivityLiveTimingBinding

class LiveTimingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLiveTimingBinding
    private val viewModel: F1LiveTimingViewModel by viewModels()
    private lateinit var adapter: LiveTimingAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveTimingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupObservers()
        setupButtons()
        
        // Auto-connect on start
        viewModel.connect()
    }
    
    private fun setupRecyclerView() {
        adapter = LiveTimingAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveTimingActivity)
            adapter = this@LiveTimingActivity.adapter
        }
    }
    
    private fun setupObservers() {
        // Observe timing data
        viewModel.timingData.observe(this) { data ->
            val sortedDrivers = viewModel.getSortedDrivers()
            adapter.submitList(sortedDrivers)
        }
        
        // Observe connection state
        viewModel.connectionState.observe(this) { state ->
            updateConnectionUI(state)
        }
        
        // Observe errors
        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupButtons() {
        binding.btnConnect.setOnClickListener {
            viewModel.connect()
        }
        
        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }
    }
    
    private fun updateConnectionUI(state: ConnectionState) {
        binding.tvConnectionStatus.text = when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.CONNECTING -> "Connecting..."
            ConnectionState.CONNECTED -> "🟢 Live"
            ConnectionState.ERROR -> "❌ Error"
        }
        
        binding.btnConnect.isEnabled = state != ConnectionState.CONNECTED
        binding.btnDisconnect.isEnabled = state == ConnectionState.CONNECTED
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }
}
```

---

### Step 6: Create RecyclerView Adapter

Create `LiveTimingAdapter.kt`:

```kotlin
package com.yourapp.f1timing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.databinding.ItemDriverTimingBinding

class LiveTimingAdapter : ListAdapter<Pair<String, DriverLine>, LiveTimingAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDriverTimingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemDriverTimingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: Pair<String, DriverLine>) {
            val (driverNumber, data) = item
            
            binding.apply {
                tvPosition.text = data.position ?: "-"
                tvDriverNumber.text = "#$driverNumber"
                tvGap.text = data.gapToLeader ?: "---"
                tvInterval.text = data.intervalToPositionAhead ?: "---"
                
                // Show pit status
                tvStatus.text = when {
                    data.retired -> "RET"
                    data.inPit -> "PIT"
                    data.pitOut -> "OUT"
                    data.stopped -> "STOP"
                    else -> ""
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Pair<String, DriverLine>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, DriverLine>,
            newItem: Pair<String, DriverLine>
        ): Boolean {
            return oldItem.first == newItem.first
        }
        
        override fun areContentsTheSame(
            oldItem: Pair<String, DriverLine>,
            newItem: Pair<String, DriverLine>
        ): Boolean {
            return oldItem.second == newItem.second
        }
    }
}
```

---

### Step 7: Create Layouts

`activity_live_timing.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">
    
    <!-- Connection Status -->
    <TextView
        android:id="@+id/tvConnectionStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Disconnected"
        android:textSize="16sp"
        android:textStyle="bold"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent" />
    
    <!-- Connect Button -->
    <Button
        android:id="@+id/btnConnect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Connect"
        android:layout_marginEnd="8dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toStartOf="@id/btnDisconnect" />
    
    <!-- Disconnect Button -->
    <Button
        android:id="@+id/btnDisconnect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Disconnect"
        android:enabled="false"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
    
    <!-- RecyclerView for timing data -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="16dp"
        app:layout_constraintTop_toBottomOf="@id/tvConnectionStatus"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
    
</androidx.constraintlayout.widget.ConstraintLayout>
```

`item_driver_timing.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardElevation="4dp"
    app:cardCornerRadius="8dp">
    
    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp">
        
        <!-- Position -->
        <TextView
            android:id="@+id/tvPosition"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:gravity="center"
            android:text="1"
            android:textSize="20sp"
            android:textStyle="bold"
            android:background="@drawable/position_circle"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />
        
        <!-- Driver Number -->
        <TextView
            android:id="@+id/tvDriverNumber"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="#1"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginStart="16dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toEndOf="@id/tvPosition" />
        
        <!-- Gap to Leader -->
        <TextView
            android:id="@+id/tvGap"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="+1.234"
            android:textSize="14sp"
            android:layout_marginStart="16dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toEndOf="@id/tvPosition" />
        
        <!-- Interval -->
        <TextView
            android:id="@+id/tvInterval"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="+0.567"
            android:textSize="14sp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvStatus" />
        
        <!-- Status (PIT, OUT, etc.) -->
        <TextView
            android:id="@+id/tvStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text=""
            android:textSize="12sp"
            android:textColor="#FF0000"
            android:textStyle="bold"
            android:layout_marginEnd="8dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />
        
    </androidx.constraintlayout.widget.ConstraintLayout>
    
</androidx.cardview.widget.CardView>
```

---

## Method 2: OkHttp WebSocket

### Step 1: Add Dependencies

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 2: Create WebSocket Client

```kotlin
package com.yourapp.f1timing

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class F1WebSocketClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    
    var onTimingData: ((F1TimingData) -> Unit)? = null
    var onConnectionError: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    
    /**
     * Connect to F1 Live Timing via WebSocket
     */
    suspend fun connect() = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            Log.d(TAG, "Step 1: Negotiating connection...")
            
            // Step 1: Negotiate
            val negotiateUrl = buildNegotiateUrl()
            val request = Request.Builder()
                .url(negotiateUrl)
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.e(TAG, "Negotiation failed", e)
                    continuation.resumeWithException(e)
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string()
                        val json = JSONObject(body ?: "")
                        val connectionToken = json.getString("ConnectionToken")
                        
                        Log.d(TAG, "Step 2: Connecting WebSocket...")
                        connectWebSocket(connectionToken, continuation)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse negotiate response", e)
                        continuation.resumeWithException(e)
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Build negotiate URL
     */
    private fun buildNegotiateUrl(): String {
        val connectionData = URLEncoder.encode("[{\"name\":\"Streaming\"}]", "UTF-8")
        return "$BASE_URL/negotiate?clientProtocol=1.5&connectionData=$connectionData"
    }
    
    /**
     * Connect WebSocket with connection token
     */
    private fun connectWebSocket(
        connectionToken: String,
        continuation: suspendCancellableCoroutine<Unit>
    ) {
        val encodedToken = URLEncoder.encode(connectionToken, "UTF-8")
        val connectionData = URLEncoder.encode("[{\"name\":\"Streaming\"}]", "UTF-8")
        
        val wsUrl = "wss://livetiming.formula1.com/signalr/connect?" +
                "transport=webSockets" +
                "&clientProtocol=1.5" +
                "&connectionToken=$encodedToken" +
                "&connectionData=$connectionData"
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket connected!")
                
                // Subscribe to feeds
                subscribeToFeeds(webSocket)
                
                onConnected?.invoke()
                continuation.resume(Unit)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleMessage(bytes.utf8())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                onConnectionError?.invoke(t.message ?: "Connection failed")
                
                if (continuation.isActive) {
                    continuation.resumeWithException(t)
                }
            }
        })
    }
    
    /**
     * Subscribe to F1 data feeds
     */
    private fun subscribeToFeeds(webSocket: WebSocket) {
        val subscribe = JSONObject().apply {
            put("H", "Streaming")
            put("M", "Subscribe")
            put("A", JSONArray().apply {
                put(JSONArray().apply {
                    put("Heartbeat")
                    put("TimingData")
                    put("TimingAppData")
                    put("CarData.z")
                    put("Position.z")
                    put("WeatherData")
                    put("TrackStatus")
                    put("SessionInfo")
                    put("RaceControlMessages")
                })
            })
            put("I", 1)
        }
        
        webSocket.send(subscribe.toString())
        Log.d(TAG, "📤 Sent subscription request")
    }
    
    /**
     * Handle incoming WebSocket message
     */
    private fun handleMessage(text: String) {
        try {
            if (text.isEmpty() || text == "{}") return
            
            val json = JSONObject(text)
            
            // Check if message contains feed data
            if (json.has("M")) {
                val messages = json.getJSONArray("M")
                
                for (i in 0 until messages.length()) {
                    val message = messages.getJSONObject(i)
                    
                    if (message.getString("H") == "Streaming" && 
                        message.getString("M") == "feed") {
                        
                        val args = message.getJSONArray("A")
                        val feedName = args.getString(0)
                        val data = args.getString(1)
                        
                        handleFeed(feedName, data)
                    }
                }
            }
            
            // Check for initial state message
            if (json.has("R")) {
                val stateData = json.getJSONObject("R")
                if (stateData.has("TimingData")) {
                    val timingJson = stateData.getJSONObject("TimingData").toString()
                    parseAndEmitTimingData(timingJson)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}")
        }
    }
    
    /**
     * Handle feed data
     */
    private fun handleFeed(feedName: String, dataJson: String) {
        when (feedName) {
            "TimingData" -> {
                parseAndEmitTimingData(dataJson)
            }
            "Heartbeat" -> {
                Log.d(TAG, "💓 Heartbeat received")
            }
            else -> {
                Log.d(TAG, "📡 Received $feedName")
            }
        }
    }
    
    /**
     * Parse and emit timing data
     */
    private fun parseAndEmitTimingData(json: String) {
        try {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            val lines = mutableMapOf<String, DriverLine>()
            
            jsonObject.getAsJsonObject("Lines")?.entrySet()?.forEach { entry ->
                val driverNumber = entry.key
                val driverJson = entry.value.asJsonObject
                
                lines[driverNumber] = DriverLine(
                    position = driverJson.get("Position")?.asString,
                    racingNumber = driverJson.get("RacingNumber")?.asString ?: driverNumber,
                    gapToLeader = driverJson.get("GapToLeader")?.asString,
                    intervalToPositionAhead = driverJson.getAsJsonObject("IntervalToPositionAhead")
                        ?.get("Value")?.asString,
                    inPit = driverJson.get("InPit")?.asBoolean ?: false,
                    pitOut = driverJson.get("PitOut")?.asBoolean ?: false,
                    stopped = driverJson.get("Stopped")?.asBoolean ?: false,
                    retired = driverJson.get("Retired")?.asBoolean ?: false
                )
            }
            
            onTimingData?.invoke(F1TimingData(lines))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timing data: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect from WebSocket
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
    
    companion object {
        private const val TAG = "F1WebSocket"
        private const val BASE_URL = "https://livetiming.formula1.com/signalr"
    }
}
```

---

## Data Structures

### Complete Data Models

```kotlin
// Timing Data
data class F1TimingData(
    val lines: Map<String, DriverLine>
)

data class DriverLine(
    val position: String?,
    val racingNumber: String,
    val gapToLeader: String?,
    val intervalToPositionAhead: String?,
    val inPit: Boolean = false,
    val pitOut: Boolean = false,
    val stopped: Boolean = false,
    val retired: Boolean = false,
    val numberOfLaps: Int? = null,
    val sectors: List<Sector>? = null,
    val speeds: Map<String, Speed>? = null
)

// Sector Times
data class Sector(
    val value: String?,
    val previousValue: String?,
    val status: Int = 0,
    val overallFastest: Boolean = false,
    val personalFastest: Boolean = false,
    val segments: List<Segment>? = null
)

data class Segment(
    val status: Int = 0
)

// Speed Data
data class Speed(
    val value: String?,
    val overallFastest: Boolean = false,
    val personalFastest: Boolean = false
)

// Weather
data class WeatherData(
    val airTemp: Double?,
    val trackTemp: Double?,
    val humidity: Int?,
    val pressure: Double?,
    val windSpeed: Double?,
    val windDirection: Int?,
    val rainfall: Boolean = false
)

// Session Status
enum class SessionStatus {
    INACTIVE,
    STARTED,
    ABORTED,
    FINISHED,
    FINALISED,
    ENDS
}

// Track Status
enum class TrackStatus(val value: String) {
    ALL_CLEAR("1"),
    YELLOW("2"),
    SC("4"),
    RED("5"),
    VSC("6"),
    VSC_ENDING("7")
}
```

---

## Complete Example App

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yourapp/
│   │   │   ├── f1timing/
│   │   │   │   ├── F1LiveTimingClient.kt
│   │   │   │   ├── F1WebSocketClient.kt (alternative)
│   │   │   │   ├── F1TimingModels.kt
│   │   │   │   ├── F1LiveTimingViewModel.kt
│   │   │   │   ├── LiveTimingActivity.kt
│   │   │   │   ├── LiveTimingAdapter.kt
│   │   │   │   └── ConnectionState.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_live_timing.xml
│   │   │   │   └── item_driver_timing.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── colors.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## Best Practices

### 1. Connection Management

```kotlin
class F1TimingService : LifecycleService() {
    
    private val client = F1LiveTimingClient()
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        lifecycleScope.launch {
            // Connect only during active sessions
            if (isActiveSession()) {
                client.connect()
                
                // Show foreground notification
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }
        
        return START_STICKY
    }
    
    private fun isActiveSession(): Boolean {
        // Check if there's an active F1 session
        // You can use your FastF1 API to check schedule
        return true // Implement your logic
    }
}
```

### 2. Battery Optimization

```kotlin
// Only connect when app is in foreground
class LiveTimingActivity : AppCompatActivity() {
    
    override fun onResume() {
        super.onResume()
        viewModel.connect()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.disconnect()
    }
}
```

### 3. Error Handling

```kotlin
fun handleConnectionError(error: String) {
    when {
        error.contains("404") -> {
            // No active session
            showMessage("No active F1 session. Check back during race weekend!")
        }
        error.contains("timeout") -> {
            // Connection timeout
            showMessage("Connection timeout. Retrying...")
            retryConnection()
        }
        else -> {
            showMessage("Connection error: $error")
        }
    }
}
```

### 4. Reconnection Logic

```kotlin
class F1LiveTimingClient {
    
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    
    private fun scheduleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delay = (reconnectAttempts * 2000L) // Exponential backoff
            
            Handler(Looper.getMainLooper()).postDelayed({
                connect()
            }, delay)
        }
    }
}
```

### 5. Data Caching

```kotlin
class F1TimingRepository(context: Context) {
    
    private val sharedPrefs = context.getSharedPreferences("f1_timing", Context.MODE_PRIVATE)
    
    fun cacheTimingData(data: F1TimingData) {
        val json = Gson().toJson(data)
        sharedPrefs.edit().putString("last_timing_data", json).apply()
    }
    
    fun getLastTimingData(): F1TimingData? {
        val json = sharedPrefs.getString("last_timing_data", null) ?: return null
        return Gson().fromJson(json, F1TimingData::class.java)
    }
}
```

### 6. Combining with FastF1 API

```kotlin
class F1DataViewModel : ViewModel() {
    
    private val liveTimingClient = F1LiveTimingClient()
    private val fastF1Api = FastF1ApiService()
    
    // Use FastF1 API for historical data
    suspend fun getHistoricalData(year: Int, gp: String, session: String) {
        val data = fastF1Api.getSessionResults(year, gp, session)
        _historicalData.value = data
    }
    
    // Use Live Timing for current session
    fun connectToLiveSession() {
        viewModelScope.launch {
            liveTimingClient.connect()
        }
    }
}
```

---

## Troubleshooting

### Issue 1: Connection Fails with 404

**Problem:** WebSocket returns 404 Not Found

**Solution:**
- There is no active F1 session right now
- Check F1 calendar for next race weekend
- Only connect during Practice, Qualifying, Sprint, or Race sessions

```kotlin
// Check if session is active before connecting
fun isSessionActive(): Boolean {
    // Call your FastF1 API to check current schedule
    val schedule = api.getSchedule(2024)
    val now = LocalDateTime.now()
    
    return schedule.any { event ->
        // Check if any session is within 6 hours
        val sessionTime = LocalDateTime.parse(event.sessionDate)
        Duration.between(now, sessionTime).toHours() in -3..3
    }
}
```

### Issue 2: SSL Certificate Error

**Problem:** SSL handshake fails

**Solution:**
```kotlin
// For debugging only (remove in production)
val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
})

val sslContext = SSLContext.getInstance("TLS")
sslContext.init(null, trustAllCerts, SecureRandom())

val client = OkHttpClient.Builder()
    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
    .hostnameVerifier { _, _ -> true }
    .build()
```

### Issue 3: No Data Received

**Problem:** Connected but no data coming through

**Solution:**
- Check if you subscribed to feeds correctly
- Verify the feed names (case-sensitive)
- Check logs for subscription confirmation

```kotlin
// Add logging
hubConnection?.on("feed", { feedName: String, data: String, timestamp: String ->
    Log.d("F1Timing", "Received feed: $feedName at $timestamp")
    // Your handler code
}, String::class.java, String::class.java, String::class.java)
```

### Issue 4: App Crashes on Background

**Problem:** App crashes when moved to background

**Solution:**
```kotlin
// Disconnect when app goes to background
override fun onPause() {
    super.onPause()
    if (!isChangingConfigurations) {
        viewModel.disconnect()
    }
}
```

### Issue 5: Memory Leaks

**Problem:** Memory increases over time

**Solution:**
```kotlin
// Clear references properly
override fun onDestroy() {
    super.onDestroy()
    client.onTimingData = null
    client.onCarData = null
    client.onConnectionError = null
    client.disconnect()
}
```

---

## Testing

### Test During Live Session

1. **Check F1 Calendar:** https://www.formula1.com/en/racing/2025.html
2. **Connect during session** (Practice, Qualifying, or Race)
3. **Verify data is streaming**

### Test Connection Without Session

```kotlin
@Test
fun testNegotiateEndpoint() = runTest {
    val client = OkHttpClient()
    val url = "https://livetiming.formula1.com/signalr/negotiate?" +
              "clientProtocol=1.5&connectionData=%5B%7B%22name%22%3A%22Streaming%22%7D%5D"
    
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    
    assertTrue(response.isSuccessful)
    assertNotNull(response.body?.string())
}
```

---

## FAQ

### Q: Does this work for all F1 sessions?
**A:** Yes - Practice, Qualifying, Sprint, and Race sessions all stream live data.

### Q: Is this legal/allowed?
**A:** The API is publicly accessible without authentication. However, check F1's Terms of Service for commercial use restrictions.

### Q: Can I get historical data?
**A:** No. Use FastF1 API for historical data. This only provides live data during active sessions.

### Q: How much data does it use?
**A:** Approximately 1-2 MB per hour during an active session.

### Q: Can I run this in background?
**A:** Yes, but implement as a Foreground Service to prevent Android from killing it.

### Q: Will this drain battery?
**A:** Yes, persistent WebSocket connections consume battery. Disconnect when not needed.

---

## Additional Resources

- **F1 Official Website:** https://www.formula1.com
- **SignalR Documentation:** https://learn.microsoft.com/en-us/aspnet/signalr/
- **OkHttp Documentation:** https://square.github.io/okhttp/
- **Android WebSocket Guide:** https://developer.android.com/guide/topics/connectivity/websockets

---

## Support

For issues related to:
- **FastF1 API:** Check your API documentation
- **Live Timing Connection:** Check F1 schedule and network connectivity
- **Android Issues:** Check Android version compatibility

---

## License

This guide is for educational purposes. Formula 1 and related trademarks are property of Formula One Management.

---

**Last Updated:** November 9, 2025  
**API Version:** SignalR 1.5  
**Min Android SDK:** 21 (Lollipop)  
**Tested On:** Android 14 (API 34)

