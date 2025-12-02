package com.f1tracker.util

/**
 * Instagram account classification and utilities
 */
object InstagramConstants {
    
    /**
     * Official F1 team accounts - display with verified badge
     */
    val OFFICIAL_ACCOUNTS = setOf(
        "f1",
        "redbullracing",
        "scuderiaferrari",
        "mclaren",
        "mercedesamgf1"
    )
    
    /**
     * Check if an account is official
     */
    fun isOfficialAccount(username: String?): Boolean {
        return username != null && username in OFFICIAL_ACCOUNTS
    }
}
