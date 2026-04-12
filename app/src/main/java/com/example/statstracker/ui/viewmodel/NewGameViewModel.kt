package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Game
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlayerWithJersey(
    val player: Player,
    val jerseyNumber: Int,
    val isSelected: Boolean
)

class NewGameViewModel(
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewGameUiState())
    val uiState: StateFlow<NewGameUiState> = _uiState.asStateFlow()

    private var homeSearchJob: Job? = null
    private var awaySearchJob: Job? = null

    fun updateHomeSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            homeSearchQuery = query,
            homeIsNewTeam = false,
            homeTeam = null,
            homeTeamHasPlayers = false,
            homePlayers = emptyList(),
            homeTrackingMode = TrackingMode.BY_TEAM,
            error = null
        )
        homeSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(homeSearchResults = emptyList())
            return
        }
        homeSearchJob = viewModelScope.launch {
            delay(250)
            try {
                val results = repository.searchTeamsByName("%$query%")
                    .filter { it.id != _uiState.value.awayTeam?.id }
                _uiState.value = _uiState.value.copy(homeSearchResults = results)
            } catch (_: Exception) {}
        }
    }

    fun updateAwaySearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            awaySearchQuery = query,
            awayIsNewTeam = false,
            awayTeam = null,
            awayTeamHasPlayers = false,
            awayPlayers = emptyList(),
            awayTrackingMode = TrackingMode.BY_TEAM,
            error = null
        )
        awaySearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(awaySearchResults = emptyList())
            return
        }
        awaySearchJob = viewModelScope.launch {
            delay(250)
            try {
                val results = repository.searchTeamsByName("%$query%")
                    .filter { it.id != _uiState.value.homeTeam?.id }
                _uiState.value = _uiState.value.copy(awaySearchResults = results)
            } catch (_: Exception) {}
        }
    }

    fun selectHomeTeam(team: Team) {
        _uiState.value = _uiState.value.copy(
            homeTeam = team,
            homeSearchQuery = team.name,
            homeSearchResults = emptyList(),
            homeIsNewTeam = false,
            error = null
        )
        loadTeamPlayers(team.id, isHome = true)
    }

    fun selectAwayTeam(team: Team) {
        _uiState.value = _uiState.value.copy(
            awayTeam = team,
            awaySearchQuery = team.name,
            awaySearchResults = emptyList(),
            awayIsNewTeam = false,
            error = null
        )
        loadTeamPlayers(team.id, isHome = false)
    }

    fun selectNewHomeTeam(name: String) {
        _uiState.value = _uiState.value.copy(
            homeTeam = null,
            homeSearchQuery = name,
            homeSearchResults = emptyList(),
            homeIsNewTeam = true,
            homeTeamHasPlayers = false,
            homePlayers = emptyList(),
            homeTrackingMode = TrackingMode.BY_TEAM,
            error = null
        )
    }

    fun selectNewAwayTeam(name: String) {
        _uiState.value = _uiState.value.copy(
            awayTeam = null,
            awaySearchQuery = name,
            awaySearchResults = emptyList(),
            awayIsNewTeam = true,
            awayTeamHasPlayers = false,
            awayPlayers = emptyList(),
            awayTrackingMode = TrackingMode.BY_TEAM,
            error = null
        )
    }

    fun clearHomeTeam() {
        _uiState.value = _uiState.value.copy(
            homeTeam = null,
            homeSearchQuery = "",
            homeSearchResults = emptyList(),
            homeIsNewTeam = false,
            homeTeamHasPlayers = false,
            homePlayers = emptyList(),
            homeTrackingMode = TrackingMode.BY_TEAM
        )
    }

    fun clearAwayTeam() {
        _uiState.value = _uiState.value.copy(
            awayTeam = null,
            awaySearchQuery = "",
            awaySearchResults = emptyList(),
            awayIsNewTeam = false,
            awayTeamHasPlayers = false,
            awayPlayers = emptyList(),
            awayTrackingMode = TrackingMode.BY_TEAM
        )
    }

    private fun loadTeamPlayers(teamId: Long, isHome: Boolean) {
        viewModelScope.launch {
            try {
                val players = repository.getPlayersForTeam(teamId)
                val teamPlayers = repository.getTeamPlayersForTeam(teamId)
                val jerseyMap = teamPlayers.associate { it.playerId to (it.jerseyNum ?: 0) }
                val playersWithJersey = players.map { player ->
                    PlayerWithJersey(
                        player = player,
                        jerseyNumber = jerseyMap[player.id] ?: 0,
                        isSelected = true
                    )
                }
                val hasPlayers = players.isNotEmpty()
                _uiState.value = if (isHome) {
                    _uiState.value.copy(
                        homePlayers = playersWithJersey,
                        homeTeamHasPlayers = hasPlayers,
                        homeTrackingMode = if (hasPlayers) TrackingMode.BY_PLAYER else TrackingMode.BY_TEAM
                    )
                } else {
                    _uiState.value.copy(
                        awayPlayers = playersWithJersey,
                        awayTeamHasPlayers = hasPlayers,
                        awayTrackingMode = if (hasPlayers) TrackingMode.BY_PLAYER else TrackingMode.BY_TEAM
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun setTrackingMode(side: GameTeamSide, mode: TrackingMode) {
        _uiState.value = if (side == GameTeamSide.HOME) {
            _uiState.value.copy(homeTrackingMode = mode)
        } else {
            _uiState.value.copy(awayTrackingMode = mode)
        }
    }

    fun togglePlayerSelection(side: GameTeamSide, playerId: Long) {
        val update: (List<PlayerWithJersey>) -> List<PlayerWithJersey> = { list ->
            list.map { if (it.player.id == playerId) it.copy(isSelected = !it.isSelected) else it }
        }
        _uiState.value = if (side == GameTeamSide.HOME) {
            _uiState.value.copy(homePlayers = update(_uiState.value.homePlayers))
        } else {
            _uiState.value.copy(awayPlayers = update(_uiState.value.awayPlayers))
        }
    }

    fun updatePlayerJersey(side: GameTeamSide, playerId: Long, jersey: Int) {
        val update: (List<PlayerWithJersey>) -> List<PlayerWithJersey> = { list ->
            list.map { if (it.player.id == playerId) it.copy(jerseyNumber = jersey) else it }
        }
        _uiState.value = if (side == GameTeamSide.HOME) {
            _uiState.value.copy(homePlayers = update(_uiState.value.homePlayers))
        } else {
            _uiState.value.copy(awayPlayers = update(_uiState.value.awayPlayers))
        }
    }

    fun updateGameDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(gameDate = date, error = null)
    }

    fun updateGamePlace(place: String) {
        _uiState.value = _uiState.value.copy(gamePlace = place.ifBlank { null }, error = null)
    }

    fun updateGameNotes(notes: String) {
        _uiState.value = _uiState.value.copy(gameNotes = notes.ifBlank { null }, error = null)
    }

    fun createGame(onGameCreated: (Long) -> Unit) {
        val s = _uiState.value

        // Validate
        val homeReady = s.homeTeam != null || s.homeIsNewTeam
        val awayReady = s.awayTeam != null || s.awayIsNewTeam
        if (!homeReady || !awayReady) {
            _uiState.value = s.copy(error = "Please select both home and away teams")
            return
        }
        if (s.homeTeam != null && s.awayTeam != null && s.homeTeam.id == s.awayTeam.id) {
            _uiState.value = s.copy(error = "Home and away teams must be different")
            return
        }
        if (s.homeTrackingMode == TrackingMode.BY_PLAYER && s.homePlayers.none { it.isSelected }) {
            _uiState.value = s.copy(error = "Select at least one player for the home team")
            return
        }
        if (s.awayTrackingMode == TrackingMode.BY_PLAYER && s.awayPlayers.none { it.isSelected }) {
            _uiState.value = s.copy(error = "Select at least one player for the away team")
            return
        }

        _uiState.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Create new teams if needed
                val homeTeamId = if (s.homeIsNewTeam) {
                    repository.insertTeam(Team(name = s.homeSearchQuery.trim()))
                } else {
                    s.homeTeam!!.id
                }
                val awayTeamId = if (s.awayIsNewTeam) {
                    repository.insertTeam(Team(name = s.awaySearchQuery.trim()))
                } else {
                    s.awayTeam!!.id
                }

                val homeSelectedIds = if (s.homeTrackingMode == TrackingMode.BY_PLAYER)
                    s.homePlayers.filter { it.isSelected }.joinToString(",") { it.player.id.toString() }.ifEmpty { null }
                else null
                val awaySelectedIds = if (s.awayTrackingMode == TrackingMode.BY_PLAYER)
                    s.awayPlayers.filter { it.isSelected }.joinToString(",") { it.player.id.toString() }.ifEmpty { null }
                else null

                val game = Game(
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    date = s.gameDate,
                    place = s.gamePlace,
                    notes = s.gameNotes,
                    homeTrackingMode = s.homeTrackingMode,
                    awayTrackingMode = s.awayTrackingMode,
                    homeSelectedPlayerIds = homeSelectedIds,
                    awaySelectedPlayerIds = awaySelectedIds
                )
                val gameId = repository.insertGame(game)
                _uiState.value = s.copy(isLoading = false)
                onGameCreated(gameId)
            } catch (e: Exception) {
                _uiState.value = s.copy(isLoading = false, error = "Failed to create game: ${e.message}")
            }
        }
    }
}

data class NewGameUiState(
    val homeSearchQuery: String = "",
    val awaySearchQuery: String = "",
    val homeSearchResults: List<Team> = emptyList(),
    val awaySearchResults: List<Team> = emptyList(),
    val homeTeam: Team? = null,
    val awayTeam: Team? = null,
    val homeIsNewTeam: Boolean = false,
    val awayIsNewTeam: Boolean = false,
    val homeTeamHasPlayers: Boolean = false,
    val awayTeamHasPlayers: Boolean = false,
    val homePlayers: List<PlayerWithJersey> = emptyList(),
    val awayPlayers: List<PlayerWithJersey> = emptyList(),
    val homeTrackingMode: TrackingMode = TrackingMode.BY_TEAM,
    val awayTrackingMode: TrackingMode = TrackingMode.BY_TEAM,
    val gameDate: LocalDate = LocalDate.now(),
    val gamePlace: String? = null,
    val gameNotes: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canCreateGame: Boolean
        get() {
            val homeReady = homeTeam != null || homeIsNewTeam
            val awayReady = awayTeam != null || awayIsNewTeam
            if (!homeReady || !awayReady || isLoading) return false
            if (homeTeam != null && awayTeam != null && homeTeam.id == awayTeam.id) return false
            if (homeTrackingMode == TrackingMode.BY_PLAYER && homePlayers.none { it.isSelected }) return false
            if (awayTrackingMode == TrackingMode.BY_PLAYER && awayPlayers.none { it.isSelected }) return false
            return true
        }
}