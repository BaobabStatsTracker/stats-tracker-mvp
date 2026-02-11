package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.relation.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for complex relationship queries.
 * Provides methods to fetch entities with their related data in single queries.
 */
@Dao
interface RelationDao {
    
    // --- Team with Players Relations ---
    
    @Transaction
    @Query("SELECT * FROM team ORDER BY name")
    suspend fun getAllTeamsWithPlayers(): List<TeamWithPlayers>
    
    @Transaction
    @Query("SELECT * FROM team ORDER BY name")
    fun getAllTeamsWithPlayersFlow(): Flow<List<TeamWithPlayers>>
    
    @Transaction
    @Query("SELECT * FROM team WHERE id = :teamId")
    suspend fun getTeamWithPlayers(teamId: Long): TeamWithPlayers?
    
    @Transaction
    @Query("SELECT * FROM team WHERE id = :teamId")
    fun getTeamWithPlayersFlow(teamId: Long): Flow<TeamWithPlayers?>
    
    @Transaction
    @Query("SELECT * FROM team WHERE id = :teamId")
    suspend fun getTeamWithDetailedPlayers(teamId: Long): TeamWithDetailedPlayers?
    
    @Transaction
    @Query("SELECT * FROM team WHERE id = :teamId")
    fun getTeamWithDetailedPlayersFlow(teamId: Long): Flow<TeamWithDetailedPlayers?>
    
    // --- Player with Teams Relations ---
    
    @Transaction
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    suspend fun getAllPlayersWithTeams(): List<PlayerWithTeams>
    
    @Transaction
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    fun getAllPlayersWithTeamsFlow(): Flow<List<PlayerWithTeams>>
    
    @Transaction
    @Query("SELECT * FROM player WHERE id = :playerId")
    suspend fun getPlayerWithTeams(playerId: Long): PlayerWithTeams?
    
    @Transaction
    @Query("SELECT * FROM player WHERE id = :playerId")
    fun getPlayerWithTeamsFlow(playerId: Long): Flow<PlayerWithTeams?>
    
    // --- Game with Teams Relations ---
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC")
    suspend fun getAllGamesWithTeams(): List<GameWithTeams>
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC")
    fun getAllGamesWithTeamsFlow(): Flow<List<GameWithTeams>>
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    suspend fun getGameWithTeams(gameId: Long): GameWithTeams?
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    fun getGameWithTeamsFlow(gameId: Long): Flow<GameWithTeams?>
    
    // --- Game with Events Relations ---
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    suspend fun getGameWithEvents(gameId: Long): GameWithEvents?
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    fun getGameWithEventsFlow(gameId: Long): Flow<GameWithEvents?>
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC")
    suspend fun getAllGamesWithEvents(): List<GameWithEvents>
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC")
    fun getAllGamesWithEventsFlow(): Flow<List<GameWithEvents>>
    
    // --- Complete Game Relations ---
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    suspend fun getGameWithTeamsAndEvents(gameId: Long): GameWithTeamsAndEvents?
    
    @Transaction
    @Query("SELECT * FROM game WHERE id = :gameId")
    fun getGameWithTeamsAndEventsFlow(gameId: Long): Flow<GameWithTeamsAndEvents?>
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentGamesWithTeamsAndEvents(limit: Int): List<GameWithTeamsAndEvents>
    
    @Transaction
    @Query("SELECT * FROM game ORDER BY date DESC LIMIT :limit")
    fun getRecentGamesWithTeamsAndEventsFlow(limit: Int): Flow<List<GameWithTeamsAndEvents>>
    
    // --- Player with Events Relations ---
    
    @Transaction
    @Query("SELECT * FROM player WHERE id = :playerId")
    suspend fun getPlayerWithEvents(playerId: Long): PlayerWithEvents?
    
    @Transaction
    @Query("SELECT * FROM player WHERE id = :playerId")
    fun getPlayerWithEventsFlow(playerId: Long): Flow<PlayerWithEvents?>
    
    @Transaction
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    suspend fun getAllPlayersWithEvents(): List<PlayerWithEvents>
    
    @Transaction
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    fun getAllPlayersWithEventsFlow(): Flow<List<PlayerWithEvents>>
    
    // --- Game Event with Context Relations ---
    
    @Transaction
    @Query("SELECT * FROM game_event WHERE id = :eventId")
    suspend fun getGameEventWithPlayerAndGame(eventId: Long): GameEventWithPlayerAndGame?
    
    @Transaction
    @Query("SELECT * FROM game_event WHERE id = :eventId")
    fun getGameEventWithPlayerAndGameFlow(eventId: Long): Flow<GameEventWithPlayerAndGame?>
    
    @Transaction
    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY timestamp")
    suspend fun getGameEventsWithPlayerAndGame(gameId: Long): List<GameEventWithPlayerAndGame>
    
    @Transaction
    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY timestamp")
    fun getGameEventsWithPlayerAndGameFlow(gameId: Long): Flow<List<GameEventWithPlayerAndGame>>
    
    // --- Team-specific game queries ---
    
    @Transaction
    @Query("""
        SELECT * FROM game 
        WHERE home_team_id = :teamId OR away_team_id = :teamId 
        ORDER BY date DESC
    """)
    suspend fun getGamesWithTeamsForTeam(teamId: Long): List<GameWithTeams>
    
    @Transaction
    @Query("""
        SELECT * FROM game 
        WHERE home_team_id = :teamId OR away_team_id = :teamId 
        ORDER BY date DESC
    """)
    fun getGamesWithTeamsForTeamFlow(teamId: Long): Flow<List<GameWithTeams>>
}