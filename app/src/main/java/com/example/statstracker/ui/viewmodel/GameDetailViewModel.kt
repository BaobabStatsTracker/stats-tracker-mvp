package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.GameStats
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.PlayerGameStats
import com.example.statstracker.database.relation.GameWithTeamsAndEvents
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Game Detail screen.
 * Manages detailed information for a specific game including stats and events.
 */
class GameDetailViewModel(
    private val gameId: Long,
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    init {
        loadGameDetails()
    }

    private fun loadGameDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load game with teams and events
                val gameWithDetails = repository.getGameWithTeamsAndEvents(gameId)
                if (gameWithDetails == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Game not found"
                    )
                    return@launch
                }

                // Load team stats for the game
                val teamStats = repository.getAllTeamStatsForGame(gameId)
                
                // Load player stats for the game
                var playerStats = repository.getAllPlayerStatsForGame(gameId)
                
                // If no PlayerGameStats exist, calculate them from GameEvents
                if (playerStats.isEmpty() && gameWithDetails.events.isNotEmpty()) {
                    playerStats = calculatePlayerStatsFromEvents(gameWithDetails.events)
                }
                
                // Load players for both teams
                val homePlayers = repository.getPlayersForTeam(gameWithDetails.homeTeam.id)
                val awayPlayers = repository.getPlayersForTeam(gameWithDetails.awayTeam.id)

                // Debug logging
                println("GameDetail Debug: Game ${gameId}")
                println("- Events count: ${gameWithDetails.events.size}")
                println("- Home players count: ${homePlayers.size}")
                println("- Away players count: ${awayPlayers.size}")
                println("- Player stats count: ${playerStats.size}")
                println("- Events with player IDs: ${gameWithDetails.events.count { it.playerId != null }}")

                _uiState.value = _uiState.value.copy(
                    gameWithDetails = gameWithDetails,
                    teamStats = teamStats,
                    playerStats = playerStats,
                    homePlayers = homePlayers,
                    awayPlayers = awayPlayers,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load game details: ${e.message}"
                )
            }
        }
    }

    private fun calculatePlayerStatsFromEvents(events: List<GameEvent>): List<PlayerGameStats> {
        val playerStatsMap = mutableMapOf<Long, MutableMap<String, Int>>()
        
        events.forEach { event ->
            val playerId = event.playerId ?: return@forEach
            val stats = playerStatsMap.getOrPut(playerId) { 
                mutableMapOf(
                    "points" to 0,
                    "fieldGoalsMade" to 0,
                    "fieldGoalsAttempted" to 0,
                    "threePointersMade" to 0,
                    "threePointersAttempted" to 0,
                    "freeThrowsMade" to 0,
                    "freeThrowsAttempted" to 0,
                    "reboundsOffensive" to 0,
                    "reboundsDefensive" to 0,
                    "assists" to 0,
                    "steals" to 0,
                    "blocks" to 0,
                    "turnovers" to 0,
                    "foulsPersonal" to 0
                )
            }
            
            // Calculate stats based on event type
            when (event.eventType) {
                GameEventType.TWO_POINTER_MADE -> {
                    stats["fieldGoalsMade"] = stats["fieldGoalsMade"]!! + 1
                    stats["fieldGoalsAttempted"] = stats["fieldGoalsAttempted"]!! + 1
                    stats["points"] = stats["points"]!! + 2
                }
                GameEventType.TWO_POINTER_MISSED -> {
                    stats["fieldGoalsAttempted"] = stats["fieldGoalsAttempted"]!! + 1
                }
                GameEventType.THREE_POINTER_MADE -> {
                    stats["fieldGoalsMade"] = stats["fieldGoalsMade"]!! + 1
                    stats["fieldGoalsAttempted"] = stats["fieldGoalsAttempted"]!! + 1
                    stats["threePointersMade"] = stats["threePointersMade"]!! + 1
                    stats["threePointersAttempted"] = stats["threePointersAttempted"]!! + 1
                    stats["points"] = stats["points"]!! + 3
                }
                GameEventType.THREE_POINTER_MISSED -> {
                    stats["fieldGoalsAttempted"] = stats["fieldGoalsAttempted"]!! + 1
                    stats["threePointersAttempted"] = stats["threePointersAttempted"]!! + 1
                }
                GameEventType.FREE_THROW_MADE -> {
                    stats["freeThrowsMade"] = stats["freeThrowsMade"]!! + 1
                    stats["freeThrowsAttempted"] = stats["freeThrowsAttempted"]!! + 1
                    stats["points"] = stats["points"]!! + 1
                }
                GameEventType.FREE_THROW_MISSED -> {
                    stats["freeThrowsAttempted"] = stats["freeThrowsAttempted"]!! + 1
                }
                GameEventType.REBOUND -> {
                    // For now, count all rebounds as defensive - this could be improved
                    stats["reboundsDefensive"] = stats["reboundsDefensive"]!! + 1
                }
                GameEventType.ASSIST -> {
                    stats["assists"] = stats["assists"]!! + 1
                }
                GameEventType.STEAL -> {
                    stats["steals"] = stats["steals"]!! + 1
                }
                GameEventType.BLOCK -> {
                    stats["blocks"] = stats["blocks"]!! + 1
                }
                GameEventType.TURNOVER -> {
                    stats["turnovers"] = stats["turnovers"]!! + 1
                }
                GameEventType.FOUL -> {
                    stats["foulsPersonal"] = stats["foulsPersonal"]!! + 1
                }
                else -> {
                    // Handle other event types or ignore
                }
            }
        }
        
        // Convert to PlayerGameStats objects
        return playerStatsMap.map { (playerId, stats) ->
            // Find the team for this player from the events
            val teamId = events.find { it.playerId == playerId }?.let { event ->
                _uiState.value.gameWithDetails?.let { gameDetails ->
                    when (event.team) {
                        GameTeamSide.HOME -> gameDetails.homeTeam.id
                        GameTeamSide.AWAY -> gameDetails.awayTeam.id
                    }
                }
            } ?: 1L // fallback team ID
            
            PlayerGameStats(
                gameId = gameId,
                playerId = playerId,
                teamId = teamId,
                quarter = null, // full game stats
                points = stats["points"]!!,
                fieldGoalsMade = stats["fieldGoalsMade"]!!,
                fieldGoalsAttempted = stats["fieldGoalsAttempted"]!!,
                threePointersMade = stats["threePointersMade"]!!,
                threePointersAttempted = stats["threePointersAttempted"]!!,
                freeThrowsMade = stats["freeThrowsMade"]!!,
                freeThrowsAttempted = stats["freeThrowsAttempted"]!!,
                reboundsOffensive = stats["reboundsOffensive"]!!,
                reboundsDefensive = stats["reboundsDefensive"]!!,
                assists = stats["assists"]!!,
                steals = stats["steals"]!!,
                blocks = stats["blocks"]!!,
                turnovers = stats["turnovers"]!!,
                foulsPersonal = stats["foulsPersonal"]!!
            )
        }
    }

    fun refresh() {
        loadGameDetails()
    }
}

/**
 * UI state for the Game Detail screen
 */
data class GameDetailUiState(
    val gameWithDetails: GameWithTeamsAndEvents? = null,
    val teamStats: List<GameStats> = emptyList(),
    val playerStats: List<PlayerGameStats> = emptyList(),
    val homePlayers: List<Player> = emptyList(),
    val awayPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)