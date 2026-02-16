package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Game
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Game Dashboard screen.
 * Handles game timer, event logging, and real-time statistics.
 */
class GameDashboardViewModel(
    private val gameId: Long,
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDashboardUiState())
    val uiState: StateFlow<GameDashboardUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var gameStartTime: Long = 0L

    init {
        loadGameData()
    }

    private fun loadGameData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val game = repository.getGameById(gameId)
                if (game == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Game not found"
                    )
                    return@launch
                }

                val homeTeam = repository.getTeamById(game.homeTeamId)
                val awayTeam = repository.getTeamById(game.awayTeamId)
                
                val homePlayers = if (game.homeTrackingMode == TrackingMode.BY_PLAYER) {
                    repository.getPlayersForTeam(game.homeTeamId)
                } else emptyList()
                
                val awayPlayers = if (game.awayTrackingMode == TrackingMode.BY_PLAYER) {
                    repository.getPlayersForTeam(game.awayTeamId)
                } else emptyList()

                val gameEvents = repository.getEventsForGame(gameId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    game = game,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    homePlayers = homePlayers,
                    awayPlayers = awayPlayers,
                    gameEvents = gameEvents,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load game data: ${e.message}"
                )
            }
        }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        
        _uiState.value = _uiState.value.copy(isTimerRunning = true)
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning && (_uiState.value.currentQuarter <= 4 || _uiState.value.isInOvertime)) {
                delay(1000)
                val currentState = _uiState.value
                
                if (currentState.quarterTimeRemaining > 0) {
                    // Countdown the current quarter
                    _uiState.value = currentState.copy(
                        quarterTimeRemaining = currentState.quarterTimeRemaining - 1,
                        elapsedSeconds = currentState.elapsedSeconds + 1 // Keep total elapsed time
                    )
                } else if (currentState.currentQuarter < 4 && !currentState.isInOvertime) {
                    // Move to next quarter
                    _uiState.value = currentState.copy(
                        currentQuarter = currentState.currentQuarter + 1,
                        quarterTimeRemaining = 600L, // Reset to 10 minutes
                        isTimerRunning = false // Pause between quarters
                    )
                } else {
                    // End of regulation or overtime period
                    _uiState.value = currentState.copy(
                        isTimerRunning = false
                    )
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _uiState.value = _uiState.value.copy(
            elapsedSeconds = 0L,
            currentQuarter = 1,
            quarterTimeRemaining = 600L,
            isInOvertime = false,
            overtimeNumber = 0,
            showEndGameDialog = false
        )
    }

    fun logPlayerEvent(
        playerId: Long,
        teamSide: GameTeamSide,
        eventType: GameEventType,
        locationX: Double? = null,
        locationY: Double? = null,
        pointsValue: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val event = GameEvent(
                    gameId = gameId,
                    playerId = playerId,
                    team = teamSide,
                    timestamp = calculateGameTimestamp(),
                    eventType = eventType,
                    locationX = locationX,
                    locationY = locationY,
                    pointsValue = pointsValue
                )
                
                repository.insertGameEvent(event)
                
                // Refresh game events
                val updatedEvents = repository.getEventsForGame(gameId)
                _uiState.value = _uiState.value.copy(gameEvents = updatedEvents)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to log event: ${e.message}"
                )
            }
        }
    }

    fun logTeamEvent(
        teamSide: GameTeamSide,
        eventType: GameEventType,
        locationX: Double? = null,
        locationY: Double? = null,
        pointsValue: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val event = GameEvent(
                    gameId = gameId,
                    playerId = null, // Team event, no specific player
                    team = teamSide,
                    timestamp = calculateGameTimestamp(),
                    eventType = eventType,
                    locationX = locationX,
                    locationY = locationY,
                    pointsValue = pointsValue
                )
                
                repository.insertGameEvent(event)
                
                // Refresh game events
                val updatedEvents = repository.getEventsForGame(gameId)
                _uiState.value = _uiState.value.copy(gameEvents = updatedEvents)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to log event: ${e.message}"
                )
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val event = _uiState.value.gameEvents.find { it.id == eventId }
                if (event != null) {
                    repository.deleteGameEvent(event)
                    
                    // Refresh game events
                    val updatedEvents = repository.getEventsForGame(gameId)
                    _uiState.value = _uiState.value.copy(gameEvents = updatedEvents)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete event: ${e.message}"
                )
            }
        }
    }

    fun toggleEventLogging() {
        val currentTab = _uiState.value.selectedTab
        val newTab = when (currentTab) {
            GameDashboardTab.HOME_EVENTS -> GameDashboardTab.AWAY_EVENTS
            GameDashboardTab.AWAY_EVENTS -> GameDashboardTab.HOME_EVENTS
            GameDashboardTab.GAME_LOG -> GameDashboardTab.HOME_EVENTS
        }
        _uiState.value = _uiState.value.copy(selectedTab = newTab)
    }

    fun selectTab(tab: GameDashboardTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun endQuarter() {
        pauseTimer()
        // Always show confirmation dialog for ending any quarter
        _uiState.value = _uiState.value.copy(showEndQuarterDialog = true)
    }
    
    fun confirmEndQuarter() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(showEndQuarterDialog = false)
        
        if (currentState.currentQuarter < 4 && !currentState.isInOvertime) {
            // Move to next quarter
            _uiState.value = _uiState.value.copy(
                currentQuarter = currentState.currentQuarter + 1,
                quarterTimeRemaining = 600L
            )
        } else {
            // End of regulation or overtime - show end game dialog
            _uiState.value = _uiState.value.copy(showEndGameDialog = true)
        }
    }
    
    fun dismissEndQuarterDialog() {
        _uiState.value = _uiState.value.copy(showEndQuarterDialog = false)
    }

    fun goToOvertime() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isInOvertime = true,
            overtimeNumber = currentState.overtimeNumber + 1,
            quarterTimeRemaining = 300L, // 5 minutes for overtime
            showEndGameDialog = false
        )
    }

    fun endGame() {
        pauseTimer()
        _uiState.value = _uiState.value.copy(
            showEndGameDialog = false
        )
        // Additional game ending logic could go here
    }

    fun dismissEndGameDialog() {
        _uiState.value = _uiState.value.copy(showEndGameDialog = false)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(minutes, secs)
    }
    
    /**
     * Calculate the game timestamp for events.
     * Formula: (completed_quarters * 600) + (completed_overtimes * 300) + time_passed_in_current_period
     */
    private fun calculateGameTimestamp(): Int {
        val currentState = _uiState.value
        
        return if (currentState.isInOvertime) {
            // All 4 quarters (4 * 600) + completed overtimes + current overtime elapsed
            val regulationTime = 4 * 600
            val completedOvertimes = (currentState.overtimeNumber - 1) * 300
            val currentOvertimeElapsed = 300 - currentState.quarterTimeRemaining
            (regulationTime + completedOvertimes + currentOvertimeElapsed).toInt()
        } else {
            // Regular quarter calculation
            val completedQuarters = currentState.currentQuarter - 1
            val timePassedInCurrentQuarter = 600 - currentState.quarterTimeRemaining
            ((completedQuarters * 600) + timePassedInCurrentQuarter).toInt()
        }
    }
}

