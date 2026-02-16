package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Game
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.TrackingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the New Game creation screen.
 * Handles team selection and game creation logic.
 */
class NewGameViewModel(
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewGameUiState())
    val uiState: StateFlow<NewGameUiState> = _uiState.asStateFlow()

    private val _availableTeams = MutableStateFlow<List<Team>>(emptyList())
    val availableTeams: StateFlow<List<Team>> = _availableTeams.asStateFlow()

    init {
        loadAvailableTeams()
    }

    private fun loadAvailableTeams() {
        viewModelScope.launch {
            try {
                val teams = repository.getAllTeams()
                _availableTeams.value = teams
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load teams: ${e.message}"
                )
            }
        }
    }

    fun selectHomeTeam(team: Team) {
        _uiState.value = _uiState.value.copy(
            homeTeam = team,
            error = null
        )
        updateTrackingMode(team, true)
    }

    fun selectAwayTeam(team: Team) {
        _uiState.value = _uiState.value.copy(
            awayTeam = team,
            error = null
        )
        updateTrackingMode(team, false)
    }

    private fun updateTrackingMode(team: Team, isHome: Boolean) {
        viewModelScope.launch {
            try {
                // Check if the team has players
                val players = repository.getPlayersForTeam(team.id)
                val trackingMode = if (players.isNotEmpty()) {
                    TrackingMode.BY_PLAYER
                } else {
                    TrackingMode.BY_TEAM
                }
                
                _uiState.value = if (isHome) {
                    _uiState.value.copy(homeTrackingMode = trackingMode)
                } else {
                    _uiState.value.copy(awayTrackingMode = trackingMode)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load team players: ${e.message}"
                )
            }
        }
    }

    fun updateGameDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            gameDate = date,
            error = null
        )
    }

    fun updateGamePlace(place: String) {
        _uiState.value = _uiState.value.copy(
            gamePlace = place.ifBlank { null },
            error = null
        )
    }

    fun updateGameNotes(notes: String) {
        _uiState.value = _uiState.value.copy(
            gameNotes = notes.ifBlank { null },
            error = null
        )
    }

    fun createGame(onGameCreated: (Long) -> Unit) {
        val currentState = _uiState.value
        
        if (currentState.homeTeam == null || currentState.awayTeam == null) {
            _uiState.value = currentState.copy(
                error = "Please select both home and away teams"
            )
            return
        }

        if (currentState.homeTeam.id == currentState.awayTeam.id) {
            _uiState.value = currentState.copy(
                error = "Home and away teams must be different"
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val game = Game(
                    homeTeamId = currentState.homeTeam.id,
                    awayTeamId = currentState.awayTeam.id,
                    date = currentState.gameDate,
                    place = currentState.gamePlace,
                    notes = currentState.gameNotes,
                    homeTrackingMode = currentState.homeTrackingMode,
                    awayTrackingMode = currentState.awayTrackingMode
                )
                
                val gameId = repository.insertGame(game)
                _uiState.value = currentState.copy(isLoading = false)
                onGameCreated(gameId)
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Failed to create game: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the New Game screen
 */
data class NewGameUiState(
    val homeTeam: Team? = null,
    val awayTeam: Team? = null,
    val gameDate: LocalDate = LocalDate.now(),
    val gamePlace: String? = null,
    val gameNotes: String? = null,
    val homeTrackingMode: TrackingMode = TrackingMode.BY_TEAM,
    val awayTrackingMode: TrackingMode = TrackingMode.BY_TEAM,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canCreateGame: Boolean
        get() = homeTeam != null && awayTeam != null && homeTeam.id != awayTeam.id && !isLoading
}