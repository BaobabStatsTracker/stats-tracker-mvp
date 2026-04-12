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

class GameDashboardViewModel(
    private val gameId: Long,
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDashboardUiState())
    val uiState: StateFlow<GameDashboardUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadGameData()
    }

    private fun loadGameData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val game = repository.getGameById(gameId)
                if (game == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Game not found")
                    return@launch
                }

                val homeTeam = repository.getTeamById(game.homeTeamId)
                val awayTeam = repository.getTeamById(game.awayTeamId)

                val homePlayers = if (game.homeTrackingMode == TrackingMode.BY_PLAYER)
                    repository.getPlayersForTeam(game.homeTeamId) else emptyList()
                val awayPlayers = if (game.awayTrackingMode == TrackingMode.BY_PLAYER)
                    repository.getPlayersForTeam(game.awayTeamId) else emptyList()

                // Load jersey numbers
                val homeTeamPlayers = if (game.homeTrackingMode == TrackingMode.BY_PLAYER)
                    repository.getTeamPlayersForTeam(game.homeTeamId) else emptyList()
                val awayTeamPlayers = if (game.awayTrackingMode == TrackingMode.BY_PLAYER)
                    repository.getTeamPlayersForTeam(game.awayTeamId) else emptyList()

                val homeJerseys = homeTeamPlayers.associate { it.playerId to (it.jerseyNum ?: 0) }
                val awayJerseys = awayTeamPlayers.associate { it.playerId to (it.jerseyNum ?: 0) }

                // Split players into on-court (first 5) and bench (rest)
                val homeOnCourt = homePlayers.take(5).map { it.id }
                val homeBench = homePlayers.drop(5).map { it.id }
                val awayOnCourt = awayPlayers.take(5).map { it.id }
                val awayBench = awayPlayers.drop(5).map { it.id }

                val gameEvents = repository.getEventsForGame(gameId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    game = game,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    homePlayers = homePlayers,
                    awayPlayers = awayPlayers,
                    gameEvents = gameEvents,
                    homePlayerJerseys = homeJerseys,
                    awayPlayerJerseys = awayJerseys,
                    homeOnCourt = homeOnCourt,
                    homeBench = homeBench,
                    awayOnCourt = awayOnCourt,
                    awayBench = awayBench,
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

    // --- Timer ---

    fun startTimer() {
        if (timerJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isTimerRunning = true)
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning) {
                delay(1000)
                val s = _uiState.value
                if (s.quarterTimeRemaining > 0) {
                    _uiState.value = s.copy(
                        quarterTimeRemaining = s.quarterTimeRemaining - 1,
                        elapsedSeconds = s.elapsedSeconds + 1
                    )
                } else {
                    _uiState.value = s.copy(isTimerRunning = false)
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
        _uiState.value = _uiState.value.copy(quarterTimeRemaining = 600L)
    }

    // --- Quarter / Overtime ---

    fun endQuarter() {
        pauseTimer()
        _uiState.value = _uiState.value.copy(showEndQuarterDialog = true)
    }

    fun confirmEndQuarter() {
        val s = _uiState.value
        _uiState.value = s.copy(showEndQuarterDialog = false)
        if (s.currentQuarter < 4 && !s.isInOvertime) {
            _uiState.value = _uiState.value.copy(
                currentQuarter = s.currentQuarter + 1,
                quarterTimeRemaining = 600L
            )
        } else {
            _uiState.value = _uiState.value.copy(showEndGameDialog = true)
        }
    }

    fun dismissEndQuarterDialog() {
        _uiState.value = _uiState.value.copy(showEndQuarterDialog = false)
    }

    fun goToOvertime() {
        val s = _uiState.value
        _uiState.value = s.copy(
            isInOvertime = true,
            overtimeNumber = s.overtimeNumber + 1,
            quarterTimeRemaining = 300L,
            showEndGameDialog = false
        )
    }

    fun endGame() {
        pauseTimer()
        _uiState.value = _uiState.value.copy(showEndGameDialog = false)
    }

    fun dismissEndGameDialog() {
        _uiState.value = _uiState.value.copy(showEndGameDialog = false)
    }

    // --- Event Modal ---

    fun openPlayerEventModal(playerId: Long, side: GameTeamSide) {
        _uiState.value = _uiState.value.copy(
            selectedPlayerForEvent = Pair(playerId, side),
            selectedTeamForEvent = null,
            showEventModal = true
        )
    }

    fun openTeamEventModal(side: GameTeamSide) {
        _uiState.value = _uiState.value.copy(
            selectedPlayerForEvent = null,
            selectedTeamForEvent = side,
            showEventModal = true
        )
    }

    fun dismissEventModal() {
        _uiState.value = _uiState.value.copy(
            showEventModal = false,
            selectedPlayerForEvent = null,
            selectedTeamForEvent = null
        )
    }

    fun logEventFromModal(eventType: GameEventType, pointsValue: Int?) {
        val s = _uiState.value
        val side = s.selectedPlayerForEvent?.second ?: s.selectedTeamForEvent ?: return
        val playerId = s.selectedPlayerForEvent?.first

        viewModelScope.launch {
            try {
                val event = GameEvent(
                    gameId = gameId,
                    playerId = playerId,
                    team = side,
                    timestamp = calculateGameTimestamp(),
                    eventType = eventType,
                    pointsValue = pointsValue
                )
                repository.insertGameEvent(event)
                val updatedEvents = repository.getEventsForGame(gameId)
                _uiState.value = _uiState.value.copy(
                    gameEvents = updatedEvents,
                    showEventModal = false,
                    selectedPlayerForEvent = null,
                    selectedTeamForEvent = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to log event: ${e.message}")
            }
        }
    }

    // --- Player Swapping ---

    fun swapPlayers(side: GameTeamSide, playerId1: Long, playerId2: Long) {
        val s = _uiState.value
        val onCourt = if (side == GameTeamSide.HOME) s.homeOnCourt.toMutableList() else s.awayOnCourt.toMutableList()
        val bench = if (side == GameTeamSide.HOME) s.homeBench.toMutableList() else s.awayBench.toMutableList()

        val idx1Court = onCourt.indexOf(playerId1)
        val idx1Bench = bench.indexOf(playerId1)
        val idx2Court = onCourt.indexOf(playerId2)
        val idx2Bench = bench.indexOf(playerId2)

        // Both on court — swap positions
        if (idx1Court >= 0 && idx2Court >= 0) {
            onCourt[idx1Court] = playerId2
            onCourt[idx2Court] = playerId1
        }
        // Both on bench — swap positions
        else if (idx1Bench >= 0 && idx2Bench >= 0) {
            bench[idx1Bench] = playerId2
            bench[idx2Bench] = playerId1
        }
        // One on court, one on bench — substitute
        else if (idx1Court >= 0 && idx2Bench >= 0) {
            onCourt[idx1Court] = playerId2
            bench[idx2Bench] = playerId1
        } else if (idx1Bench >= 0 && idx2Court >= 0) {
            onCourt[idx2Court] = playerId1
            bench[idx1Bench] = playerId2
        }

        _uiState.value = if (side == GameTeamSide.HOME) {
            s.copy(homeOnCourt = onCourt, homeBench = bench)
        } else {
            s.copy(awayOnCourt = onCourt, awayBench = bench)
        }
    }

    // --- Delete Event ---

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val event = _uiState.value.gameEvents.find { it.id == eventId }
                if (event != null) {
                    repository.deleteGameEvent(event)
                    val updatedEvents = repository.getEventsForGame(gameId)
                    _uiState.value = _uiState.value.copy(gameEvents = updatedEvents)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete event: ${e.message}")
            }
        }
    }

    // --- Utilities ---

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(minutes, secs)
    }

    private fun calculateGameTimestamp(): Int {
        val s = _uiState.value
        return if (s.isInOvertime) {
            val regulationTime = 4 * 600
            val completedOvertimes = (s.overtimeNumber - 1) * 300
            val currentOvertimeElapsed = 300 - s.quarterTimeRemaining
            (regulationTime + completedOvertimes + currentOvertimeElapsed).toInt()
        } else {
            val completedQuarters = s.currentQuarter - 1
            val timePassedInCurrentQuarter = 600 - s.quarterTimeRemaining
            ((completedQuarters * 600) + timePassedInCurrentQuarter).toInt()
        }
    }
}

