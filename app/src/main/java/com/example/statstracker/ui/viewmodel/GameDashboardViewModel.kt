package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Game
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.PlayerGameStats
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

                // Build initial court-entry-time map: all starters entered at t=0
                val entryTimes = mutableMapOf<Long, Int>()
                (homeOnCourt + awayOnCourt).forEach { entryTimes[it] = 0 }

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
                    playerCourtEntryTimes = entryTimes,
                    playerAccumulatedSeconds = emptyMap(),
                    error = null
                )

                // Log initial-lineup SUBSTITUTION events (playerIn only, no playerOut)
                // so GameDetailViewModel can reconstruct time from events.
                // Guard: only if no SUBSTITUTION events already exist for this game.
                val hasExistingSubs = gameEvents.any { it.eventType == GameEventType.SUBSTITUTION }
                if (!hasExistingSubs) {
                    val allStarters = homeOnCourt.map { it to GameTeamSide.HOME } +
                            awayOnCourt.map { it to GameTeamSide.AWAY }
                    for ((pid, side) in allStarters) {
                        repository.insertGameEvent(
                            GameEvent(
                                gameId = gameId,
                                playerId = pid,
                                team = side,
                                timestamp = 0,
                                eventType = GameEventType.SUBSTITUTION,
                                assistPlayerId = null // no player out — initial lineup
                            )
                        )
                    }
                    // Refresh events list to include the new sub events
                    val refreshed = repository.getEventsForGame(gameId)
                    _uiState.value = _uiState.value.copy(gameEvents = refreshed)
                }
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
        // Persist final PlayerGameStats (including time played) to DB
        viewModelScope.launch {
            persistPlayerGameStats()
        }
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

    // --- Player Swapping & Substitution Tracking ---

    fun swapPlayers(side: GameTeamSide, playerId1: Long, playerId2: Long) {
        val s = _uiState.value
        val onCourt = if (side == GameTeamSide.HOME) s.homeOnCourt.toMutableList() else s.awayOnCourt.toMutableList()
        val bench = if (side == GameTeamSide.HOME) s.homeBench.toMutableList() else s.awayBench.toMutableList()

        val idx1Court = onCourt.indexOf(playerId1)
        val idx1Bench = bench.indexOf(playerId1)
        val idx2Court = onCourt.indexOf(playerId2)
        val idx2Bench = bench.indexOf(playerId2)

        // Identify playerIn / playerOut for the substitution cases
        var playerIn: Long? = null
        var playerOut: Long? = null

        // Both on court — swap positions (not a real substitution)
        if (idx1Court >= 0 && idx2Court >= 0) {
            onCourt[idx1Court] = playerId2
            onCourt[idx2Court] = playerId1
        }
        // Both on bench — swap positions (not a real substitution)
        else if (idx1Bench >= 0 && idx2Bench >= 0) {
            bench[idx1Bench] = playerId2
            bench[idx2Bench] = playerId1
        }
        // Court → bench substitution: player1 leaves court, player2 enters
        else if (idx1Court >= 0 && idx2Bench >= 0) {
            onCourt[idx1Court] = playerId2
            bench[idx2Bench] = playerId1
            playerOut = playerId1
            playerIn = playerId2
        }
        // Bench → court substitution: player1 enters court, player2 leaves
        else if (idx1Bench >= 0 && idx2Court >= 0) {
            onCourt[idx2Court] = playerId1
            bench[idx1Bench] = playerId2
            playerOut = playerId2
            playerIn = playerId1
        }

        // Update court/bench lists
        val updatedEntryTimes = s.playerCourtEntryTimes.toMutableMap()
        val updatedAccumulated = s.playerAccumulatedSeconds.toMutableMap()

        if (playerIn != null && playerOut != null) {
            // Accumulate the completed stint for the player leaving the court
            val entryTime = updatedEntryTimes[playerOut] ?: 0
            val stint = (s.elapsedSeconds - entryTime).toInt().coerceAtLeast(0)
            updatedAccumulated[playerOut] = (updatedAccumulated[playerOut] ?: 0) + stint
            updatedEntryTimes.remove(playerOut)

            // Record court entry time for the player entering
            updatedEntryTimes[playerIn] = s.elapsedSeconds.toInt()

            // Log SUBSTITUTION event to DB asynchronously
            val timestamp = calculateGameTimestamp()
            viewModelScope.launch {
                try {
                    repository.insertGameEvent(
                        GameEvent(
                            gameId = gameId,
                            playerId = playerIn,
                            team = side,
                            timestamp = timestamp,
                            eventType = GameEventType.SUBSTITUTION,
                            assistPlayerId = playerOut  // encodes "who left the court"
                        )
                    )
                    val refreshed = repository.getEventsForGame(gameId)
                    _uiState.value = _uiState.value.copy(gameEvents = refreshed)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to log substitution: ${e.message}"
                    )
                }
            }
        }

        _uiState.value = if (side == GameTeamSide.HOME) {
            s.copy(
                homeOnCourt = onCourt, homeBench = bench,
                playerCourtEntryTimes = updatedEntryTimes,
                playerAccumulatedSeconds = updatedAccumulated
            )
        } else {
            s.copy(
                awayOnCourt = onCourt, awayBench = bench,
                playerCourtEntryTimes = updatedEntryTimes,
                playerAccumulatedSeconds = updatedAccumulated
            )
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

    // --- Time Played Persistence ---

    /**
     * Builds and upserts [PlayerGameStats] for every player who spent time on court.
     * Called on game end to write final `timePlayedSeconds` to the database.
     *
     * For players still on court when the game ends, their active stint is
     * calculated as (current elapsed seconds − entry time) and added to
     * their accumulated total.
     */
    private suspend fun persistPlayerGameStats() {
        val s = _uiState.value
        val game = s.game ?: return

        // Merge accumulated seconds with any active (still-on-court) stint
        val finalSeconds = s.playerAccumulatedSeconds.toMutableMap()
        for ((playerId, entryTime) in s.playerCourtEntryTimes) {
            val activeStint = (s.elapsedSeconds - entryTime).toInt().coerceAtLeast(0)
            finalSeconds[playerId] = (finalSeconds[playerId] ?: 0) + activeStint
        }

        // Determine which team each player belongs to
        val homePlayerIds = s.homePlayers.map { it.id }.toSet()

        for ((playerId, seconds) in finalSeconds) {
            if (seconds <= 0) continue
            val teamId = if (playerId in homePlayerIds) game.homeTeamId else game.awayTeamId

            // Load existing stats (from event logging) or create a fresh row
            val existing = repository.getPlayerGameStats(gameId, playerId)
            val stats = (existing ?: PlayerGameStats(
                gameId = gameId,
                playerId = playerId,
                teamId = teamId,
                quarter = null
            )).copy(timePlayedSeconds = seconds)

            repository.upsertPlayerGameStats(stats)
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
    val selectedTeamForEvent: GameTeamSide? = null,
    // Per-player time tracking (in-memory during live game)
    // Maps playerId → elapsed-seconds value when they entered the court
    val playerCourtEntryTimes: Map<Long, Int> = emptyMap(),
    // Maps playerId → total accumulated seconds from completed (subbed-out) stints
    val playerAccumulatedSeconds: Map<Long, Int> = emptyMap()
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