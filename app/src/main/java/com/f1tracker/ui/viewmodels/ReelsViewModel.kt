package com.f1tracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.models.InstagramPost
import com.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val repository: F1Repository
) : ViewModel() {

    private val _reels = MutableStateFlow<List<InstagramPost>>(emptyList())
    val reels: StateFlow<List<InstagramPost>> = _reels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchReels()
    }

    /**
     * Fetch reels with optional force refresh.
     * 
     * @param forceRefresh If true, bypasses cache and fetches fresh data.
     *                     Use this for pull-to-refresh actions.
     */
    fun fetchReels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            
            repository.getInstagramReels(forceRefresh = forceRefresh)
                .onSuccess { posts ->
                    _reels.value = sortByEngagement(posts)
                }
                .onFailure {
                    // Handle error (maybe retry or show empty state)
                    android.util.Log.e("ReelsViewModel", "Failed to fetch reels", it)
                }
            
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
    
    /**
     * Score posts by engagement (likes + comments*3) with time decay.
     * Formula: (likes + comments*3) / (hours_ago + 2)^1.5
     * Also applies author diversity (max 2 consecutive from same author for reels).
     */
    private fun sortByEngagement(posts: List<InstagramPost>): List<InstagramPost> {
        val now = java.time.Instant.now()
        
        fun getScore(post: InstagramPost): Double {
            val likes = post.like_count.toDouble()
            val comments = post.comments_count.toDouble()
            val engagement = likes + (comments * 3) // Comments worth 3x likes
            
            val hoursAgo = try {
                val postTime = java.time.Instant.parse(post.timestamp)
                java.time.Duration.between(postTime, now).toHours().toDouble()
            } catch (e: Exception) { 100.0 }
            
            // Time decay: older posts score lower
            val timeDecay = Math.pow(hoursAgo + 2.0, 1.5)
            var score = engagement / timeDecay
            
            // Debuff F1 official account - they have massive follower advantage
            // This balances their likes to give smaller accounts a fair chance
            if (post.author == "f1") {
                score *= 0.4
            }
            
            // Add slight randomization (±15%) so refresh feels fresh
            val randomFactor = 0.85 + (Math.random() * 0.30)
            return score * randomFactor
        }
        
        // Sort by score
        val scoredPosts = posts.sortedByDescending { getScore(it) }
        
        // Apply author diversity: no more than 2 consecutive from same author (stricter for reels)
        val diversified = mutableListOf<InstagramPost>()
        val remaining = scoredPosts.toMutableList()
        
        while (remaining.isNotEmpty()) {
            // Count consecutive from last author
            val lastAuthor = diversified.lastOrNull()?.author
            val consecutiveCount = if (lastAuthor != null) {
                diversified.takeLastWhile { it.author == lastAuthor }.size
            } else 0
            
            // Find next post that doesn't exceed 2 consecutive from same author
            val nextPost = if (consecutiveCount >= 2) {
                remaining.firstOrNull { it.author != lastAuthor } ?: remaining.first()
            } else {
                remaining.first()
            }
            
            diversified.add(nextPost)
            remaining.remove(nextPost)
        }
        
        return diversified
    }
    
    /**
     * Refresh reels, bypassing cache.
     * Call this from pull-to-refresh UI.
     */
    fun refresh() {
        fetchReels(forceRefresh = true)
    }
    
    // Throttle error-triggered refreshes to once per 2 minutes
    private var lastErrorRefresh: Long = 0
    private val ERROR_REFRESH_COOLDOWN = 2 * 60 * 1000L // 2 minutes
    
    /**
     * Called when a video fails to play. Triggers a refresh if cooldown has passed.
     * This handles expired Instagram CDN URLs by fetching fresh data.
     */
    fun onVideoError() {
        val now = System.currentTimeMillis()
        if (now - lastErrorRefresh > ERROR_REFRESH_COOLDOWN && !_isRefreshing.value) {
            android.util.Log.d("ReelsViewModel", "Video error detected, refreshing reels data...")
            lastErrorRefresh = now
            fetchReels(forceRefresh = true)
        }
    }
}
