package com.f1tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1tracker.R
import com.f1tracker.ui.viewmodels.HomeViewModel
import com.google.gson.GsonBuilder

@Composable
fun StandingsScreen(
    viewModel: HomeViewModel = remember { HomeViewModel.getInstance() }
) {
    val brigendsFont = FontFamily(Font(R.font.brigends_expanded, FontWeight.Normal))
    val michromaFont = FontFamily(Font(R.font.michroma, FontWeight.Normal))
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Drivers, 1 = Constructors
    
    val driverStandings by viewModel.driverStandings.collectAsState()
    val constructorStandings by viewModel.constructorStandings.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Innovative Tab Switcher with sliding indicator
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            val tabWidth = maxWidth / 2
            
            // Background container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF0F0F0F),
                        RoundedCornerShape(50.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(4.dp)
            ) {
                // Animated sliding indicator
                val indicatorOffset by animateDpAsState(
                    targetValue = if (selectedTab == 0) 0.dp else tabWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tab_indicator"
                )
                
                Box(
                    modifier = Modifier
                        .width(tabWidth - 8.dp)
                        .height(40.dp)
                        .offset(x = indicatorOffset)
                        .background(
                            Color(0xFFE6007E).copy(alpha = 0.4f), // Further reduced opacity
                            RoundedCornerShape(50.dp)
                        )
                )
                
                // Tab buttons
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Drivers Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DRIVERS",
                            fontFamily = brigendsFont,
                            fontSize = 11.sp,
                            color = Color.White,
                            letterSpacing = 1.2.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    // Constructors Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CONSTRUCTORS",
                            fontFamily = brigendsFont,
                            fontSize = 11.sp,
                            color = Color.White,
                            letterSpacing = 1.2.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        // JSON display based on selected tab with swipe gesture
        var swipeOffset by remember { mutableStateOf(0f) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > 150 && selectedTab == 1) {
                                // Swipe right - go to drivers
                                selectedTab = 0
                            } else if (swipeOffset < -150 && selectedTab == 0) {
                                // Swipe left - go to constructors
                                selectedTab = 1
                            }
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffset += dragAmount
                        }
                    )
                }
        ) {
            val scrollState = rememberScrollState()
            val gson = remember { GsonBuilder().setPrettyPrinting().create() }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "DRIVER STANDINGS JSON" else "CONSTRUCTOR STANDINGS JSON",
                    fontFamily = brigendsFont,
                    fontSize = 14.sp,
                    color = Color(0xFFE6007E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val jsonText = if (selectedTab == 0) {
                    driverStandings?.let { gson.toJson(it) } ?: "Loading driver standings..."
                } else {
                    constructorStandings?.let { gson.toJson(it) } ?: "Loading constructor standings..."
                }
                
                Text(
                    text = jsonText,
                    fontFamily = michromaFont,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
