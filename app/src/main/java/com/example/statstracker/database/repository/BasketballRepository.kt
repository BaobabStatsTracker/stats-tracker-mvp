package com.example.statstracker.database.repository

import com.example.statstracker.database.BasketballDatabase
import com.example.statstracker.database.entity.*
import com.example.statstracker.database.relation.*
import com.example.statstracker.model.PlayerRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for basketball data operations.
 * Provides a clean API for accessing basketball statistics data.
 * 
 * Usage example in your ViewModel or UseCase:
 * ```
 * class BasketballViewModel constructor(
 *     private val repository: BasketballRepository
 * ) : ViewModel() {
 *     
 *     val allTeams = repository.getAllTeamsWithPlayers().asLiveData()
 *     
 *     fun addNewPlayer(firstName: String, lastName: String) {
 *         viewModelScope.launch {
 *             repository.insertPlayer(Player(firstName = firstName, lastName = lastName))
 *         }
 *     }
 * }
 * ```
 */
class BasketballRepository constructor(
    private val database: BasketballDatabase
) {
    
    // --- Player Operations ---
    
    suspend fun insertPlayer(player: Player): Long = database.playerDao().insert(player)
    suspend fun updatePlayer(player: Player) = database.playerDao().update(player)
    suspend fun deletePlayer(player: Player) = database.playerDao().delete(player)
    suspend fun getAllPlayers(): List<Player> = database.playerDao().getAllPlayers()
    fun getAllPlayersFlow(): Flow<List<Player>> = database.playerDao().getAllPlayersFlow()
    suspend fun getPlayerById(playerId: Long): Player? = database.playerDao().getPlayerById(playerId)
    suspend fun searchPlayersByName(query: String): List<Player> = 
        database.playerDao().searchPlayersByName(query)
    
    // --- Team Operations ---
    
    suspend fun insertTeam(team: Team): Long = database.teamDao().insert(team)
    suspend fun updateTeam(team: Team) = database.teamDao().update(team)
    suspend fun deleteTeam(team: Team) = database.teamDao().delete(team)
    suspend fun getAllTeams(): List<Team> = database.teamDao().getAllTeams()
    fun getAllTeamsFlow(): Flow<List<Team>> = database.teamDao().getAllTeamsFlow()
    suspend fun getTeamById(teamId: Long): Team? = database.teamDao().getTeamById(teamId)
    
    // --- Team-Player Relationships ---
    
    suspend fun addPlayerToTeam(
        playerId: Long, 
        teamId: Long, 
        jerseyNum: Int? = null, 
        role: PlayerRole? = null
    ): Long {
        val teamPlayer = TeamPlayer(
            playerId = playerId,
            teamId = teamId,
            jerseyNum = jerseyNum,
            role = role
        )
        return database.teamPlayerDao().insert(teamPlayer)
    }
    
    suspend fun removePlayerFromTeam(playerId: Long, teamId: Long) = 
        database.teamPlayerDao().unlinkPlayerFromTeam(playerId, teamId)
    
    suspend fun getPlayersForTeam(teamId: Long): List<Player> = 
        database.teamPlayerDao().getPlayersForTeam(teamId)
    
    suspend fun getTeamsForPlayer(playerId: Long): List<Team> = 
        database.teamPlayerDao().getTeamsForPlayer(playerId)
    
    // --- Game Operations ---
    
    suspend fun insertGame(game: Game): Long = database.gameDao().insert(game)
    suspend fun updateGame(game: Game) = database.gameDao().update(game)
    suspend fun deleteGame(game: Game) = database.gameDao().delete(game)
    suspend fun getAllGames(): List<Game> = database.gameDao().getAllGames()
    fun getAllGamesFlow(): Flow<List<Game>> = database.gameDao().getAllGamesFlow()
    suspend fun getGameById(gameId: Long): Game? = database.gameDao().getGameById(gameId)
    suspend fun getGamesForTeam(teamId: Long): List<Game> = 
        database.gameDao().getGamesForTeam(teamId)
    
    // --- Game Event Operations ---
    
    suspend fun insertGameEvent(event: GameEvent): Long = database.gameEventDao().insert(event)
    suspend fun insertMultipleGameEvents(events: List<GameEvent>): List<Long> = 
        database.gameEventDao().insertAll(events)
    suspend fun deleteGameEvent(event: GameEvent) = database.gameEventDao().delete(event)
    suspend fun getEventsForGame(gameId: Long): List<GameEvent> = 
        database.gameEventDao().getEventsForGame(gameId)
    suspend fun getEventsForPlayer(playerId: Long): List<GameEvent> = 
        database.gameEventDao().getEventsForPlayer(playerId)
    
    // --- Complex Relationships ---
    
    suspend fun getTeamWithPlayers(teamId: Long): TeamWithPlayers? = 
        database.relationDao().getTeamWithPlayers(teamId)
    fun getTeamWithPlayersFlow(teamId: Long): Flow<TeamWithPlayers?> = 
        database.relationDao().getTeamWithPlayersFlow(teamId)
    
    suspend fun getPlayerWithTeams(playerId: Long): PlayerWithTeams? = 
        database.relationDao().getPlayerWithTeams(playerId)
    
    suspend fun getGameWithTeams(gameId: Long): GameWithTeams? = 
        database.relationDao().getGameWithTeams(gameId)
    fun getGameWithTeamsFlow(gameId: Long): Flow<GameWithTeams?> = 
        database.relationDao().getGameWithTeamsFlow(gameId)
    
    suspend fun getGameWithEvents(gameId: Long): GameWithEvents? = 
        database.relationDao().getGameWithEvents(gameId)
    
    suspend fun getGameWithTeamsAndEvents(gameId: Long): GameWithTeamsAndEvents? = 
        database.relationDao().getGameWithTeamsAndEvents(gameId)
    fun getGameWithTeamsAndEventsFlow(gameId: Long): Flow<GameWithTeamsAndEvents?> = 
        database.relationDao().getGameWithTeamsAndEventsFlow(gameId)
    
    suspend fun getPlayerWithEvents(playerId: Long): PlayerWithEvents? = 
        database.relationDao().getPlayerWithEvents(playerId)
    
    suspend fun getAllTeamsWithPlayers(): List<TeamWithPlayers> = 
        database.relationDao().getAllTeamsWithPlayers()
    fun getAllTeamsWithPlayersFlow(): Flow<List<TeamWithPlayers>> = 
        database.relationDao().getAllTeamsWithPlayersFlow()
    
    suspend fun getAllGamesWithTeams(): List<GameWithTeams> = 
        database.relationDao().getAllGamesWithTeams()
    fun getAllGamesWithTeamsFlow(): Flow<List<GameWithTeams>> = 
        database.relationDao().getAllGamesWithTeamsFlow()
    
    // --- Game Statistics Operations ---
    
    suspend fun insertGameStats(gameStats: GameStats): Long = 
        database.gameStatsDao().insert(gameStats)
    suspend fun updateGameStats(gameStats: GameStats) = 
        database.gameStatsDao().update(gameStats)
    suspend fun upsertGameStats(gameStats: GameStats) = 
        database.gameStatsDao().upsertGameStats(gameStats)
    
    suspend fun getGameOverallStats(gameId: Long): GameStats? = 
        database.gameStatsDao().getGameOverallStats(gameId)
    fun getGameOverallStatsFlow(gameId: Long): Flow<GameStats?> = 
        database.gameStatsDao().getGameOverallStatsFlow(gameId)
    
    suspend fun getTeamGameStats(gameId: Long, teamId: Long): GameStats? = 
        database.gameStatsDao().getTeamGameStats(gameId, teamId)
    fun getTeamGameStatsFlow(gameId: Long, teamId: Long): Flow<GameStats?> = 
        database.gameStatsDao().getTeamGameStatsFlow(gameId, teamId)
    
    suspend fun getAllTeamStatsForGame(gameId: Long): List<GameStats> = 
        database.gameStatsDao().getAllTeamStatsForGame(gameId)
    fun getAllTeamStatsForGameFlow(gameId: Long): Flow<List<GameStats>> = 
        database.gameStatsDao().getAllTeamStatsForGameFlow(gameId)
    
    // --- Player Game Statistics Operations ---
    
    suspend fun insertPlayerGameStats(playerGameStats: PlayerGameStats): Long = 
        database.playerGameStatsDao().insert(playerGameStats)
    suspend fun updatePlayerGameStats(playerGameStats: PlayerGameStats) = 
        database.playerGameStatsDao().update(playerGameStats)
    suspend fun upsertPlayerGameStats(playerGameStats: PlayerGameStats) = 
        database.playerGameStatsDao().upsertPlayerGameStats(playerGameStats)
    
    suspend fun getPlayerGameStats(gameId: Long, playerId: Long): PlayerGameStats? = 
        database.playerGameStatsDao().getPlayerGameStats(gameId, playerId)
    fun getPlayerGameStatsFlow(gameId: Long, playerId: Long): Flow<PlayerGameStats?> = 
        database.playerGameStatsDao().getPlayerGameStatsFlow(gameId, playerId)
    
    suspend fun getAllPlayerStatsForGame(gameId: Long): List<PlayerGameStats> = 
        database.playerGameStatsDao().getAllPlayerStatsForGame(gameId)
    fun getAllPlayerStatsForGameFlow(gameId: Long): Flow<List<PlayerGameStats>> = 
        database.playerGameStatsDao().getAllPlayerStatsForGameFlow(gameId)
    
    suspend fun getTeamPlayerStatsForGame(gameId: Long, teamId: Long): List<PlayerGameStats> = 
        database.playerGameStatsDao().getTeamPlayerStatsForGame(gameId, teamId)
    fun getTeamPlayerStatsForGameFlow(gameId: Long, teamId: Long): Flow<List<PlayerGameStats>> = 
        database.playerGameStatsDao().getTeamPlayerStatsForGameFlow(gameId, teamId)
    
    suspend fun getPlayerAllGameStats(playerId: Long): List<PlayerGameStats> = 
        database.playerGameStatsDao().getPlayerAllGameStats(playerId)
    fun getPlayerAllGameStatsFlow(playerId: Long): Flow<List<PlayerGameStats>> = 
        database.playerGameStatsDao().getPlayerAllGameStatsFlow(playerId)
    
    // --- Player Season Statistics Operations ---
    
    suspend fun insertPlayerSeasonStats(playerSeasonStats: PlayerSeasonStats): Long = 
        database.playerSeasonStatsDao().insert(playerSeasonStats)
    suspend fun updatePlayerSeasonStats(playerSeasonStats: PlayerSeasonStats) = 
        database.playerSeasonStatsDao().update(playerSeasonStats)
    suspend fun upsertPlayerSeasonStats(playerSeasonStats: PlayerSeasonStats) = 
        database.playerSeasonStatsDao().upsertPlayerSeasonStats(playerSeasonStats)
    
    suspend fun getPlayerSeasonStats(playerId: Long, seasonYear: Int, teamId: Long? = null): PlayerSeasonStats? = 
        database.playerSeasonStatsDao().getPlayerSeasonStats(playerId, seasonYear, teamId)
    fun getPlayerSeasonStatsFlow(playerId: Long, seasonYear: Int, teamId: Long? = null): Flow<PlayerSeasonStats?> = 
        database.playerSeasonStatsDao().getPlayerSeasonStatsFlow(playerId, seasonYear, teamId)
    
    suspend fun getPlayerCareerStats(playerId: Long): List<PlayerSeasonStats> = 
        database.playerSeasonStatsDao().getPlayerCareerStats(playerId)
    fun getPlayerCareerStatsFlow(playerId: Long): Flow<List<PlayerSeasonStats>> = 
        database.playerSeasonStatsDao().getPlayerCareerStatsFlow(playerId)
    
    suspend fun getTeamSeasonStats(teamId: Long, seasonYear: Int): List<PlayerSeasonStats> = 
        database.playerSeasonStatsDao().getTeamSeasonStats(teamId, seasonYear)
    fun getTeamSeasonStatsFlow(teamId: Long, seasonYear: Int): Flow<List<PlayerSeasonStats>> = 
        database.playerSeasonStatsDao().getTeamSeasonStatsFlow(teamId, seasonYear)
    
    suspend fun getAvailableSeasons(): List<Int> = 
        database.playerSeasonStatsDao().getAvailableSeasons()
    fun getAvailableSeasonsFlow(): Flow<List<Int>> = 
        database.playerSeasonStatsDao().getAvailableSeasonsFlow()
    
    // --- Statistics Calculation & Processing ---
    
    /**
     * Process a game event and update related statistics.
     * This should be called whenever a new GameEvent is inserted.
     */
    suspend fun processGameEvent(event: GameEvent) {
        // TODO: Implement logic to calculate and update:
        // 1. PlayerGameStats for the player involved
        // 2. GameStats for the team/game
        // 3. PlayerSeasonStats for season totals
        // This will be implemented in future iterations
    }
    
    /**
     * Recalculate all statistics for a game based on its events.
     * Useful for data integrity checks or after bulk event imports.
     */
    suspend fun recalculateGameStats(gameId: Long) {
        // TODO: Implement logic to:
        // 1. Get all GameEvents for the game
        // 2. Calculate stats from events
        // 3. Update GameStats and PlayerGameStats tables
        // This will be implemented in future iterations
    }
    
    /**
     * Update season statistics for a player after a game.
     * This should be called after game completion.
     */
    suspend fun updatePlayerSeasonFromGame(playerId: Long, gameId: Long, seasonYear: Int) {
        // TODO: Implement logic to:
        // 1. Get PlayerGameStats for the specific game
        // 2. Add those stats to PlayerSeasonStats
        // This will be implemented in future iterations
    }
}