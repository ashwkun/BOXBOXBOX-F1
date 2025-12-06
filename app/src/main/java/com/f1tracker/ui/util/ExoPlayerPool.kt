package com.f1tracker.ui.util

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Collections

/**
 * A pool of ExoPlayer instances for efficient reuse.
 * 
 * This avoids the overhead of creating/destroying ExoPlayer instances
 * on every page of the VerticalPager, significantly improving scroll performance.
 */
object ExoPlayerPool {
    private const val TAG = "ExoPlayerPool"
    private const val POOL_SIZE = 3
    
    private val availablePlayers = ConcurrentLinkedQueue<ExoPlayer>()
    private val inUsePlayers = Collections.synchronizedSet(mutableSetOf<ExoPlayer>())
    private var applicationContext: Context? = null
    
    /**
     * Initialize the pool with application context.
     * Call this from Application.onCreate()
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        Log.d(TAG, "ExoPlayerPool initialized")
    }
    
    /**
     * Acquire a player from the pool.
     * If no players are available and we haven't reached max pool size, create a new one.
     * 
     * @param context Context for creating new players (falls back to app context)
     * @return An ExoPlayer instance ready for use
     */
    @Synchronized
    fun acquire(context: Context): ExoPlayer {
        val ctx = applicationContext ?: context.applicationContext
        
        // Try to get an existing player from the pool
        var player = availablePlayers.poll()
        
        if (player == null) {
            // Create a new player if pool is empty
            Log.d(TAG, "Creating new ExoPlayer (total in use: ${inUsePlayers.size})")
            player = createPlayer(ctx)
        } else {
            Log.d(TAG, "Reusing ExoPlayer from pool (available: ${availablePlayers.size})")
        }
        
        inUsePlayers.add(player)
        return player
    }
    
    /**
     * Release a player back to the pool for reuse.
     * The player is stopped and cleared but not destroyed.
     */
    @Synchronized
    fun release(player: ExoPlayer) {
        if (!inUsePlayers.remove(player)) {
            Log.w(TAG, "Releasing player that wasn't in use")
            return
        }
        
        // Reset the player for reuse
        player.stop()
        player.clearMediaItems()
        player.volume = 1f // Reset volume to max
        
        // Only keep up to POOL_SIZE players
        if (availablePlayers.size < POOL_SIZE) {
            availablePlayers.offer(player)
            Log.d(TAG, "Returned player to pool (available: ${availablePlayers.size})")
        } else {
            // Pool is full, release this player
            player.release()
            Log.d(TAG, "Pool full, releasing player")
        }
    }
    
    /**
     * Release all players when the app is going to background or shutting down.
     * Call this from Activity.onDestroy() or when no longer needed.
     */
    @Synchronized
    fun releaseAll() {
        Log.d(TAG, "Releasing all players")
        
        // Release in-use players
        inUsePlayers.forEach { it.release() }
        inUsePlayers.clear()
        
        // Release pooled players
        var player = availablePlayers.poll()
        while (player != null) {
            player.release()
            player = availablePlayers.poll()
        }
    }
    
    /**
     * Get the current number of players in use (for debugging)
     */
    fun getInUseCount(): Int = inUsePlayers.size
    
    /**
     * Get the current number of available players in pool (for debugging)
     */
    fun getAvailableCount(): Int = availablePlayers.size
    
    private fun createPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .build()
    }
}

/**
 * Extension function to prepare and configure a pooled player for video playback.
 */
fun ExoPlayer.prepareForVideo(
    mediaUrl: String,
    playWhenReady: Boolean = true,
    repeatMode: Int = Player.REPEAT_MODE_ONE,
    volume: Float = 0f
) {
    setMediaItem(MediaItem.fromUri(mediaUrl))
    this.playWhenReady = playWhenReady
    this.repeatMode = repeatMode
    this.volume = volume
    prepare()
}
