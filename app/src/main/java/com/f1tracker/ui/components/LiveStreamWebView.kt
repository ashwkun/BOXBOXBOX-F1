package com.f1tracker.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.f1tracker.R
import kotlinx.coroutines.delay

data class StreamProvider(
    val name: String,
    val url: String,
    val label: String
)

private val STREAM_PROVIDERS = listOf(
    StreamProvider("sky", "https://php.adffdafdsafds.sbs/channel/SkySportsF1%5BUK%5D", "SKY F1"),
    StreamProvider("f1tv", "https://hakunamatata5.org/hakunamatata5.html", "F1 TV"),
    StreamProvider("other", "https://embedsports.top/embed/admin/ppv-australian-grand-prix-practice-3/1", "OTHER")
)

private val AD_DOMAINS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com",
    "adclick", "popads", "popcash", "propellerads", "trafficjunky",
    "exoclick", "juicyads", "clickadu", "hilltopads", "pushground",
    "adsterra", "vibe-promo.com", "betvibe", "1xbet", "bet365",
    "betway", "casino", "gambling", "wagering"
)

// JS to unmute via API (not clicking buttons), hide unmute UI, and force live edge
private val UNMUTE_AND_CLEANUP_JS = """
(function() {
    window.open = function() { return null; };
    
    function forceUnmuteAndClean() {
        // 1. Force unmute and play ALL videos
        document.querySelectorAll('video').forEach(function(v) {
            v.muted = false;
            v.volume = 1.0;
            if (v.paused) {
                console.log("BoxBoxBox: Forcing video play()");
                var playPromise = v.play();
                if (playPromise !== undefined) {
                    playPromise.catch(function(e) {
                        console.log("BoxBoxBox: Autoplay prevented: " + e);
                    });
                }
            }
            
            // 2. Soft-sync to live edge (don't hard seek, it causes buffering loops)
            try {
                if (v.buffered && v.buffered.length > 0) {
                    var liveEdge = v.buffered.end(v.buffered.length - 1);
                    var diff = liveEdge - v.currentTime;
                    
                    if (diff > 10) {
                        console.log("BoxBoxBox: Out of sync (" + diff.toFixed(1) + "s behind), speeding up to catch live edge");
                        v.playbackRate = 1.5; // Speed up to catch live edge
                    } else if (diff < 3) {
                        v.playbackRate = 1.0; // Back to normal speed when close
                    }
                }
            } catch(e) {
                console.log("BoxBoxBox: Error syncing to live edge: " + e);
            }
        });
        
        // 3. Brutal click on anything that looks like a play overlay (fixes F1 TV / Clappr)
        document.querySelectorAll(
            '.plyr__control--overlaid, button[aria-label="Play"], ' +
            '.vjs-big-play-button, .play-wrapper, .play-btn, ' +
            '[class*="play-overlay"], [class*="overlay-play"]'
        ).forEach(function(btn) {
            var style = window.getComputedStyle(btn);
            if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') {
               console.log("BoxBoxBox: Clicking play overlay button");
               try { btn.click(); } catch(e) {}
            }
        });
        
        // For Plyr player: unmute and play via plyr API
        if (window.player && window.player.muted !== undefined) {
            window.player.muted = false;
            window.player.volume = 1.0;
            if (!window.player.playing) window.player.play();
        }
        document.querySelectorAll('.plyr').forEach(function(el) {
            if (el.plyr) {
                el.plyr.muted = false;
                el.plyr.volume = 1.0;
                if (!el.plyr.playing) el.plyr.play();
            }
        });
        
        // 4. HIDE unmute buttons/overlays via CSS
        var style = document.getElementById('boxboxbox-unmute-fix');
        if (!style) {
            style = document.createElement('style');
            style.id = 'boxboxbox-unmute-fix';
            style.textContent = [
                '[class*="unmute"] { display: none !important; }',
                '#UnMutePlayer { display: none !important; }',
                '#bet-banner { display: none !important; }',
                '.grecaptcha-badge { display: none !important; }',
                '[class*="tap-to"] { display: none !important; }',
                '[class*="sound-btn"] { display: none !important; }',
                '[class*="audio-btn"] { display: none !important; }',
                '[class*="mute-overlay"] { display: none !important; }',
                '[class*="volume-overlay"] { display: none !important; }',
                '.overlay-unmute { display: none !important; }',
                '[class*="click-to-unmute"] { display: none !important; }',
                '[class*="tap_unmute"] { display: none !important; }'
            ].join('\n');
            document.head.appendChild(style);
        }
        
        // 5. Remove floating ad overlays (high z-index, absolute/fixed, not video related)
        document.querySelectorAll('div, a').forEach(function(el) {
            if (el.id === 'boxboxbox-unmute-fix') return;
            if (el.querySelector('video') || el.closest('video') || 
                el.classList.contains('plyr') || el.closest('.plyr') ||
                el.closest('[class*="player"]') || el.closest('[id*="player"]')) return;
                
            var cs = window.getComputedStyle(el);
            if ((cs.position === 'fixed' || cs.position === 'absolute') && 
                cs.zIndex && parseInt(cs.zIndex) > 50 &&
                el.offsetWidth > 100 && el.offsetHeight > 100) {
                
                // Don't hide the player itself if it's fullscreen
                if (el.offsetWidth < window.innerWidth * 0.9) {
                    el.style.display = 'none';
                }
            }
        });
    }

    // Run aggressively at first, then settle into a poll
    [100, 300, 800, 1500, 2500, 4000, 6000, 8000, 10000, 15000].forEach(function(ms) {
        setTimeout(forceUnmuteAndClean, ms);
    });
    setInterval(forceUnmuteAndClean, 3000); // Check sync and keep clean every 3s
})();
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun LiveStreamWebView(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    var isLoading by remember { mutableStateOf(true) }
    var selectedProvider by remember { mutableStateOf(STREAM_PROVIDERS[0]) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showControls by remember { mutableStateOf(true) }
    
    // Force landscape + immersive on mount
    DisposableEffect(Unit) {
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.let { window ->
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                window.statusBarColor = android.graphics.Color.BLACK
                window.navigationBarColor = android.graphics.Color.BLACK
            }
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView fills entire screen
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    
                    setBackgroundColor(android.graphics.Color.BLACK)
                    
                    // Toggle controls on tap (WebView eats Compose touches)
                    var lastTouchTime = 0L
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val now = System.currentTimeMillis()
                            if (now - lastTouchTime < 300) {
                                // Double-tap: ignore (let player handle)
                            } else {
                                // Single tap: toggle controls after a brief delay
                                lastTouchTime = now
                                postDelayed({
                                    // Only toggle if no second tap came (not a double-tap)
                                    if (System.currentTimeMillis() - lastTouchTime >= 280) {
                                        showControls = !showControls
                                    }
                                }, 300)
                            }
                        }
                        false // Pass touch through to WebView
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?, request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (isAdUrl(url)) return true
                            val host = Uri.parse(url).host ?: return true
                            val allowed = listOf(
                                "adffdafdsafds.sbs", "hakunamatata5.org",
                                "embedsports.top", "strmd.top",
                                "amazonaws.com", "cdn.plyr.io", "plyr.io"
                            )
                            return allowed.none { host.contains(it) }
                        }
                        
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            view?.evaluateJavascript(UNMUTE_AND_CLEANUP_JS, null)
                        }
                        
                        override fun shouldInterceptRequest(
                            view: WebView?, request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            if (isAdUrl(url)) {
                                return WebResourceResponse(
                                    "text/plain", "UTF-8",
                                    java.io.ByteArrayInputStream("".toByteArray())
                                )
                            }
                            return null
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?, isDialog: Boolean,
                            isUserGesture: Boolean, resultMsg: android.os.Message?
                        ): Boolean = false
                        
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                android.util.Log.d("LiveStreamJS", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                            }
                            return true
                        }
                    }
                    
                    webView = this
                    loadUrl(selectedProvider.url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE6007E),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "LOADING STREAM",
                        fontFamily = michromaFont,
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
        
        // Floating controls - tap to show, auto-hides
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top: gradient + close + LIVE badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF0040))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "d")
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                tween(800), RepeatMode.Reverse
                            ), label = "a"
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = dotAlpha))
                        )
                        Text(
                            text = "LIVE",
                            fontFamily = michromaFont,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                // Bottom: Channel switcher pill
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    STREAM_PROVIDERS.forEach { provider ->
                        val isSelected = selectedProvider == provider
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFFE6007E) else Color.Transparent
                                )
                                .clickable {
                                    if (!isSelected) {
                                        selectedProvider = provider
                                        webView?.loadUrl(provider.url)
                                        isLoading = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = provider.label,
                                fontFamily = michromaFont,
                                fontSize = 8.sp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isAdUrl(url: String): Boolean {
    val lowerUrl = url.lowercase()
    return AD_DOMAINS.any { lowerUrl.contains(it) }
}
