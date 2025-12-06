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
                    _reels.value = posts
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
     * Refresh reels, bypassing cache.
     * Call this from pull-to-refresh UI.
     */
    fun refresh() {
        fetchReels(forceRefresh = true)
    }
}

