package com.example.statstracker.database.repository

import com.example.statstracker.database.BasketballDatabase
import com.example.statstracker.database.entity.*
import com.example.statstracker.database.relation.*
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
}