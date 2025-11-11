package com.f1tracker.ui.screens

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.f1tracker.R
import com.f1tracker.ui.components.AnimatedHeader

@Composable
fun WebViewScreen(
    url: String
) {
    var isLoading by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App header
            AnimatedHeader()
            
            // WebView content
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Full screen loader overlay (covers everything including header)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                PulsingLogo()
            }
        }
    }
}

@Composable
private fun PulsingLogo() {
    val brigendsFont = FontFamily(
        Font(R.font.brigends_expanded, FontWeight.Normal)
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Only the dot pulses
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    
    // Middle pink "BOX·" (BOX with dot)
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BOX text (static pink)
        Text(
            text = "BOX",
            fontSize = 48.sp,
            fontFamily = brigendsFont,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFFF0080), // Hot pink/magenta
            letterSpacing = 0.sp
        )
        
        // Pulsing dot
        Text(
            text = "·",
            fontSize = 48.sp,
            fontFamily = brigendsFont,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFFF0080).copy(alpha = dotPulse), // Pulsing pink dot
            letterSpacing = 0.sp
        )
    }
}
