package com.f1tracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1tracker.data.local.F1DataProvider
import com.f1tracker.data.models.Constructor
import com.f1tracker.data.models.ConstructorStanding
import com.f1tracker.data.models.Driver
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
            
            var driverList: List<DriverStanding>? = null
            var constructorList: List<ConstructorStanding>? = null
            
            driverResult.onSuccess { standings ->
                driverList = standings
            }.onFailure { e ->
                Log.e("StandingsViewModel", "Failed to load driver standings", e)
            }
            
            constructorResult.onSuccess { standings ->
                constructorList = standings
            }.onFailure { e ->
                Log.e("StandingsViewModel", "Failed to load constructor standings", e)
            }
            
            // Pre-season: if current year returns empty, generate grid from JSON data
            val currentYear = java.time.Year.now().value.toString()
            if (season == currentYear && driverList.isNullOrEmpty() && F1DataProvider.isDataLoaded()) {
                Log.d("StandingsViewModel", "Generating pre-season driver standings from JSON data")
                driverList = generatePreSeasonDriverStandings()
            }
            if (season == currentYear && constructorList.isNullOrEmpty() && F1DataProvider.isDataLoaded()) {
                Log.d("StandingsViewModel", "Generating pre-season constructor standings from JSON data")
                constructorList = generatePreSeasonConstructorStandings()
            }
            
            _driverStandings.value = if (!driverList.isNullOrEmpty()) driverList else emptyList()
            _constructorStandings.value = if (!constructorList.isNullOrEmpty()) constructorList else emptyList()
            
            // Show error only when data is truly empty and not current season
            if (driverList.isNullOrEmpty() && constructorList.isNullOrEmpty() && season != currentYear) {
                _error.value = "No standings available for $season season"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Generate synthetic driver standings from local JSON data (pre-season)
     * All drivers shown with 0 points, ordered alphabetically by family name
     */
    private fun generatePreSeasonDriverStandings(): List<DriverStanding> {
        val allDrivers = F1DataProvider.getAllDrivers()
        return allDrivers.sortedBy { it.familyName }.mapIndexed { index, driverInfo ->
            val teamInfo = F1DataProvider.getTeamByApiId(driverInfo.team)
            DriverStanding(
                position = (index + 1).toString(),
                positionText = (index + 1).toString(),
                points = "0",
                wins = "0",
                driver = Driver(
                    driverId = driverInfo.id,
                    permanentNumber = driverInfo.permanentNumber,
                    code = driverInfo.code,
                    givenName = driverInfo.givenName,
                    familyName = driverInfo.familyName
                ),
                constructors = listOf(
                    Constructor(
                        constructorId = driverInfo.team,
                        name = teamInfo?.displayName ?: driverInfo.team,
                        nationality = ""
                    )
                )
            )
        }
    }
    
    /**
     * Generate synthetic constructor standings from local JSON data (pre-season)
     * All teams shown with 0 points, ordered alphabetically
     */
    private fun generatePreSeasonConstructorStandings(): List<ConstructorStanding> {
        val allTeams = F1DataProvider.getAllTeams()
        return allTeams.sortedBy { it.displayName }.mapIndexed { index, teamInfo ->
            ConstructorStanding(
                position = (index + 1).toString(),
                positionText = (index + 1).toString(),
                points = "0",
                wins = "0",
                constructor = Constructor(
                    constructorId = teamInfo.id,
                    name = teamInfo.displayName,
                    nationality = ""
                )
            )
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
