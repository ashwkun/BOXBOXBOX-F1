package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.DriverStanding
import com.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val repository: F1Repository
) : ViewModel() {

    private val _selectedYear = MutableStateFlow(java.time.Year.now().value.toString())
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    private val _driverStandings = MutableStateFlow<List<DriverStanding>?>(null)
    val driverStandings: StateFlow<List<DriverStanding>?> = _driverStandings.asStateFlow()

    private val _constructorStandings = MutableStateFlow<List<ConstructorStanding>?>(null)
    val constructorStandings: StateFlow<List<ConstructorStanding>?> = _constructorStandings.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadStandings(_selectedYear.value)
    }
    
    fun selectYear(year: String) {
        _selectedYear.value = year
        loadStandings(year)
    }

    fun resetToCurrentYear() {
        val currentYear = java.time.Year.now().value.toString()
        if (_selectedYear.value != currentYear) {
            _selectedYear.value = currentYear
            loadStandings(currentYear)
        }
    }
    
    fun loadStandings(season: String = _selectedYear.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Load both driver and constructor standings
            val driverResult = repository.getDriverStandings(season)
            val constructorResult = repository.getConstructorStandings(season)
            
            driverResult.onSuccess { standings ->
                _driverStandings.value = standings
            }.onFailure { e ->
                Log.e("StandingsViewModel", "Failed to load driver standings", e)
                _error.value = "Failed to load driver standings: ${e.message}"
            }
            
            constructorResult.onSuccess { standings ->
                _constructorStandings.value = standings
            }.onFailure { e ->
                Log.e("StandingsViewModel", "Failed to load constructor standings", e)
                _error.value = "Failed to load constructor standings: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }

    fun loadDriverStandings() {
        loadStandings(_selectedYear.value)
    }

    fun loadConstructorStandings() {
        loadStandings(_selectedYear.value)
    }
    
    fun clearError() {
        _error.value = null
    }
}
