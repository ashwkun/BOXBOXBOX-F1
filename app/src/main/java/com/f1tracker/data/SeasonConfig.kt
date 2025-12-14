package com.f1tracker.data

/**
 * Centralized season configuration.
 * Update this single file when a new season starts.
 */
object SeasonConfig {
    /**
     * The current F1 season year as a string (for API calls)
     */
    const val CURRENT_SEASON = "2025"
    
    /**
     * The current season year as an integer
     */
    const val CURRENT_SEASON_INT = 2025
    
    /**
     * List of available years for historical data (newest first)
     */
    val AVAILABLE_YEARS = (2012..CURRENT_SEASON_INT).toList().reversed()
}
