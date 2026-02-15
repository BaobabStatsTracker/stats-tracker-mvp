package com.example.statstracker.database.dao

import androidx.room.*
import com.example.statstracker.database.entity.PlayerSeasonStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PlayerSeasonStats entity operations.
 * Manages aggregated player statistics across seasons.
 */
@Dao
interface PlayerSeasonStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playerSeasonStats: PlayerSeasonStats): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playerSeasonStats: List<PlayerSeasonStats>): List<Long>
    
    @Update
    suspend fun update(playerSeasonStats: PlayerSeasonStats)
    
    @Delete
    suspend fun delete(playerSeasonStats: PlayerSeasonStats)
    
    @Query("DELETE FROM player_season_stats WHERE id = :statsId")
    suspend fun deleteById(statsId: Long)
    
    // Get season stats for a player
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId AND season_year = :seasonYear AND team_id = :teamId")
    suspend fun getPlayerSeasonStats(playerId: Long, seasonYear: Int, teamId: Long?): PlayerSeasonStats?
    
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId AND season_year = :seasonYear AND team_id = :teamId")
    fun getPlayerSeasonStatsFlow(playerId: Long, seasonYear: Int, teamId: Long?): Flow<PlayerSeasonStats?>
    
    // Get all season stats for a player across all teams
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId AND season_year = :seasonYear ORDER BY team_id")
    suspend fun getPlayerAllTeamsSeasonStats(playerId: Long, seasonYear: Int): List<PlayerSeasonStats>
    
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId AND season_year = :seasonYear ORDER BY team_id")
    fun getPlayerAllTeamsSeasonStatsFlow(playerId: Long, seasonYear: Int): Flow<List<PlayerSeasonStats>>
    
    // Get career stats for a player (all seasons)
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId ORDER BY season_year DESC, team_id")
    suspend fun getPlayerCareerStats(playerId: Long): List<PlayerSeasonStats>
    
    @Query("SELECT * FROM player_season_stats WHERE player_id = :playerId ORDER BY season_year DESC, team_id")
    fun getPlayerCareerStatsFlow(playerId: Long): Flow<List<PlayerSeasonStats>>
    
    // Get all players' stats for a specific season and team
    @Query("SELECT * FROM player_season_stats WHERE team_id = :teamId AND season_year = :seasonYear ORDER BY points_total DESC")
    suspend fun getTeamSeasonStats(teamId: Long, seasonYear: Int): List<PlayerSeasonStats>
    
    @Query("SELECT * FROM player_season_stats WHERE team_id = :teamId AND season_year = :seasonYear ORDER BY points_total DESC") 
    fun getTeamSeasonStatsFlow(teamId: Long, seasonYear: Int): Flow<List<PlayerSeasonStats>>
    
    // Get all players' stats for a specific season (across all teams)
    @Query("SELECT * FROM player_season_stats WHERE season_year = :seasonYear ORDER BY points_total DESC")
    suspend fun getAllPlayersSeasonStats(seasonYear: Int): List<PlayerSeasonStats>
    
    @Query("SELECT * FROM player_season_stats WHERE season_year = :seasonYear ORDER BY points_total DESC")
    fun getAllPlayersSeasonStatsFlow(seasonYear: Int): Flow<List<PlayerSeasonStats>>
    
    // Get available seasons
    @Query("SELECT DISTINCT season_year FROM player_season_stats ORDER BY season_year DESC")
    suspend fun getAvailableSeasons(): List<Int>
    
    @Query("SELECT DISTINCT season_year FROM player_season_stats ORDER BY season_year DESC")
    fun getAvailableSeasonsFlow(): Flow<List<Int>>
    
    // Delete all stats for a specific season
    @Query("DELETE FROM player_season_stats WHERE season_year = :seasonYear")
    suspend fun deleteSeasonStats(seasonYear: Int)
    
    // Delete all stats for a player
    @Query("DELETE FROM player_season_stats WHERE player_id = :playerId")
    suspend fun deletePlayerStats(playerId: Long)
    
    // Delete stats for a player-team combination
    @Query("DELETE FROM player_season_stats WHERE player_id = :playerId AND team_id = :teamId")
    suspend fun deletePlayerTeamStats(playerId: Long, teamId: Long)
    
    // Top performers queries
    @Query("""
        SELECT * FROM player_season_stats 
        WHERE season_year = :seasonYear 
        ORDER BY points_total DESC 
        LIMIT :limit
    """)
    suspend fun getTopScorers(seasonYear: Int, limit: Int = 10): List<PlayerSeasonStats>
    
    @Query("""
        SELECT * FROM player_season_stats 
        WHERE season_year = :seasonYear 
        ORDER BY (rebounds_offensive + rebounds_defensive) DESC 
        LIMIT :limit
    """)
    suspend fun getTopRebounders(seasonYear: Int, limit: Int = 10): List<PlayerSeasonStats>
    
    @Query("""
        SELECT * FROM player_season_stats 
        WHERE season_year = :seasonYear 
        ORDER BY assists_total DESC 
        LIMIT :limit
    """)
    suspend fun getTopAssistLeaders(seasonYear: Int, limit: Int = 10): List<PlayerSeasonStats>
    
    @Query("""
        SELECT * FROM player_season_stats 
        WHERE season_year = :seasonYear AND field_goals_attempted >= :minAttempts
        ORDER BY (CAST(field_goals_made AS REAL) / field_goals_attempted) DESC 
        LIMIT :limit
    """)
    suspend fun getTopFieldGoalPercentage(seasonYear: Int, minAttempts: Int = 50, limit: Int = 10): List<PlayerSeasonStats>
    
    @Query("""
        SELECT * FROM player_season_stats 
        WHERE season_year = :seasonYear AND three_pointers_attempted >= :minAttempts
        ORDER BY (CAST(three_pointers_made AS REAL) / three_pointers_attempted) DESC 
        LIMIT :limit
    """)
    suspend fun getTopThreePointPercentage(seasonYear: Int, minAttempts: Int = 25, limit: Int = 10): List<PlayerSeasonStats>
    
    // Upsert operation for stats updates
    @Transaction
    suspend fun upsertPlayerSeasonStats(playerSeasonStats: PlayerSeasonStats) {
        val existing = getPlayerSeasonStats(
            playerSeasonStats.playerId,
            playerSeasonStats.seasonYear,
            playerSeasonStats.teamId
        )
        
        if (existing != null) {
            update(playerSeasonStats.copy(id = existing.id))
        } else {
            insert(playerSeasonStats)
        }
    }
    
    // Update stats incrementally (for adding new game data)
    @Query("""
        UPDATE player_season_stats 
        SET games_played = games_played + :gamesPlayed,
            games_started = games_started + :gamesStarted,
            total_minutes_played = total_minutes_played + :minutesPlayed,
            points_total = points_total + :points,
            field_goals_made = field_goals_made + :fieldGoalsMade,
            field_goals_attempted = field_goals_attempted + :fieldGoalsAttempted,
            three_pointers_made = three_pointers_made + :threePointersMade,
            three_pointers_attempted = three_pointers_attempted + :threePointersAttempted,
            free_throws_made = free_throws_made + :freeThrowsMade,
            free_throws_attempted = free_throws_attempted + :freeThrowsAttempted,
            rebounds_offensive = rebounds_offensive + :reboundsOffensive,
            rebounds_defensive = rebounds_defensive + :reboundsDefensive,
            assists_total = assists_total + :assists,
            steals_total = steals_total + :steals,
            blocks_total = blocks_total + :blocks,
            turnovers_total = turnovers_total + :turnovers,
            fouls_personal = fouls_personal + :foulsPersonal,
            fouls_technical = fouls_technical + :foulsTechnical,
            plus_minus_total = plus_minus_total + :plusMinus
        WHERE player_id = :playerId AND season_year = :seasonYear AND team_id = :teamId
    """)
    suspend fun incrementPlayerSeasonStats(
        playerId: Long,
        seasonYear: Int,
        teamId: Long?,
        gamesPlayed: Int = 0,
        gamesStarted: Int = 0,
        minutesPlayed: Int = 0,
        points: Int = 0,
        fieldGoalsMade: Int = 0,
        fieldGoalsAttempted: Int = 0,
        threePointersMade: Int = 0,
        threePointersAttempted: Int = 0,
        freeThrowsMade: Int = 0,
        freeThrowsAttempted: Int = 0,
        reboundsOffensive: Int = 0,
        reboundsDefensive: Int = 0,
        assists: Int = 0,
        steals: Int = 0,
        blocks: Int = 0,
        turnovers: Int = 0,
        foulsPersonal: Int = 0,
        foulsTechnical: Int = 0,
        plusMinus: Int = 0
    )
}