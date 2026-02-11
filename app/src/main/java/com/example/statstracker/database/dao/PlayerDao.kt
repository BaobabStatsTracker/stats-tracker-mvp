package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.Player
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Player entity operations.
 * Provides suspending functions for all database operations.
 */
@Dao
interface PlayerDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: Player): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<Player>): List<Long>
    
    @Update
    suspend fun update(player: Player)
    
    @Delete
    suspend fun delete(player: Player)
    
    @Query("DELETE FROM player WHERE id = :playerId")
    suspend fun deleteById(playerId: Long)
    
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    suspend fun getAllPlayers(): List<Player>
    
    @Query("SELECT * FROM player ORDER BY last_name, first_name")
    fun getAllPlayersFlow(): Flow<List<Player>>
    
    @Query("SELECT * FROM player WHERE id = :playerId")
    suspend fun getPlayerById(playerId: Long): Player?
    
    @Query("SELECT * FROM player WHERE id = :playerId")
    fun getPlayerByIdFlow(playerId: Long): Flow<Player?>
    
    @Query("""
        SELECT * FROM player 
        WHERE first_name LIKE '%' || :searchQuery || '%' 
        OR last_name LIKE '%' || :searchQuery || '%'
        ORDER BY last_name, first_name
    """)
    suspend fun searchPlayersByName(searchQuery: String): List<Player>
    
    @Query("""
        SELECT * FROM player 
        WHERE first_name LIKE '%' || :searchQuery || '%' 
        OR last_name LIKE '%' || :searchQuery || '%'
        ORDER BY last_name, first_name
    """)
    fun searchPlayersByNameFlow(searchQuery: String): Flow<List<Player>>
}