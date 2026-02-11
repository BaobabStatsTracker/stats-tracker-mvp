package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.Game
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for Game entity operations.
 * Provides suspending functions for all database operations.
 */
@Dao
interface GameDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: Game): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<Game>): List<Long>
    
    @Update
    suspend fun update(game: Game)
    
    @Delete
    suspend fun delete(game: Game)
    
    @Query("DELETE FROM game WHERE id = :gameId")
    suspend fun deleteById(gameId: Long)
    
    @Query("SELECT * FROM game ORDER BY date DESC, id DESC")
    suspend fun getAllGames(): List<Game>
    
    @Query("SELECT * FROM game ORDER BY date DESC, id DESC")
    fun getAllGamesFlow(): Flow<List<Game>>
    
    @Query("SELECT * FROM game WHERE id = :gameId")
    suspend fun getGameById(gameId: Long): Game?
    
    @Query("SELECT * FROM game WHERE id = :gameId")
    fun getGameByIdFlow(gameId: Long): Flow<Game?>
    
    // Get games for a specific team (as home or away)
    @Query("""
        SELECT * FROM game 
        WHERE home_team_id = :teamId OR away_team_id = :teamId 
        ORDER BY date DESC
    """)
    suspend fun getGamesForTeam(teamId: Long): List<Game>
    
    @Query("""
        SELECT * FROM game 
        WHERE home_team_id = :teamId OR away_team_id = :teamId 
        ORDER BY date DESC
    """)
    fun getGamesForTeamFlow(teamId: Long): Flow<List<Game>>
    
    // Get games by date range
    @Query("""
        SELECT * FROM game 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    suspend fun getGamesByDateRange(startDate: LocalDate, endDate: LocalDate): List<Game>
    
    @Query("""
        SELECT * FROM game 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getGamesByDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Game>>
    
    // Get games on a specific date
    @Query("SELECT * FROM game WHERE date = :date ORDER BY id")
    suspend fun getGamesByDate(date: LocalDate): List<Game>
    
    @Query("SELECT * FROM game WHERE date = :date ORDER BY id")
    fun getGamesByDateFlow(date: LocalDate): Flow<List<Game>>
    
    // Get upcoming games
    @Query("SELECT * FROM game WHERE date >= :currentDate ORDER BY date ASC")
    suspend fun getUpcomingGames(currentDate: LocalDate): List<Game>
    
    @Query("SELECT * FROM game WHERE date >= :currentDate ORDER BY date ASC")
    fun getUpcomingGamesFlow(currentDate: LocalDate): Flow<List<Game>>
    
    // Get past games
    @Query("SELECT * FROM game WHERE date < :currentDate ORDER BY date DESC")
    suspend fun getPastGames(currentDate: LocalDate): List<Game>
    
    @Query("SELECT * FROM game WHERE date < :currentDate ORDER BY date DESC")
    fun getPastGamesFlow(currentDate: LocalDate): Flow<List<Game>>
}