data class GameDashboardUiState(
    val game: Game? = null,
    val homeTeam: Team? = null,
    val awayTeam: Team? = null,
    val homePlayers: List<Player> = emptyList(),
    val awayPlayers: List<Player> = emptyList(),
    val gameEvents: List<GameEvent> = emptyList(),
    val elapsedSeconds: Long = 0L,
    val currentQuarter: Int = 1,
    val quarterTimeRemaining: Long = 600L,
    val isTimerRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEndGameDialog: Boolean = false,
    val showEndQuarterDialog: Boolean = false,
    val isInOvertime: Boolean = false,
    val overtimeNumber: Int = 0,
    // On-court / bench player tracking
    val homeOnCourt: List<Long> = emptyList(),
    val homeBench: List<Long> = emptyList(),
    val awayOnCourt: List<Long> = emptyList(),
    val awayBench: List<Long> = emptyList(),
    // Jersey numbers
    val homePlayerJerseys: Map<Long, Int> = emptyMap(),
    val awayPlayerJerseys: Map<Long, Int> = emptyMap(),
    // Event modal
    val showEventModal: Boolean = false,
    val selectedPlayerForEvent: Pair<Long, GameTeamSide>? = null,
    val selectedTeamForEvent: GameTeamSide? = null
) {
    val homeScore: Int
        get() = gameEvents
            .filter { it.team == GameTeamSide.HOME && it.pointsValue != null }
            .sumOf { it.pointsValue ?: 0 }

    val awayScore: Int
        get() = gameEvents
            .filter { it.team == GameTeamSide.AWAY && it.pointsValue != null }
            .sumOf { it.pointsValue ?: 0 }
}