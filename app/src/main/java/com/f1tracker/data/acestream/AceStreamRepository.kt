package com.f1tracker.data.acestream

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Repository for interacting with the locally-installed Ace Stream Engine
 * via its HTTP API at 127.0.0.1:6878.
 *
 * The user must have the Ace Stream Engine app installed on their device.
 * This repository handles:
 * - Checking if the engine is installed
 * - Checking if the engine is running
 * - Searching for F1 channels
 * - Building launch intents
 */
class AceStreamRepository {

    companion object {
        private const val TAG = "AceStreamRepo"
        private const val ENGINE_BASE_URL = "http://127.0.0.1:6878"
        private const val SEARCH_ENDPOINT = "/search"
        private const val VERSION_ENDPOINT = "/webui/api/service?method=get_version"
        private const val CONNECT_TIMEOUT_MS = 3000
        private const val READ_TIMEOUT_MS = 10000

        // Known Ace Stream package names
        val ACE_STREAM_PACKAGES = listOf(
            "org.acestream.node",          // Play Store version
            "org.acestream.media",
            "org.acestream.engine",
            "org.acestream.core",
            "org.acestream.media.atv"      // Android TV variant
        )

        // F1-related search queries — cast a wide net
        val F1_SEARCH_QUERIES = listOf(
            "Sky Sports F1",
            "Sky F1",
            "F1",
            "Formula 1",
            "F1 TV",
            "ESPN F1",
            "DAZN F1",
            "Star Sports",
            "Sport TV",
            "Movistar F1",
            "Canal+ F1",
            "Viaplay F1"
        )

        // Broad regex to match any F1-relevant channel
        val F1_CHANNEL_REGEX = Regex(
            """(?i)(f1|formula\s*1|sky\s*sports?\s*f1|espn|dazn|star\s*sports|sport\s*tv|canal\s*\+?|viaplay|movistar|rtbf|servus|sky\s*sport|tsn|ssc)"""
        )

        // Regex to reject clearly non-F1 channels that slip through
        val NON_F1_REJECT_REGEX = Regex(
            """(?i)(cricket|football|soccer|baseball|basketball|nba|nfl|tennis|golf|boxing|ufc|mma|wrestling|hockey|rugby|volleyball|handball|cycling|swimming|darts)"""
        )

        // Regex for known foreign language primary broadcasters to de-prioritize
        val FOREIGN_BROADCASTER_REGEX = Regex(
            """(?i)(sky\s*sport\s*(de|it)|servus|orf|rtbf|canal\s*\+?\s*(fr)?|movistar|dazn\s*(es|de|it)|ziggo)"""
        )

        // Quality detection from channel names
        val QUALITY_REGEX_FHD = Regex("""(?i)(1080[pi]?|fhd|full\s*hd)""")
        val QUALITY_REGEX_HD = Regex("""(?i)(720[pi]?|\bhd\b|high\s*def)""")
        val QUALITY_REGEX_4K = Regex("""(?i)(4k|uhd|2160)""")

        // English language codes
        val ENGLISH_LANG_CODES = setOf("eng", "en")

        @Volatile
        private var instance: AceStreamRepository? = null

        fun getInstance(): AceStreamRepository {
            return instance ?: synchronized(this) {
                instance ?: AceStreamRepository().also { instance = it }
            }
        }

        /**
         * Extract quality label from channel name.
         */
        fun getQualityLabel(name: String): String {
            return when {
                QUALITY_REGEX_4K.containsMatchIn(name) -> "4K"
                QUALITY_REGEX_FHD.containsMatchIn(name) -> "FHD"
                QUALITY_REGEX_HD.containsMatchIn(name) -> "HD"
                else -> "SD"
            }
        }

        /**
         * Check if a channel is likely English-language.
         */
        fun isEnglish(channel: AceStreamChannel): Boolean {
            // Check language metadata strictly
            if (channel.languages?.any { it.lowercase() in ENGLISH_LANG_CODES } == true) return true
            
            val name = channel.name.lowercase()
            
            // If the name explicitly claims to be Spanish, German, Italian, etc., it's not English
            if (Regex("""\b(de|ita?|esp?|fra?|nld?|pol?|ru|ger|spa|cze)\b""").containsMatchIn(name)) return false
            if (FOREIGN_BROADCASTER_REGEX.containsMatchIn(name)) return false

            // Check name for pure English channel indicators
            if (Regex("""\b(sky\s*sports?\s*(f1|uk|main\s*event)|espn|star\s*sports|bt\s*sport|tsn|fox\s*sports|supersport|ssc)\b""").containsMatchIn(name)) return true
            
            return false
        }
    }

    private val gson = Gson()

