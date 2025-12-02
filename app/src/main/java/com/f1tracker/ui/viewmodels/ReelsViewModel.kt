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

    init {
        fetchReels()
    }

    fun fetchReels() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getInstagramReels()
                .onSuccess { posts ->
                    _reels.value = posts
                }
                .onFailure {
                    // Handle error (maybe retry or show empty state)
                }
            _isLoading.value = false
        }
    }
}
