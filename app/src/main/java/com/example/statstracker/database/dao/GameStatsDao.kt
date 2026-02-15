package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.GameStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GameStats entity operations.
 * Manages aggregated game-level statistics.
 */
@Dao
interface GameStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gameStats: GameStats): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gameStats: List<GameStats>): List<Long>
    
    @Update
    suspend fun update(gameStats: GameStats)
    
    @Delete
    suspend fun delete(gameStats: GameStats)
    
    @Query("DELETE FROM game_stats WHERE id = :statsId")
    suspend fun deleteById(statsId: Long)
    
    // Get stats for a specific game
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id IS NULL AND quarter IS NULL")
    suspend fun getGameOverallStats(gameId: Long): GameStats?
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id IS NULL AND quarter IS NULL")
    fun getGameOverallStatsFlow(gameId: Long): Flow<GameStats?>
    
    // Get team stats for a specific game
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NULL")
    suspend fun getTeamGameStats(gameId: Long, teamId: Long): GameStats?
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NULL")
    fun getTeamGameStatsFlow(gameId: Long, teamId: Long): Flow<GameStats?>
    
    // Get all team stats for a game
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id IS NOT NULL AND quarter IS NULL ORDER BY team_id")
    suspend fun getAllTeamStatsForGame(gameId: Long): List<GameStats>
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id IS NOT NULL AND quarter IS NULL ORDER BY team_id")
    fun getAllTeamStatsForGameFlow(gameId: Long): Flow<List<GameStats>>
    
    // Quarter-specific stats
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter = :quarter")
    suspend fun getQuarterStats(gameId: Long, teamId: Long, quarter: Int): GameStats?
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter = :quarter")
    fun getQuarterStatsFlow(gameId: Long, teamId: Long, quarter: Int): Flow<GameStats?>
    
    // Get all quarter stats for a team in a game
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NOT NULL ORDER BY quarter")
    suspend fun getTeamQuarterStats(gameId: Long, teamId: Long): List<GameStats>
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId AND team_id = :teamId AND quarter IS NOT NULL ORDER BY quarter")
    fun getTeamQuarterStatsFlow(gameId: Long, teamId: Long): Flow<List<GameStats>>
    
    // Get all stats for a game (overall + teams + quarters)
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId ORDER BY team_id, quarter")
    suspend fun getAllGameStats(gameId: Long): List<GameStats>
    
    @Query("SELECT * FROM game_stats WHERE game_id = :gameId ORDER BY team_id, quarter")
    fun getAllGameStatsFlow(gameId: Long): Flow<List<GameStats>>
    
    // Delete all stats for a game
    @Query("DELETE FROM game_stats WHERE game_id = :gameId")
    suspend fun deleteAllStatsForGame(gameId: Long)
    
    // Upsert operation for stats updates
    @Transaction
    suspend fun upsertGameStats(gameStats: GameStats) {
        val existing = if (gameStats.quarter == null) {
            if (gameStats.teamId == null) {
                getGameOverallStats(gameStats.gameId)
            } else {
                getTeamGameStats(gameStats.gameId, gameStats.teamId)
            }
        } else {
            getQuarterStats(gameStats.gameId, gameStats.teamId!!, gameStats.quarter)
        }
        
        if (existing != null) {
            update(gameStats.copy(id = existing.id))
        } else {
            insert(gameStats)
        }
    }
}