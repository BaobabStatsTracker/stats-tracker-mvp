package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Game
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.relation.GameWithTeams
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Games screen.
 * Manages the list of all recorded games.
 */
class GamesViewModel(
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                repository.getAllGamesWithTeamsFlow().collect { games ->
                    _uiState.value = _uiState.value.copy(
                        games = games,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load games: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadGames()
    }
}

/**
 * UI state for the Games screen
 */
data class GamesUiState(
    val games: List<GameWithTeams> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)