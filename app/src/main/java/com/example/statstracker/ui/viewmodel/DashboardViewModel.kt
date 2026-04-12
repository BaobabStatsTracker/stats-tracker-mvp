package com.example.statstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.PlayerSeasonStats
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.relation.GameWithTeams
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TopPerformerData(
    val player: Player,
    val stats: PlayerSeasonStats,
    val teamName: String
)

data class RecentResultData(
    val gameWithTeams: GameWithTeams,
    val ourTeamIsHome: Boolean,
    val ourScore: Int,
    val opponentScore: Int,
    val won: Boolean
)

data class TeamAveragesData(
    val team: Team,
    val playerStats: List<PlayerSeasonStats>
) {
    val gamesPlayed: Int get() = playerStats.maxOfOrNull { it.gamesPlayed } ?: 0
    val pointsPerGame: Double get() =
        if (playerStats.isEmpty()) 0.0
        else {
            val games = gamesPlayed
            if (games == 0) 0.0 else playerStats.sumOf { it.pointsTotal }.toDouble() / games
        }
    val reboundsPerGame: Double get() =
        if (playerStats.isEmpty()) 0.0
        else {
            val games = gamesPlayed
            if (games == 0) 0.0 else playerStats.sumOf { it.totalRebounds }.toDouble() / games
        }
    val assistsPerGame: Double get() =
        if (playerStats.isEmpty()) 0.0
        else {
            val games = gamesPlayed
            if (games == 0) 0.0 else playerStats.sumOf { it.assistsTotal }.toDouble() / games
        }
    val fieldGoalPercentage: Double get() {
        val attempted = playerStats.sumOf { it.fieldGoalsAttempted }
        val made = playerStats.sumOf { it.fieldGoalsMade }
        return if (attempted > 0) made.toDouble() / attempted else 0.0
    }
    val threePointPercentage: Double get() {
        val attempted = playerStats.sumOf { it.threePointersAttempted }
        val made = playerStats.sumOf { it.threePointersMade }
        return if (attempted > 0) made.toDouble() / attempted else 0.0
    }
    val freeThrowPercentage: Double get() {
        val attempted = playerStats.sumOf { it.freeThrowsAttempted }
        val made = playerStats.sumOf { it.freeThrowsMade }
        return if (attempted > 0) made.toDouble() / attempted else 0.0
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val ourTeams: List<Team> = emptyList(),
    val topPerformer: TopPerformerData? = null,
    val recentResults: List<RecentResultData> = emptyList(),
    val nextGame: GameWithTeams? = null,
    val teamAverages: List<TeamAveragesData> = emptyList()
) {
    val hasOurTeams: Boolean get() = ourTeams.isNotEmpty()
}

class DashboardViewModel(
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                repository.getOurTeamsFlow(),
                repository.getAllGamesWithTeamsFlow()
            ) { ourTeams, allGames ->
                Pair(ourTeams, allGames)
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .collect { (ourTeams, allGames) ->
                val ourTeamIds = ourTeams.map { it.id }.toSet()
                val today = LocalDate.now()
                val currentYear = today.year

                // Games involving any of our teams
                val ourGames = allGames.filter { gwt ->
                    gwt.game.homeTeamId in ourTeamIds || gwt.game.awayTeamId in ourTeamIds
                }

                // Recent results: past games sorted newest first, take 5
                val pastGames = ourGames
                    .filter { it.game.date < today }
                    .sortedByDescending { it.game.date }
                    .take(5)

                // Next scheduled game: first upcoming
                val nextGame = ourGames
                    .filter { it.game.date >= today }
                    .minByOrNull { it.game.date }

                // Build recent result items (require GameStats to determine score)
                val recentResults = buildRecentResults(pastGames, ourTeamIds)

                // Team averages from season stats
                val teamAverages = buildTeamAverages(ourTeams, currentYear)

                // Top performer: highest PPG among players on our teams this season
                val topPerformer = findTopPerformer(ourTeams, currentYear)

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    ourTeams = ourTeams,
                    topPerformer = topPerformer,
                    recentResults = recentResults,
                    nextGame = nextGame,
                    teamAverages = teamAverages
                )
            }
        }
    }

    private suspend fun buildRecentResults(
        games: List<GameWithTeams>,
        ourTeamIds: Set<Long>
    ): List<RecentResultData> {
        return games.mapNotNull { gwt ->
            val homeId = gwt.game.homeTeamId
            val awayId = gwt.game.awayTeamId
            val ourTeamIsHome = homeId in ourTeamIds

            val homeStats = repository.getTeamGameStats(gwt.game.id, homeId)
            val awayStats = repository.getTeamGameStats(gwt.game.id, awayId)

            val homeScore = homeStats?.points ?: 0
            val awayScore = awayStats?.points ?: 0

            // Only include if at least one side has any stats recorded
            if (homeScore == 0 && awayScore == 0) return@mapNotNull null

            val ourScore = if (ourTeamIsHome) homeScore else awayScore
            val opponentScore = if (ourTeamIsHome) awayScore else homeScore

            RecentResultData(
                gameWithTeams = gwt,
                ourTeamIsHome = ourTeamIsHome,
                ourScore = ourScore,
                opponentScore = opponentScore,
                won = ourScore > opponentScore
            )
        }
    }

    private suspend fun buildTeamAverages(
        ourTeams: List<Team>,
        seasonYear: Int
    ): List<TeamAveragesData> {
        return ourTeams.mapNotNull { team ->
            val stats = repository.getTeamSeasonStats(team.id, seasonYear)
            if (stats.isEmpty()) null
            else TeamAveragesData(team = team, playerStats = stats)
        }
    }

    private suspend fun findTopPerformer(
        ourTeams: List<Team>,
        seasonYear: Int
    ): TopPerformerData? {
        var best: Triple<Player, PlayerSeasonStats, String>? = null

        for (team in ourTeams) {
            val seasonStats = repository.getTeamSeasonStats(team.id, seasonYear)
            for (stat in seasonStats) {
                if (stat.gamesPlayed < 1) continue
                val player = repository.getPlayerById(stat.playerId) ?: continue
                val currentBest = best
                if (currentBest == null || stat.pointsPerGame > currentBest.second.pointsPerGame) {
                    best = Triple(player, stat, team.name)
                }
            }
        }

        return best?.let { (player, stats, teamName) ->
            TopPerformerData(player = player, stats = stats, teamName = teamName)
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        observeDashboard()
    }
}
