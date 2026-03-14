package com.f1tracker.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized analytics logger wrapping Firebase Analytics.
 * Initialize once from Application.onCreate(), then call from anywhere.
 */
object AnalyticsLogger {

    private lateinit var analytics: FirebaseAnalytics

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
        // Set app version as a persistent user property
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        analytics.setUserProperty("app_version", versionName)
    }

    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        if (!::analytics.isInitialized) return
        val bundle = Bundle().apply(params)
        analytics.logEvent(event, bundle)
        android.util.Log.d("Analytics", "Event: $event | ${bundle.keySet().joinToString { "$it=${bundle.get(it)}" }}")
    }

    // ── Navigation ──────────────────────────────────────────────

    fun screenView(screenName: String) = log("screen_view") {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
    }

    fun deepLinkOpened(url: String, targetTab: String) = log("deep_link_opened") {
        putString("url", url.take(100)) // Truncate for GA limits
        putString("target_tab", targetTab)
    }

    // ── Race ────────────────────────────────────────────────────

    fun raceViewed(raceName: String, round: String) = log("race_viewed") {
        putString("race_name", raceName)
        putString("race_round", round)
    }

    fun sessionResultViewed(sessionName: String, raceName: String) = log("session_result_viewed") {
        putString("session_name", sessionName)
        putString("race_name", raceName)
    }

    // ── Live Timing ─────────────────────────────────────────────

    fun liveTimingConnected() = log("live_timing_connected")

    fun liveTimingDisconnected(reason: String) = log("live_timing_disconnected") {
        putString("reason", reason.take(100))
    }

    // ── Stream ──────────────────────────────────────────────────

    fun streamLaunched(channel: String, method: String) = log("stream_launched") {
        putString("channel", channel)
        putString("method", method) // "ace_player" or "preview"
    }

    fun streamPreviewFailed(channel: String, error: String) = log("stream_preview_failed") {
        putString("channel", channel)
        putString("error", error.take(100))
    }

    // ── Feed / Content ──────────────────────────────────────────

    fun articleOpened(url: String?) = log("article_opened") {
        putString("url", url?.take(100) ?: "")
    }

    fun videoOpened(videoId: String) = log("video_opened") {
        putString("video_id", videoId)
    }

    fun feedTabChanged(tabName: String) = log("feed_tab_changed") {
        putString("tab_name", tabName)
    }

    // ── Podcast ─────────────────────────────────────────────────

    fun podcastPlayed(episodeTitle: String) = log("podcast_played") {
        putString("episode_title", episodeTitle.take(100))
    }

    // ── App Updates ─────────────────────────────────────────────

    fun appUpdateShown(version: String) = log("app_update_shown") {
        putString("version", version)
    }

    fun appUpdateStarted(version: String) = log("app_update_started") {
        putString("version", version)
    }

    // ── Notifications ───────────────────────────────────────────

    fun notificationReceived(title: String, topic: String) = log("notification_received") {
        putString("title", title.take(100))
        putString("topic", topic)
    }
}
