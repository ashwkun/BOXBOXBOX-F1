package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.data.models.RaceWeekendState
import com.f1tracker.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Preload data during splash
    val viewModel = remember { HomeViewModel.getInstance() }
    val raceState by viewModel.raceWeekendState.collectAsState()
    
    // Keep system bars black during splash
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
    }
    
    LaunchedEffect(Unit) {
        startAnimation = true
        // Wait for minimum splash duration OR data to load
        val minDelay = async { delay(2000) }
        
        // Wait for either data to load or timeout
        var waited = 0L
        while (raceState is RaceWeekendState.Loading && waited < 5000) {
            delay(100)
            waited += 100
        }
        
        minDelay.await() // Ensure minimum splash time
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background glow
        PulsatingGlow(startAnimation)
        
        // Main logo
        LogoAnimation(startAnimation)
    }
}

@Composable
fun LogoAnimation(startAnimation: Boolean) {
    // Load custom font
    val brigendsFont = FontFamily(
        Font(R.font.brigends_expanded, FontWeight.Normal)
    )
    
    // Scale animation for the entire logo
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 80f
        ),
        label = "logoScale"
    )
    
    // Fade in animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    
    // Individual word animations
    val firstBoxAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "firstBoxAlpha"
    )
    
    val secondBoxAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(500, delayMillis = 400),
        label = "secondBoxAlpha"
    )
    
    val thirdBoxAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(500, delayMillis = 600),
        label = "thirdBoxAlpha"
    )
    
    // Dot appears LAST with a pop animation
    val dotAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(400, delayMillis = 900),
        label = "dotAlpha"
    )
    
    val dotScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 200f
        ),
        label = "dotScale"
    )
    
    // Pulsing dot animation after initial fade
    val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    
    Box(
        modifier = Modifier
            .scale(logoScale)
            .alpha(logoAlpha)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // First BOX (White)
            Text(
                text = "BOX",
                fontSize = 28.sp,
                fontFamily = brigendsFont,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(firstBoxAlpha)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // Second BOX (Pink/Magenta)
            Text(
                text = "BOX",
                fontSize = 28.sp,
                fontFamily = brigendsFont,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFFF0080),
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(secondBoxAlpha)
            )
            
            // Dot separator (Pink, pulsing after fade in, appears LAST with pop)
            Box(
                modifier = Modifier
                    .scale(dotScale)
                    .alpha(dotAlpha)
            ) {
                Text(
                    text = "·",
                    fontSize = 28.sp,
                    fontFamily = brigendsFont,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF0080).copy(alpha = if (startAnimation) dotPulse else 1f),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // Third BOX (White)
            Text(
                text = "BOX",
                fontSize = 28.sp,
                fontFamily = brigendsFont,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(thirdBoxAlpha)
            )
        }
    }
}

@Composable
fun PulsatingGlow(startAnimation: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.3f else 0f,
        animationSpec = tween(1000),
        label = "glowAlpha"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Pink glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(glowScale)
                .blur(100.dp)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF0080).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // White accent glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(glowScale * 0.8f)
                .blur(60.dp)
                .alpha(glowAlpha * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

