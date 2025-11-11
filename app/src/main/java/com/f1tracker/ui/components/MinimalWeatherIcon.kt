package com.f1tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.f1tracker.data.models.WeatherIcon

@Composable
fun MinimalWeatherIcon(
    weatherIcon: WeatherIcon,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val icon = when (weatherIcon) {
        WeatherIcon.CLEAR -> Icons.Filled.WbSunny
        WeatherIcon.PARTLY_CLOUDY -> Icons.Filled.WbCloudy
        WeatherIcon.CLOUDY -> Icons.Filled.Cloud
        WeatherIcon.OVERCAST -> Icons.Filled.CloudQueue
        WeatherIcon.FOG -> Icons.Filled.Dehaze
        WeatherIcon.DRIZZLE -> Icons.Filled.WaterDrop
        WeatherIcon.RAIN -> Icons.Filled.WaterDrop
        WeatherIcon.HEAVY_RAIN -> Icons.Filled.WaterDrop
        WeatherIcon.FREEZING_RAIN -> Icons.Filled.AcUnit
        WeatherIcon.SNOW -> Icons.Filled.AcUnit
        WeatherIcon.THUNDERSTORM -> Icons.Filled.Bolt
    }
    
    Icon(
        imageVector = icon,
        contentDescription = weatherIcon.name,
        tint = color,
        modifier = modifier
    )
}

