package com.f1tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.ui.components.DownloadProgressDialog
import com.f1tracker.ui.components.UpdateDialog
import com.f1tracker.ui.navigation.AppNavigation
import com.f1tracker.ui.theme.F1TrackerTheme
import com.f1tracker.util.ApkDownloader
import com.f1tracker.util.DownloadState
import com.f1tracker.util.UpdateChecker
import com.f1tracker.util.UpdateStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var updateChecker: UpdateChecker
    
    private val liveClient by lazy {
        com.f1tracker.data.live.SignalRLiveTimingClient.getInstance()
    }
    
    private val raceViewModel: com.f1tracker.ui.viewmodels.RaceViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[com.f1tracker.ui.viewmodels.RaceViewModel::class.java]
    }
    
    // State to hold intent data (url, target_tab)
    private val intentData = mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle initial intent
        handleIntent(intent)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Load F1 driver and team data from JSON files
        loadF1Data()
        
        // Set system bars to black initially for splash screen
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        
        // Manage SignalR connection based on session state (persists across navigation)
        lifecycleScope.launch {
            raceViewModel.raceWeekendState.collect { state ->
                val shouldConnect = when (state) {
                    is com.f1tracker.data.models.RaceWeekendState.Active -> state.currentEvent?.isLive == true
                    else -> false
                }
                
                if (shouldConnect) {
                    liveClient.connect()
                } else {
                    liveClient.disconnect()
                }
            }
        }
        
        setContent {
            var updateStatus by remember { mutableStateOf<UpdateStatus?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
            
            // Check for updates on startup
            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    val status = updateChecker.checkForUpdate()
                    if (status is UpdateStatus.UpdateAvailable) {
                        updateStatus = status
                        if (status.shouldShowDialog) {
                            showUpdateDialog = true
                        }
                    }
                }
            }
            
            F1TrackerTheme {
                AppNavigation(
                    updateStatus = updateStatus,
                    showUpdateDialog = showUpdateDialog,
                    onShowUpdateDialogChange = { showUpdateDialog = it },
                    intentData = intentData.value,
                    onIntentHandled = { intentData.value = null }
                )
                
                // Show update dialog if available
                if (showUpdateDialog && updateStatus is UpdateStatus.UpdateAvailable) {
                    val status = updateStatus as UpdateStatus.UpdateAvailable
                    UpdateDialog(
                        release = status.release,
                        onUpdateClick = {
                            showUpdateDialog = false
                            // Start download
                            val downloader = ApkDownloader(this@MainActivity)
                            lifecycleScope.launch {
                                downloader.download(status.downloadUrl).collect { state ->
                                    downloadState = state
                                    if (state is DownloadState.Downloaded) {
                                        downloader.installApk(state.file)
                                        // Reset state after install prompt
                                        downloadState = DownloadState.Idle
                                    }
                                }
                            }
                        },
                        onDismiss = {
                            showUpdateDialog = false
                        }
                    )
                }

                // Show download progress
                if (downloadState is DownloadState.Downloading) {
                    val progress = (downloadState as DownloadState.Downloading).progress
                    DownloadProgressDialog(
                        progress = progress,
                        onDismiss = { /* Cannot dismiss */ }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val url = intent?.getStringExtra("url")
        val targetTab = intent?.getStringExtra("target_tab")
        if (!url.isNullOrEmpty()) {
            intentData.value = url to (targetTab ?: "news")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Only disconnect when app is being killed (not just paused)
        if (isFinishing) {
            liveClient.disconnect()
        }
    }
    
    private fun loadF1Data() {
        try {
            val driversJson = assets.open("f1_2025_drivers_reformed.json").bufferedReader().use { it.readText() }
            val teamsJson = assets.open("f1_2025_teams_reformed.json").bufferedReader().use { it.readText() }
            F1DataProvider.loadData(driversJson, teamsJson)
            
            // Load ESPN race IDs
            val raceIdsJson = assets.open("espn_2025_race_ids.json").bufferedReader().use { it.readText() }
            com.f1tracker.data.local.ESPNRaceIdProvider.loadData(raceIdsJson)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load F1 data: ${e.message}")
        }
    }
}

