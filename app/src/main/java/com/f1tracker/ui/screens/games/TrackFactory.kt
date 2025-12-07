package com.f1tracker.ui.screens.games

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/**
 * Track factory with Masters Circuit only.
 * Scaled up to fill more of the available space.
 */
object TrackFactory {

    data class TrackDefinition(
        val id: String,
        val name: String,
        val difficulty: Int,
        val generator: () -> List<Offset>
    )

    val tracks = listOf(
        TrackDefinition("track_masters", "Masters Circuit", 5) { generateMasters() }
    )

    fun getTrack(id: String): TrackData? {
        val def = tracks.find { it.id == id } ?: return null
        val rawPoints = def.generator()
        return createTrackData(def.name, rawPoints)
    }

    /**
     * Masters Circuit - scaled up from original 0.15-0.82 range to 0.10-0.90 range.
     * This fills more of the available canvas space.
     */
    private fun generateMasters(): List<Offset> {
        // Original coordinates from GeoJSON
        val original = listOf(
            Offset(0.40f, 0.15f), Offset(0.50f, 0.15f), Offset(0.60f, 0.15f),
            Offset(0.68f, 0.15f), Offset(0.72f, 0.17f), Offset(0.75f, 0.20f),
            Offset(0.77f, 0.25f), Offset(0.77f, 0.30f), Offset(0.75f, 0.35f),
            Offset(0.70f, 0.38f), Offset(0.65f, 0.38f), Offset(0.60f, 0.40f),
            Offset(0.58f, 0.45f), Offset(0.60f, 0.50f), Offset(0.65f, 0.52f),
            Offset(0.72f, 0.52f), Offset(0.78f, 0.55f), Offset(0.82f, 0.60f),
            Offset(0.82f, 0.68f), Offset(0.78f, 0.75f), Offset(0.72f, 0.78f),
            Offset(0.65f, 0.78f), Offset(0.58f, 0.75f), Offset(0.55f, 0.70f),
            Offset(0.50f, 0.68f), Offset(0.45f, 0.70f), Offset(0.40f, 0.75f),
            Offset(0.32f, 0.78f), Offset(0.25f, 0.78f), Offset(0.20f, 0.75f),
            Offset(0.18f, 0.70f), Offset(0.18f, 0.65f), Offset(0.20f, 0.60f),
            Offset(0.25f, 0.58f), Offset(0.30f, 0.55f), Offset(0.32f, 0.50f),
            Offset(0.30f, 0.45f), Offset(0.25f, 0.42f), Offset(0.20f, 0.42f),
            Offset(0.18f, 0.38f), Offset(0.18f, 0.32f), Offset(0.17f, 0.25f),
            Offset(0.15f, 0.20f), Offset(0.15f, 0.15f), Offset(0.20f, 0.15f),
            Offset(0.30f, 0.15f), Offset(0.40f, 0.15f)
        )
        
        // Calculate bounds
        var minX = 1f
        var maxX = 0f
        var minY = 1f
        var maxY = 0f
        
        original.forEach { p ->
            minX = minOf(minX, p.x)
            maxX = maxOf(maxX, p.x)
            minY = minOf(minY, p.y)
            maxY = maxOf(maxY, p.y)
        }
        
        // Scale up to use more space (0.05 to 0.95 instead of 0.15 to 0.82)
        val targetMin = 0.05f
        val targetMax = 0.95f
        val targetRange = targetMax - targetMin
        
        val origRangeX = maxX - minX
        val origRangeY = maxY - minY
        
        return original.map { p ->
            val normalizedX = (p.x - minX) / origRangeX
            val normalizedY = (p.y - minY) / origRangeY
            
            Offset(
                targetMin + normalizedX * targetRange,
                targetMin + normalizedY * targetRange
            )
        }
    }

    private fun createTrackData(name: String, rawPoints: List<Offset>): TrackData {
        if (rawPoints.isEmpty()) return TrackData(name, "Unknown", emptyList())

        val pointDistances = mutableListOf<Float>()
        var currentDist = 0f
        pointDistances.add(0f)

        for (i in 0 until rawPoints.size - 1) {
            currentDist += distance(rawPoints[i], rawPoints[i + 1])
            pointDistances.add(currentDist)
        }
        if (rawPoints.isNotEmpty()) {
            currentDist += distance(rawPoints.last(), rawPoints.first())
        }

        return TrackData(
            name = name,
            location = "Generated",
            points = rawPoints,
            totalDistance = currentDist,
            pointDistances = pointDistances
        )
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
