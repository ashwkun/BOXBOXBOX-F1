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
    url: String,
    isUpdateAvailable: Boolean = false,
    onUpdateClick: () -> Unit = {}
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
            AnimatedHeader(
                isUpdateAvailable = isUpdateAvailable,
                onUpdateClick = onUpdateClick
            )
            
            // WebView content
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }
                            
                            // Dismiss loader as soon as content is visible (API 26+)
                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                super.onPageCommitVisible(view, url)
                                isLoading = false
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Fallback in case onPageCommitVisible didn't fire or wasn't enough
                                isLoading = false
                                
                                // Inject JS to hide ads and headers (Visual cleanup)
                                view?.evaluateJavascript("""
                                    (function() {
                                        // Hide Ads (Best Effort)
                                        var adSelectors = [
                                            '.ad', '.advertisement', '.banner-ad', 
                                            '[id*="google_ads"]', '.adsbygoogle',
                                            '[class*="ad-container"]', '[class*="ad_wrapper"]',
                                            '.ad-slot', '.ad-unit',
                                            '.ms-ad', '.ms-ad-wrapper' // Motorsport specific
                                        ];
                                        
                                        adSelectors.forEach(function(selector) {
                                            var ads = document.querySelectorAll(selector);
                                            for (var i = 0; i < ads.length; i++) {
                                                ads[i].style.display = 'none';
                                            }
                                        });
                                        
                                        // Hide Headers and Navigation
                                        var headerSelectors = [
                                            'header', 'nav', 
                                            '.header', '.site-header', '.main-header',
                                            '.nav', '.navbar', '.navigation', '.main-nav',
                                            '.menu', '.top-bar', '.top-nav',
                                            '[role="banner"]', '[role="navigation"]',
                                            '.f1-header', '.espn-header',
                                            '.ms-header', '.ms-navbar', '.ms-navigation' // Motorsport specific
                                        ];
                                        
                                        headerSelectors.forEach(function(selector) {
                                            var headers = document.querySelectorAll(selector);
                                            for (var i = 0; i < headers.length; i++) {
                                                headers[i].style.display = 'none';
                                            }
                                        });
                                        
                                        // Hide Overlays (Recommended, Video Suggestions)
                                        var overlaySelectors = [
                                            '.ms-widget-recommended', '.ms-read-more', 
                                            '.ms-related-articles', '.related-content',
                                            '.video-player-overlay', '.vjs-overlay',
                                            '[class*="recommend"]', '[class*="suggestion"]',
                                            '.dailymotion-widget', '.connatix-widget',
                                            '.ms-item--video', '.widget-video'
                                        ];
                                        
                                        overlaySelectors.forEach(function(selector) {
                                            var overlays = document.querySelectorAll(selector);
                                            for (var i = 0; i < overlays.length; i++) {
                                                overlays[i].style.display = 'none';
                                                overlays[i].style.visibility = 'hidden';
                                                overlays[i].style.height = '0';
                                            }
                                        });
                                        
                                        // Text-based removal (Aggressive)
                                        var allDivs = document.getElementsByTagName('div');
                                        for (var i = 0; i < allDivs.length; i++) {
                                            var text = allDivs[i].textContent.trim().toLowerCase();
                                            if (text === 'recommended for you' || text === 'videos' || text === 'related') {
                                                allDivs[i].style.display = 'none';
                                                // Also try to hide parent if it's a container
                                                if (allDivs[i].parentElement && allDivs[i].parentElement.className.includes('widget')) {
                                                    allDivs[i].parentElement.style.display = 'none';
                                                }
                                            }
                                        }
                                    })();
                                """.trimIndent(), null)
                            }
                            
                            // Network-level Ad Blocking
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val url = request?.url?.toString()?.lowercase() ?: return null
                                
                                val adDomains = listOf(
                                    "doubleclick.net", "googlesyndication.com", "google-analytics.com",
                                    "criteo.com", "outbrain.com", "taboola.com", "rubiconproject.com",
                                    "pubmatic.com", "openx.net", "adnxs.com", "smartadserver.com",
                                    "amazon-adsystem.com", "moatads.com", "ads.motorsport.com"
                                )
                                
                                if (adDomains.any { url.contains(it) }) {
                                    // Return empty response for ads
                                    return android.webkit.WebResourceResponse("text/plain", "utf-8", null)
                                }
                                
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
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
