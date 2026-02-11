package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.entity.TeamPlayer
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TeamPlayer entity operations.
 * Manages the many-to-many relationship between players and teams.
 */
@Dao
interface TeamPlayerDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(teamPlayer: TeamPlayer): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teamPlayers: List<TeamPlayer>): List<Long>
    
    @Update
    suspend fun update(teamPlayer: TeamPlayer)
    
    @Delete
    suspend fun delete(teamPlayer: TeamPlayer)
    
    @Query("DELETE FROM team_players WHERE id = :teamPlayerId")
    suspend fun deleteById(teamPlayerId: Long)
    
    @Query("DELETE FROM team_players WHERE player_id = :playerId AND team_id = :teamId")
    suspend fun unlinkPlayerFromTeam(playerId: Long, teamId: Long)
    
    @Query("SELECT * FROM team_players WHERE player_id = :playerId AND team_id = :teamId")
    suspend fun getTeamPlayer(playerId: Long, teamId: Long): TeamPlayer?
    
    @Query("SELECT * FROM team_players WHERE player_id = :playerId AND team_id = :teamId")
    fun getTeamPlayerFlow(playerId: Long, teamId: Long): Flow<TeamPlayer?>
    
    // Get all players for a specific team
    @Query("""
        SELECT p.* FROM player p
        INNER JOIN team_players tp ON p.id = tp.player_id
        WHERE tp.team_id = :teamId
        ORDER BY tp.jersey_num, p.last_name, p.first_name
    """)
    suspend fun getPlayersForTeam(teamId: Long): List<Player>
    
    @Query("""
        SELECT p.* FROM player p
        INNER JOIN team_players tp ON p.id = tp.player_id
        WHERE tp.team_id = :teamId
        ORDER BY tp.jersey_num, p.last_name, p.first_name
    """)
    fun getPlayersForTeamFlow(teamId: Long): Flow<List<Player>>
    
    // Get all teams for a specific player
    @Query("""
        SELECT t.* FROM team t
        INNER JOIN team_players tp ON t.id = tp.team_id
        WHERE tp.player_id = :playerId
        ORDER BY t.name
    """)
    suspend fun getTeamsForPlayer(playerId: Long): List<Team>
    
    @Query("""
        SELECT t.* FROM team t
        INNER JOIN team_players tp ON t.id = tp.team_id
        WHERE tp.player_id = :playerId
        ORDER BY t.name
    """)
    fun getTeamsForPlayerFlow(playerId: Long): Flow<List<Team>>
    
    // Get all team-player relationships
    @Query("SELECT * FROM team_players ORDER BY team_id, jersey_num")
    suspend fun getAllTeamPlayers(): List<TeamPlayer>
    
    @Query("SELECT * FROM team_players ORDER BY team_id, jersey_num")
    fun getAllTeamPlayersFlow(): Flow<List<TeamPlayer>>
    
    // Get team-player relationships for a specific team
    @Query("SELECT * FROM team_players WHERE team_id = :teamId ORDER BY jersey_num")
    suspend fun getTeamPlayersForTeam(teamId: Long): List<TeamPlayer>
    
    @Query("SELECT * FROM team_players WHERE team_id = :teamId ORDER BY jersey_num")
    fun getTeamPlayersForTeamFlow(teamId: Long): Flow<List<TeamPlayer>>
    
    // Update jersey number and role
    @Query("""
        UPDATE team_players 
        SET jersey_num = :jerseyNum, role = :role 
        WHERE player_id = :playerId AND team_id = :teamId
    """)
    suspend fun updatePlayerTeamInfo(
        playerId: Long, 
        teamId: Long, 
        jerseyNum: Int?, 
        role: String?
    )
}