package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.GameEventType
import com.example.statstracker.database.entity.GameEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GameEvent entity operations.
 * Provides suspending functions for all database operations.
 */
@Dao
interface GameEventDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gameEvent: GameEvent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gameEvents: List<GameEvent>): List<Long>
    
    @Update
    suspend fun update(gameEvent: GameEvent)
    
    @Delete
    suspend fun delete(gameEvent: GameEvent)
    
    @Query("DELETE FROM game_event WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)
    
    @Query("DELETE FROM game_event WHERE game_id = :gameId")
    suspend fun deleteAllEventsForGame(gameId: Long)
    
    // Get all events for a specific game, ordered by timestamp
    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY timestamp ASC")
    suspend fun getEventsForGame(gameId: Long): List<GameEvent>
    
    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY timestamp ASC")
    fun getEventsForGameFlow(gameId: Long): Flow<List<GameEvent>>
    
    // Get events for a specific player in a specific game
    @Query("""
        SELECT * FROM game_event 
        WHERE game_id = :gameId AND player_id = :playerId 
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsForPlayerInGame(gameId: Long, playerId: Long): List<GameEvent>
    
    @Query("""
        SELECT * FROM game_event 
        WHERE game_id = :gameId AND player_id = :playerId 
        ORDER BY timestamp ASC
    """)
    fun getEventsForPlayerInGameFlow(gameId: Long, playerId: Long): Flow<List<GameEvent>>
    
    // Get all events for a specific player across all games
    @Query("SELECT * FROM game_event WHERE player_id = :playerId ORDER BY game_id, timestamp")
    suspend fun getEventsForPlayer(playerId: Long): List<GameEvent>
    
    @Query("SELECT * FROM game_event WHERE player_id = :playerId ORDER BY game_id, timestamp")
    fun getEventsForPlayerFlow(playerId: Long): Flow<List<GameEvent>>
    
    // Get events by type for a specific game
    @Query("""
        SELECT * FROM game_event 
        WHERE game_id = :gameId AND event_type = :eventType 
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsByTypeForGame(gameId: Long, eventType: GameEventType): List<GameEvent>
    
    @Query("""
        SELECT * FROM game_event 
        WHERE game_id = :gameId AND event_type = :eventType 
        ORDER BY timestamp ASC
    """)
    fun getEventsByTypeForGameFlow(gameId: Long, eventType: GameEventType): Flow<List<GameEvent>>
    
    // Get events by type for a specific player
    @Query("""
        SELECT * FROM game_event 
        WHERE player_id = :playerId AND event_type = :eventType 
        ORDER BY game_id, timestamp
    """)
    suspend fun getEventsByTypeForPlayer(playerId: Long, eventType: GameEventType): List<GameEvent>
    
    @Query("""
        SELECT * FROM game_event 
        WHERE player_id = :playerId AND event_type = :eventType 
        ORDER BY game_id, timestamp
    """)
    fun getEventsByTypeForPlayerFlow(playerId: Long, eventType: GameEventType): Flow<List<GameEvent>>
    
    // Get event counts by type for a game (useful for statistics)
    @Query("""
        SELECT event_type, COUNT(*) as count 
        FROM game_event 
        WHERE game_id = :gameId 
        GROUP BY event_type
    """)
    suspend fun getEventCountsByTypeForGame(gameId: Long): Map<GameEventType, Int>
    
    // Get event counts by type for a player across all games
    @Query("""
        SELECT event_type, COUNT(*) as count 
        FROM game_event 
        WHERE player_id = :playerId 
        GROUP BY event_type
    """)
    suspend fun getEventCountsByTypeForPlayer(playerId: Long): Map<GameEventType, Int>
    
    // Get specific event by id
    @Query("SELECT * FROM game_event WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): GameEvent?
    
    @Query("SELECT * FROM game_event WHERE id = :eventId")
    fun getEventByIdFlow(eventId: Long): Flow<GameEvent?>
}