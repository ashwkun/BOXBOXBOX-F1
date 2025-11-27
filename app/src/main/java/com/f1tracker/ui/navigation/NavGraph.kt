package com.f1tracker.ui.navigation

import androidx.compose.runtime.*
import com.f1tracker.ui.screens.SplashScreen
import com.f1tracker.ui.screens.MainAppScreen

@Composable
fun AppNavigation(
    updateStatus: com.f1tracker.util.UpdateStatus? = null,
    showUpdateDialog: Boolean = false,
    onShowUpdateDialogChange: (Boolean) -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(
            onSplashComplete = {
                showSplash = false
            }
        )
    } else {
        MainAppScreen(
            updateStatus = updateStatus,
            showUpdateDialog = showUpdateDialog,
            onShowUpdateDialogChange = onShowUpdateDialogChange
        )
    }
}

