package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.Team
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Team entity operations.
 * Provides suspending functions for all database operations.
 */
@Dao
interface TeamDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: Team): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<Team>): List<Long>
    
    @Update
    suspend fun update(team: Team)
    
    @Delete
    suspend fun delete(team: Team)
    
    @Query("DELETE FROM team WHERE id = :teamId")
    suspend fun deleteById(teamId: Long)
    
    @Query("SELECT * FROM team ORDER BY name")
    suspend fun getAllTeams(): List<Team>
    
    @Query("SELECT * FROM team ORDER BY name")
    fun getAllTeamsFlow(): Flow<List<Team>>
    
    @Query("SELECT * FROM team WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): Team?
    
    @Query("SELECT * FROM team WHERE id = :teamId")
    fun getTeamByIdFlow(teamId: Long): Flow<Team?>
    
    @Query("SELECT * FROM team WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name")
    suspend fun searchTeamsByName(searchQuery: String): List<Team>
    
    @Query("SELECT * FROM team WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name")
    fun searchTeamsByNameFlow(searchQuery: String): Flow<List<Team>>
}