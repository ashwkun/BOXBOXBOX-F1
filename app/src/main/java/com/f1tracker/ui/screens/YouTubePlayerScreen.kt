package com.f1tracker.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.f1tracker.R
import com.f1tracker.ui.components.AnimatedHeader

@SuppressLint("SetJavaScriptEnabled", "SourceLockedOrientationActivity")
@Composable
fun YouTubePlayerScreen(
    videoId: String
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    
    // Save original colors - capture them BEFORE any changes
    val originalStatusBarColor = remember { 
        val color = activity?.window?.statusBarColor ?: android.graphics.Color.BLACK
        android.util.Log.d("YouTubePlayer", "Saved original status bar color: $color")
        color
    }
    val originalNavBarColor = remember { 
        val color = activity?.window?.navigationBarColor ?: android.graphics.Color.BLACK
        android.util.Log.d("YouTubePlayer", "Saved original nav bar color: $color")
        color
    }
    
    // Force landscape orientation and hide system UI when screen opens
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Set black colors for video player BEFORE hiding
        activity?.window?.apply {
            statusBarColor = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
            android.util.Log.d("YouTubePlayer", "Set colors to BLACK on entry")
        }
        
        // Hide status bar and navigation bar
        activity?.window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
    
    // Continuously enforce black colors while video player is active
    androidx.compose.runtime.SideEffect {
        activity?.window?.apply {
            if (statusBarColor != android.graphics.Color.BLACK) {
                android.util.Log.d("YouTubePlayer", "Color changed detected! Current: $statusBarColor, forcing BLACK")
                statusBarColor = android.graphics.Color.BLACK
                navigationBarColor = android.graphics.Color.BLACK
            }
        }
    }
    
    // Restore orientation and system UI when leaving
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            
            // Restore original status bar and navigation bar colors
            activity?.window?.apply {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                android.util.Log.d("YouTubePlayer", "Restoring colors - Status: $originalStatusBarColor, Nav: $originalNavBarColor")
                statusBarColor = originalStatusBarColor
                navigationBarColor = originalNavBarColor
            }
            
            webView?.destroy()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isFullscreen) {
            // YouTube player (full screen)
            AndroidView(
                factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                                setSupportZoom(true)
                                builtInZoomControls = false
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                javaScriptCanOpenWindowsAutomatically = true
                                loadsImagesAutomatically = true
                                blockNetworkImage = false
                                blockNetworkLoads = false
                            }
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    
                                    // Inject script to force fullscreen video with persistent styling
                                    view?.evaluateJavascript("""
                                        (function() {
                                            console.log('Starting fullscreen script...');
                                            
                                            var attempts = 0;
                                            var maxAttempts = 15;
                                            var videoFound = false;
                                            
                                            function forceFullscreen() {
                                                attempts++;
                                                var video = document.querySelector('video');
                                                
                                                if (video && !videoFound) {
                                                    videoFound = true;
                                                    console.log('Video element found!', video);
                                                    
                                                    // Unmute
                                                    video.muted = false;
                                                    video.volume = 1.0;
                                                    
                                                    // Try to force high quality
                                                    try {
                                                        var player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                                                        if (player) {
                                                            console.log('Found player, attempting to set quality to hd1080');
                                                            if (typeof player.setPlaybackQualityRange === 'function') {
                                                                player.setPlaybackQualityRange('hd1080');
                                                            }
                                                            if (typeof player.setPlaybackQuality === 'function') {
                                                                player.setPlaybackQuality('hd1080');
                                                            }
                                                        }
                                                    } catch (e) {
                                                        console.log('Error setting quality:', e);
                                                    }
                                                    
                                                    // Play
                                                    if (video.paused) {
                                                        video.play().catch(e => console.log('Play error:', e));
                                                    }
                                                    
                                                    // Function to apply fullscreen styles
                                                    function applyFullscreenStyles() {
                                                        if (!video) return;
                                                        
                                                        // Set video styles directly
                                                        video.style.setProperty('position', 'fixed', 'important');
                                                        video.style.setProperty('top', '0', 'important');
                                                        video.style.setProperty('left', '0', 'important');
                                                        video.style.setProperty('width', '100vw', 'important');
                                                        video.style.setProperty('height', '100vh', 'important');
                                                        video.style.setProperty('z-index', '999999', 'important');
                                                        video.style.setProperty('object-fit', 'contain', 'important');
                                                        
                                                        // Hide all siblings
                                                        var parent = video.parentElement;
                                                        if (parent) {
                                                            Array.from(parent.children).forEach(child => {
                                                                if (child !== video) {
                                                                    child.style.setProperty('display', 'none', 'important');
                                                                }
                                                            });
                                                            
                                                            // Style parent chain
                                                            var current = parent;
                                                            while (current && current !== document.body) {
                                                                current.style.setProperty('position', 'fixed', 'important');
                                                                current.style.setProperty('top', '0', 'important');
                                                                current.style.setProperty('left', '0', 'important');
                                                                current.style.setProperty('width', '100vw', 'important');
                                                                current.style.setProperty('height', '100vh', 'important');
                                                                current.style.setProperty('overflow', 'hidden', 'important');
                                                                current = current.parentElement;
                                                            }
                                                        }
                                                        
                                                        // Style body and html
                                                        document.body.style.setProperty('margin', '0', 'important');
                                                        document.body.style.setProperty('padding', '0', 'important');
                                                        document.body.style.setProperty('overflow', 'hidden', 'important');
                                                        document.body.style.setProperty('background', '#000', 'important');
                                                        document.documentElement.style.setProperty('overflow', 'hidden', 'important');
                                                        
                                                        console.log('Styles applied. Video dimensions:', video.offsetWidth, 'x', video.offsetHeight);
                                                        console.log('Video computed style:', window.getComputedStyle(video).position, window.getComputedStyle(video).width);
                                                    }
                                                    
                                                    // Apply styles initially
                                                    setTimeout(applyFullscreenStyles, 500);
                                                    
                                                    // Keep reapplying styles every 500ms to fight YouTube's changes
                                                    setInterval(applyFullscreenStyles, 500);
                                                    
                                                    // Also use MutationObserver to reapply when DOM changes
                                                    var observer = new MutationObserver(function(mutations) {
                                                        applyFullscreenStyles();
                                                    });
                                                    
                                                    observer.observe(video, {
                                                        attributes: true,
                                                        attributeFilter: ['style', 'class']
                                                    });
                                                    
                                                    if (video.parentElement) {
                                                        observer.observe(video.parentElement, {
                                                            attributes: true,
                                                            childList: true,
                                                            subtree: true
                                                        });
                                                    }
                                                    
                                                } else if (!videoFound && attempts < maxAttempts) {
                                                    console.log('Video not found, attempt:', attempts);
                                                    setTimeout(forceFullscreen, 500);
                                                }
                                            }
                                            
                                            // Start searching for video
                                            setTimeout(forceFullscreen, 1000);
                                        })();
                                    """.trimIndent(), null)
                                }
                            }
                            
                            webChromeClient = object : WebChromeClient() {
                                // Enable console logging
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    consoleMessage?.let {
                                        android.util.Log.d("WebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                                    }
                                    return true
                                }
                                
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (customView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    
                                    customView = view
                                    isFullscreen = true
                                    
                                    // Hide system UI
                                    activity?.window?.decorView?.systemUiVisibility = (
                                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    )
                                    
                                    // Add custom view to content
                                    val decorView = activity?.window?.decorView as? FrameLayout
                                    decorView?.addView(view, FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    ))
                                }
                                
                                override fun onHideCustomView() {
                                    val decorView = activity?.window?.decorView as? FrameLayout
                                    decorView?.removeView(customView)
                                    customView = null
                                    isFullscreen = false
                                    
                                    // Restore system UI
                                    activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                                }
                            }
                            
                            val mobileYouTubeUrl = "https://m.youtube.com/watch?v=$videoId&autoplay=1&vq=hd1080"
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            loadUrl(mobileYouTubeUrl)
                            
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            
            // Transparent header overlay (on top of video)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "BOX",
                        fontFamily = FontFamily(
                            Font(
                                R.font.brigends_expanded,
                                FontWeight.Normal
                            )
                        ),
                        fontSize = 16.sp,
                        color = Color(0xFFFF0080),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "·",
                        fontFamily = FontFamily(
                            Font(
                                R.font.brigends_expanded,
                                FontWeight.Normal
                            )
                        ),
                        fontSize = 16.sp,
                        color = Color(0xFFFF0080),
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
        }
    }
}

