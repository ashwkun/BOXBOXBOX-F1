package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("hourly") val hourly: HourlyWeather,
    @SerializedName("hourly_units") val hourlyUnits: HourlyUnits
)

data class HourlyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperature: List<Double>,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerializedName("weather_code") val weatherCode: List<Int>
)

data class HourlyUnits(
    @SerializedName("temperature_2m") val temperature: String,
    @SerializedName("precipitation_probability") val precipitationProbability: String
)

data class SessionWeather(
    val temperature: Int,
    val rainChance: Int,
    val weatherCode: Int,
    val weatherIcon: WeatherIcon
)

enum class WeatherIcon {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    FREEZING_RAIN,
    SNOW,
    THUNDERSTORM;
    
    companion object {
        fun fromWMOCode(code: Int): WeatherIcon {
            return when (code) {
                0 -> CLEAR
                1, 2 -> PARTLY_CLOUDY
                3 -> CLOUDY
                45, 48 -> FOG
                51, 53, 55, 56, 57 -> DRIZZLE
                61, 63, 80, 81 -> RAIN
                65, 82 -> HEAVY_RAIN
                66, 67 -> FREEZING_RAIN
                71, 73, 75, 77, 85, 86 -> SNOW
                95, 96, 99 -> THUNDERSTORM
                else -> PARTLY_CLOUDY
            }
        }
    }
}


