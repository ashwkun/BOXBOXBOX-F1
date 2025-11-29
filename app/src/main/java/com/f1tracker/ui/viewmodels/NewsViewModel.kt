package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.models.NewsArticle
import com.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: F1Repository
) : ViewModel() {

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.getF1News()
            result.onSuccess { articles ->
                _newsArticles.value = articles
                Log.d("NewsViewModel", "Loaded ${articles.size} news articles")
            }.onFailure { e ->
                Log.e("NewsViewModel", "Failed to load news", e)
            }
            _isRefreshing.value = false
        }
    }
    
    fun refreshNews() {
        loadNews()
    }

    // Scroll Persistence
    var newsScrollIndex = 0
    var newsScrollOffset = 0

    fun updateScrollPosition(index: Int, offset: Int) {
        newsScrollIndex = index
        newsScrollOffset = offset
    }

    fun resetScrollPosition() {
        newsScrollIndex = 0
        newsScrollOffset = 0
    }
}