/**
 * UI state for the Game Dashboard screen
 */
data class GameDashboardUiState(
    val game: Game? = null,
    val homeTeam: Team? = null,
    val awayTeam: Team? = null,
    val homePlayers: List<Player> = emptyList(),
    val awayPlayers: List<Player> = emptyList(),
    val gameEvents: List<GameEvent> = emptyList(),
    val elapsedSeconds: Long = 0L, // Keep for compatibility, but quarter system is primary
    val currentQuarter: Int = 1,
    val quarterTimeRemaining: Long = 600L, // 10 minutes in seconds, counts down
    val isTimerRunning: Boolean = false,
    val selectedTab: GameDashboardTab = GameDashboardTab.HOME_EVENTS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEndGameDialog: Boolean = false,
    val showEndQuarterDialog: Boolean = false,
    val isInOvertime: Boolean = false,
    val overtimeNumber: Int = 0 // 0 = regulation, 1 = OT1, 2 = OT2, etc.
) {
    val homeScore: Int
        get() = gameEvents
            .filter { it.team == GameTeamSide.HOME && it.pointsValue != null }
            .sumOf { it.pointsValue ?: 0 }
    
    val awayScore: Int
        get() = gameEvents
            .filter { it.team == GameTeamSide.AWAY && it.pointsValue != null }
            .sumOf { it.pointsValue ?: 0 }
    
    val homeCanLogPlayerEvents: Boolean
        get() = game?.homeTrackingMode == TrackingMode.BY_PLAYER && homePlayers.isNotEmpty()
    
    val awayCanLogPlayerEvents: Boolean
        get() = game?.awayTrackingMode == TrackingMode.BY_PLAYER && awayPlayers.isNotEmpty()
}

/**
 * Tabs available in the Game Dashboard
 */
enum class GameDashboardTab {
    HOME_EVENTS,
    AWAY_EVENTS,
    GAME_LOG
}