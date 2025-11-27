package com.f1tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.ui.navigation.AppNavigation
import com.f1tracker.ui.theme.F1TrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load F1 driver and team data from JSON files
        loadF1Data()
        
        // Set system bars to black initially for splash screen
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        
        setContent {
            F1TrackerTheme {
                AppNavigation()
            }
        }
    }
    
    private fun loadF1Data() {
        try {
            val driversJson = assets.open("f1_2025_drivers_reformed.json").bufferedReader().use { it.readText() }
            val teamsJson = assets.open("f1_2025_teams_reformed.json").bufferedReader().use { it.readText() }
            F1DataProvider.loadData(driversJson, teamsJson)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load F1 data: ${e.message}")
        }
    }
}

