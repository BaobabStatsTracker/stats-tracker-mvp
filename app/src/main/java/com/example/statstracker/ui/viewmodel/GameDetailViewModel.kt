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
                
                // Load players for both teams — filter to only selected players when tracked by player
                val homeSelectedIds = gameWithDetails.game.homeSelectedPlayerIds
                    ?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet()
                val awaySelectedIds = gameWithDetails.game.awaySelectedPlayerIds
                    ?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet()

                val allHomePlayers = repository.getPlayersForTeam(gameWithDetails.homeTeam.id)
                val allAwayPlayers = repository.getPlayersForTeam(gameWithDetails.awayTeam.id)

                val homePlayers = if (!homeSelectedIds.isNullOrEmpty())
                    allHomePlayers.filter { it.id in homeSelectedIds } else allHomePlayers
                val awayPlayers = if (!awaySelectedIds.isNullOrEmpty())
                    allAwayPlayers.filter { it.id in awaySelectedIds } else allAwayPlayers

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

        // --- Time-played reconstruction from SUBSTITUTION events ---
        // Each stint is a (entryTime, exitTime?) pair.
        // playerId = who entered, assistPlayerId = who left (null for initial lineup).
        val playerStints = mutableMapOf<Long, MutableList<Pair<Int, Int?>>>()
        val maxTimestamp = events.maxOfOrNull { it.timestamp } ?: 0
        
        events.filter { it.eventType == GameEventType.SUBSTITUTION }
            .sortedBy { it.timestamp }
            .forEach { event ->
                val enteredId = event.playerId ?: return@forEach
                val leftId = event.assistPlayerId

                // Player entering the court: start a new open stint
                val entryStints = playerStints.getOrPut(enteredId) { mutableListOf() }
                entryStints.add(Pair(event.timestamp, null))

                // Player leaving the court: close their most recent open stint
                if (leftId != null) {
                    val exitStints = playerStints.getOrPut(leftId) { mutableListOf() }
                    val openIdx = exitStints.indexOfLast { it.second == null }
                    if (openIdx >= 0) {
                        exitStints[openIdx] = exitStints[openIdx].copy(second = event.timestamp)
                    }
                }
            }

        // Close any still-open stints using the latest event timestamp as the end
        for ((_, stints) in playerStints) {
            for (i in stints.indices) {
                if (stints[i].second == null) {
                    stints[i] = stints[i].copy(second = maxTimestamp)
                }
            }
        }

        // Compute total seconds per player from stints
        val playerTimeSeconds = playerStints.mapValues { (_, stints) ->
            stints.sumOf { (entry, exit) -> (exit ?: entry) - entry }
        }
        
        events.forEach { event ->
            val playerId = event.playerId ?: return@forEach
            // Skip SUBSTITUTION events for counting stats
            if (event.eventType == GameEventType.SUBSTITUTION) return@forEach

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
        // Merge stat-event players and substitution-only players (who may have
        // court time but no other stat events)
        val allPlayerIds = (playerStatsMap.keys + playerTimeSeconds.keys).toSet()

        return allPlayerIds.map { playerId ->
            val stats = playerStatsMap[playerId]
            // Find the team for this player from any event that references them
            val teamId = events.find { it.playerId == playerId || it.assistPlayerId == playerId }?.let { event ->
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
                points = stats?.get("points") ?: 0,
                fieldGoalsMade = stats?.get("fieldGoalsMade") ?: 0,
                fieldGoalsAttempted = stats?.get("fieldGoalsAttempted") ?: 0,
                threePointersMade = stats?.get("threePointersMade") ?: 0,
                threePointersAttempted = stats?.get("threePointersAttempted") ?: 0,
                freeThrowsMade = stats?.get("freeThrowsMade") ?: 0,
                freeThrowsAttempted = stats?.get("freeThrowsAttempted") ?: 0,
                reboundsOffensive = stats?.get("reboundsOffensive") ?: 0,
                reboundsDefensive = stats?.get("reboundsDefensive") ?: 0,
                assists = stats?.get("assists") ?: 0,
                steals = stats?.get("steals") ?: 0,
                blocks = stats?.get("blocks") ?: 0,
                turnovers = stats?.get("turnovers") ?: 0,
                foulsPersonal = stats?.get("foulsPersonal") ?: 0,
                timePlayedSeconds = playerTimeSeconds[playerId] ?: 0
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