package com.f1tracker.data.local

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ESPN Race ID data classes
data class ESPNRaceIdData(
    val season: String,
    val races: List<ESPNRaceMapping>
)

data class ESPNRaceMapping(
    val round: Int,
    val raceName: String,
    val espnId: String,
    val location: String
)

object ESPNRaceIdProvider {
    private var raceIdMap: Map<Int, String> = emptyMap()
    
    fun loadData(jsonString: String) {
        try {
            val data = Gson().fromJson(jsonString, ESPNRaceIdData::class.java)
            raceIdMap = data.races.associate { it.round to it.espnId }
            Log.d("ESPNRaceIdProvider", "Loaded ${raceIdMap.size} ESPN race IDs")
        } catch (e: Exception) {
            Log.e("ESPNRaceIdProvider", "Error loading ESPN race IDs", e)
        }
    }
    
    fun getESPNIdForRound(round: Int): String? {
        return raceIdMap[round]
    }
}