    /**
     * Check if the Ace Stream Engine is currently running by pinging its version endpoint.
     * This acts as a reliable fallback for Android 11+ where package queries might fail.
     */
    private fun isEngineRunningSync(): Boolean {
        return try {
            val url = java.net.URL("$ENGINE_BASE_URL$VERSION_ENDPOINT")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"

            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                // We got a 200 OK from local port 6878, engine is definitely running
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if any Ace Stream app is installed on the device.
     */
    fun isEngineInstalled(context: Context): Boolean {
        // 1. Try standard PackageManager check
        val pm = context.packageManager
        val isInstalledViaPm = ACE_STREAM_PACKAGES.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        
        if (isInstalledViaPm) return true
        
        // 2. Fallback: Check if the local HTTP port is active
        // Android 11+ package visibility is sometimes flaky even with <queries>
        // If the engine is currently running in the background, this will catch it
        return isEngineRunningSync()
    }

    /**
     * Get the installed Ace Stream package name (for launching).
     */
    fun getInstalledPackage(context: Context): String? {
        val pm = context.packageManager
        return ACE_STREAM_PACKAGES.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Check if the Ace Stream Engine is currently running by pinging its version endpoint.
     */
    suspend fun isEngineRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = httpGet("$ENGINE_BASE_URL$VERSION_ENDPOINT")
            if (response != null) {
                val versionResponse = gson.fromJson(response, AceStreamVersionResponse::class.java)
                versionResponse.result != null
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Engine not running: ${e.message}")
            false
        }
    }

    /**
     * Search for F1 channels using the Ace Stream Engine's built-in search API.
     * Searches multiple queries, deduplicates, filters for F1, and sorts by
     * English preference → quality → availability.
     */
    suspend fun searchF1Channels(): List<AceStreamChannel> = withContext(Dispatchers.IO) {
        val allChannels = mutableMapOf<String, AceStreamChannel>() // Dedup by infohash

        for (query in F1_SEARCH_QUERIES) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                // Add category=sport to narrow results
                val url = "$ENGINE_BASE_URL$SEARCH_ENDPOINT?query=$encodedQuery&category=sport"
                val response = httpGet(url)

                if (response != null) {
                    val searchResponse = gson.fromJson(response, AceStreamSearchResponse::class.java)
                    searchResponse.result?.results?.forEach { group ->
                        group.items
                            .filter { it.isAvailable || it.status == 1 }
                            // Must match F1 regex in either item name or group name
                            .filter { F1_CHANNEL_REGEX.containsMatchIn(it.name) || F1_CHANNEL_REGEX.containsMatchIn(group.name) }
                            // Reject clearly non-F1 sports channels
                            .filter { !NON_F1_REJECT_REGEX.containsMatchIn(it.name) }
                            .forEach { channel ->
                                // Keep the one with higher availability
                                val existing = allChannels[channel.infohash]
                                if (existing == null || channel.availability > existing.availability) {
                                    allChannels[channel.infohash] = channel
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search failed for query '$query': ${e.message}")
            }
        }

        // Sort by:
        // 1. Is it definitely English?
        // 2. Is it DEFINITELY NOT a foreign broadcaster?
        // 3. Higher quality
        // 4. Available status
        // 5. Higher availability
        allChannels.values.toList().sortedWith(
            compareByDescending<AceStreamChannel> { isEnglish(it) }
                .thenBy { FOREIGN_BROADCASTER_REGEX.containsMatchIn(it.name) } // False (not foreign) comes before True
                .thenByDescending {
                    when (getQualityLabel(it.name)) {
                        "4K" -> 4; "FHD" -> 3; "HD" -> 2; else -> 1
                    }
                }
                .thenByDescending { it.status }
                .thenByDescending { it.availability }
        )
    }

    /**
     * Build an Intent to launch an Ace Stream by its infohash.
     * Note: "acestream://" followed directly by a hash is interpreted as a Content ID (CID).
     * Since we have an infohash, we must explicitly pass it as a query parameter.
     */
    fun buildStreamIntent(infohash: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("acestream:?infohash=$infohash")
        }
    }

    /**
     * Build an Intent to open the Ace Stream Engine app (to start the service).
     */
    fun buildLaunchEngineIntent(context: Context): Intent? {
        val pkg = getInstalledPackage(context)
        return if (pkg != null) {
            context.packageManager.getLaunchIntentForPackage(pkg)
        } else {
            null
        }
    }

    /**
     * Build an Intent to install Ace Stream from the Play Store.
     */
    fun buildInstallIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=org.acestream.node")
            setPackage("com.android.vending")
        }
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "HTTP ${connection.responseCode} from $urlString")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "HTTP GET failed for $urlString: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
