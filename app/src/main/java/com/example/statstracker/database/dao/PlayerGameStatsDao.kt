package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.PlayerGameStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PlayerGameStats entity operations.
 * Manages individual player statistics per game.
 */
@Dao
interface PlayerGameStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playerGameStats: PlayerGameStats): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playerGameStats: List<PlayerGameStats>): List<Long>
    
    @Update
    suspend fun update(playerGameStats: PlayerGameStats)
    
    @Delete
    suspend fun delete(playerGameStats: PlayerGameStats)
    
    @Query("DELETE FROM player_game_stats WHERE id = :statsId")
    suspend fun deleteById(statsId: Long)
    
    // Get full game stats for a player
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter IS NULL")
    suspend fun getPlayerGameStats(gameId: Long, playerId: Long): PlayerGameStats?
    
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter IS NULL")
    fun getPlayerGameStatsFlow(gameId: Long, playerId: Long): Flow<PlayerGameStats?>
    
    // Get quarter stats for a player
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter = :quarter")
    suspend fun getPlayerQuarterStats(gameId: Long, playerId: Long, quarter: Int): PlayerGameStats?
    
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter = :quarter")
    fun getPlayerQuarterStatsFlow(gameId: Long, playerId: Long, quarter: Int): Flow<PlayerGameStats?>
    
    // Get all players' stats for a game
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND quarter IS NULL ORDER BY team_id, player_id")
    suspend fun getAllPlayerStatsForGame(gameId: Long): List<PlayerGameStats>
    
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND quarter IS NULL ORDER BY team_id, player_id")
    fun getAllPlayerStatsForGameFlow(gameId: Long): Flow<List<PlayerGameStats>>
    
    // Get team players' stats for a game
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NULL ORDER BY player_id")
    suspend fun getTeamPlayerStatsForGame(gameId: Long, teamId: Long): List<PlayerGameStats>
    
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NULL ORDER BY player_id")
    fun getTeamPlayerStatsForGameFlow(gameId: Long, teamId: Long): Flow<List<PlayerGameStats>>
    
    // Get all quarter stats for a player in a game
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter IS NOT NULL ORDER BY quarter")
    suspend fun getPlayerQuarterStatsList(gameId: Long, playerId: Long): List<PlayerGameStats>
    
    @Query("SELECT * FROM player_game_stats WHERE game_id = :gameId AND player_id = :playerId AND quarter IS NOT NULL ORDER BY quarter")
    fun getPlayerQuarterStatsListFlow(gameId: Long, playerId: Long): Flow<List<PlayerGameStats>>
    
    // Get all stats for a player across all games
    @Query("SELECT * FROM player_game_stats WHERE player_id = :playerId AND quarter IS NULL ORDER BY game_id DESC")
    suspend fun getPlayerAllGameStats(playerId: Long): List<PlayerGameStats>
    
    @Query("SELECT * FROM player_game_stats WHERE player_id = :playerId AND quarter IS NULL ORDER BY game_id DESC")
    fun getPlayerAllGameStatsFlow(playerId: Long): Flow<List<PlayerGameStats>>
    
    // Get player's stats for a specific team across all games
    @Query("SELECT * FROM player_game_stats WHERE player_id = :playerId AND team_id = :teamId AND quarter IS NULL ORDER BY game_id DESC")
    suspend fun getPlayerTeamStats(playerId: Long, teamId: Long): List<PlayerGameStats>
    
    @Query("SELECT * FROM player_game_stats WHERE player_id = :playerId AND team_id = :teamId AND quarter IS NULL ORDER BY game_id DESC")
    fun getPlayerTeamStatsFlow(playerId: Long, teamId: Long): Flow<List<PlayerGameStats>>
    
    // Delete all stats for a game
    @Query("DELETE FROM player_game_stats WHERE game_id = :gameId")
    suspend fun deleteAllStatsForGame(gameId: Long)
    
    // Delete all stats for a player
    @Query("DELETE FROM player_game_stats WHERE player_id = :playerId")
    suspend fun deleteAllStatsForPlayer(playerId: Long)
    
    // Get top performers for a specific stat
    @Query("""
        SELECT * FROM player_game_stats 
        WHERE quarter IS NULL 
        ORDER BY points DESC 
        LIMIT :limit
    """)
    suspend fun getTopScorers(limit: Int = 10): List<PlayerGameStats>
    
    @Query("""
        SELECT * FROM player_game_stats 
        WHERE quarter IS NULL 
        ORDER BY (rebounds_offensive + rebounds_defensive) DESC 
        LIMIT :limit
    """)
    suspend fun getTopRebounders(limit: Int = 10): List<PlayerGameStats>
    
    @Query("""
        SELECT * FROM player_game_stats 
        WHERE quarter IS NULL 
        ORDER BY assists DESC 
        LIMIT :limit
    """)
    suspend fun getTopAssistLeaders(limit: Int = 10): List<PlayerGameStats>
    
    // Upsert operation for stats updates
    @Transaction
    suspend fun upsertPlayerGameStats(playerGameStats: PlayerGameStats) {
        val existing = if (playerGameStats.quarter == null) {
            getPlayerGameStats(playerGameStats.gameId, playerGameStats.playerId)
        } else {
            getPlayerQuarterStats(playerGameStats.gameId, playerGameStats.playerId, playerGameStats.quarter)
        }
        
        if (existing != null) {
            update(playerGameStats.copy(id = existing.id))
        } else {
            insert(playerGameStats)
        }
    }
}