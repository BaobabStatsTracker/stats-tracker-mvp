package com.example.statstracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.statstracker.model.*
import java.time.LocalDate

/**
 * Basketball Stats Tracker - Database Entities
 * 
 * This file contains all Room database entities for the basketball statistics tracking app:
 * - Player: Individual player information and attributes
 * - Team: Team information and metadata  
 * - TeamPlayer: Many-to-many relationship between players and teams
 * - Game: Game information with home/away teams
 * - GameEvent: Individual events during games
 * - GameStats: Aggregated game-level statistics
 * - PlayerGameStats: Individual player performance per game
 * - PlayerSeasonStats: Aggregated player statistics across seasons
 */
@Entity(tableName = "player")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "image")
    val image: String? = null,

    @ColumnInfo(name = "height_cm")
    val heightCm: Int? = null,

    @ColumnInfo(name = "wingspan_cm")
    val wingspanCm: Int? = null,

    @ColumnInfo(name = "primary_hand")
    val primaryHand: PrimaryHand? = null,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: LocalDate? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)

// --- Team Entity ---

/**
 * Represents a basketball team with basic information.
 */
@Entity(tableName = "team")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "logo")
    val logo: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null
)

// --- TeamPlayer Entity (Join table with extra fields) ---

/**
 * Represents the many-to-many relationship between players and teams,
 * with additional context like jersey number and role.
 */
@Entity(
    tableName = "team_players",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["player_id"]),
        Index(value = ["team_id"]),
        Index(value = ["player_id", "team_id"], unique = true)
    ]
)
data class TeamPlayer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "player_id")
    val playerId: Long,
    
    @ColumnInfo(name = "team_id")
    val teamId: Long,
    
    @ColumnInfo(name = "jersey_num")
    val jerseyNum: Int? = null,
    
    @ColumnInfo(name = "role")
    val role: PlayerRole? = null
)

// --- Game Entity ---

/**
 * Represents a basketball game between two teams.
 */
@Entity(
    tableName = "game",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["home_team_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["away_team_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["home_team_id"]),
        Index(value = ["away_team_id"]),
        Index(value = ["date"])
    ]
)
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "home_team_id")
    val homeTeamId: Long,
    
    @ColumnInfo(name = "away_team_id")
    val awayTeamId: Long,
    
    @ColumnInfo(name = "date")
    val date: LocalDate,
    
    @ColumnInfo(name = "place")
    val place: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "home_tracking_mode")
    val homeTrackingMode: TrackingMode,
    
    @ColumnInfo(name = "away_tracking_mode")
    val awayTrackingMode: TrackingMode
)

// --- GameEvent Entity ---

/**
 * Represents an event that occurred during a basketball game,
 * such as shots, fouls, assists, etc.
 */
@Entity(
    tableName = "game_event",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["player_id"]),
        Index(value = ["game_id", "timestamp"])
    ]
)
data class GameEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "game_id")
    val gameId: Long,
    
    @ColumnInfo(name = "player_id")
    val playerId: Long?,
    
    @ColumnInfo(name = "team")
    val team: GameTeamSide,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Int, // seconds from game start
    
    @ColumnInfo(name = "event_type")
    val eventType: GameEventType,
    
    // Enhanced fields for shot tracking and analytics
    @ColumnInfo(name = "location_x")
    val locationX: Double? = null, // Court X coordinate (0-1)
    
    @ColumnInfo(name = "location_y")
    val locationY: Double? = null, // Court Y coordinate (0-1)
    
    @ColumnInfo(name = "shot_distance")
    val shotDistance: Double? = null, // Distance from basket in feet
    
    @ColumnInfo(name = "shot_result")
    val shotResult: String? = null, // 'made', 'missed', 'blocked'
    
    @ColumnInfo(name = "assist_player_id")
    val assistPlayerId: Long? = null,
    
    @ColumnInfo(name = "foul_type")
    val foulType: String? = null, // 'personal', 'technical', 'flagrant'
    
    @ColumnInfo(name = "points_value")
    val pointsValue: Int? = null // 1, 2, or 3 points
)

// --- GameStats Entity ---

/**
 * Represents aggregated statistics for a game at team level.
 * Supports both overall game stats and quarter-by-quarter breakdowns.
 */
@Entity(
    tableName = "game_stats",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["team_id"]),
        Index(value = ["game_id", "team_id", "quarter"], unique = true)
    ]
)
data class GameStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "game_id")
    val gameId: Long,
    
    @ColumnInfo(name = "team_id")
    val teamId: Long?, // NULL for overall game stats
    
    @ColumnInfo(name = "quarter")
    val quarter: Int?, // NULL for full game stats
    
    // Scoring Stats
    @ColumnInfo(name = "points")
    val points: Int = 0,
    
    @ColumnInfo(name = "field_goals_made")
    val fieldGoalsMade: Int = 0,
    
    @ColumnInfo(name = "field_goals_attempted")
    val fieldGoalsAttempted: Int = 0,
    
    @ColumnInfo(name = "three_pointers_made")
    val threePointersMade: Int = 0,
    
    @ColumnInfo(name = "three_pointers_attempted")
    val threePointersAttempted: Int = 0,
    
    @ColumnInfo(name = "free_throws_made")
    val freeThrowsMade: Int = 0,
    
    @ColumnInfo(name = "free_throws_attempted")
    val freeThrowsAttempted: Int = 0,
    
    // Possession Stats
    @ColumnInfo(name = "rebounds_offensive")
    val reboundsOffensive: Int = 0,
    
    @ColumnInfo(name = "rebounds_defensive")
    val reboundsDefensive: Int = 0,
    
    @ColumnInfo(name = "assists")
    val assists: Int = 0,
    
    @ColumnInfo(name = "steals")
    val steals: Int = 0,
    
    @ColumnInfo(name = "blocks")
    val blocks: Int = 0,
    
    @ColumnInfo(name = "turnovers")
    val turnovers: Int = 0,
    
    // Defensive Stats
    @ColumnInfo(name = "fouls_personal")
    val foulsPersonal: Int = 0,
    
    @ColumnInfo(name = "fouls_technical")
    val foulsTechnical: Int = 0,
    
    @ColumnInfo(name = "time_played_seconds")
    val timePlayedSeconds: Int = 0
) {
    // Computed properties
    val totalRebounds: Int get() = reboundsOffensive + reboundsDefensive
    val fieldGoalPercentage: Double 
        get() = if (fieldGoalsAttempted > 0) fieldGoalsMade.toDouble() / fieldGoalsAttempted else 0.0
    val threePointPercentage: Double 
        get() = if (threePointersAttempted > 0) threePointersMade.toDouble() / threePointersAttempted else 0.0
    val freeThrowPercentage: Double 
        get() = if (freeThrowsAttempted > 0) freeThrowsMade.toDouble() / freeThrowsAttempted else 0.0
}

// --- PlayerGameStats Entity ---

/**
 * Represents individual player statistics for a specific game.
 * Supports quarter-by-quarter and full-game statistics.
 */
@Entity(
    tableName = "player_game_stats",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["player_id"]),
        Index(value = ["team_id"]),
        Index(value = ["game_id", "player_id", "quarter"], unique = true)
    ]
)
data class PlayerGameStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "game_id")
    val gameId: Long,
    
    @ColumnInfo(name = "player_id")
    val playerId: Long,
    
    @ColumnInfo(name = "team_id")
    val teamId: Long,
    
    @ColumnInfo(name = "quarter")
    val quarter: Int?, // NULL for full game stats
    
    // Scoring Stats
    @ColumnInfo(name = "points")
    val points: Int = 0,
    
    @ColumnInfo(name = "field_goals_made")
    val fieldGoalsMade: Int = 0,
    
    @ColumnInfo(name = "field_goals_attempted")
    val fieldGoalsAttempted: Int = 0,
    
    @ColumnInfo(name = "three_pointers_made")
    val threePointersMade: Int = 0,
    
    @ColumnInfo(name = "three_pointers_attempted")
    val threePointersAttempted: Int = 0,
    
    @ColumnInfo(name = "free_throws_made")
    val freeThrowsMade: Int = 0,
    
    @ColumnInfo(name = "free_throws_attempted")
    val freeThrowsAttempted: Int = 0,
    
    // Possession Stats
    @ColumnInfo(name = "rebounds_offensive")
    val reboundsOffensive: Int = 0,
    
    @ColumnInfo(name = "rebounds_defensive")
    val reboundsDefensive: Int = 0,
    
    @ColumnInfo(name = "assists")
    val assists: Int = 0,
    
    @ColumnInfo(name = "steals")
    val steals: Int = 0,
    
    @ColumnInfo(name = "blocks")
    val blocks: Int = 0,
    
    @ColumnInfo(name = "turnovers")
    val turnovers: Int = 0,
    
    // Defensive Stats  
    @ColumnInfo(name = "fouls_personal")
    val foulsPersonal: Int = 0,
    
    @ColumnInfo(name = "fouls_technical")
    val foulsTechnical: Int = 0,
    
    // Advanced Stats
    @ColumnInfo(name = "plus_minus")
    val plusMinus: Int = 0,
    
    @ColumnInfo(name = "time_played_seconds")
    val timePlayedSeconds: Int = 0,
    
    // Shot Chart Data (JSON string)
    @ColumnInfo(name = "shot_chart_data")
    val shotChartData: String? = null
) {
    // Computed properties
    val totalRebounds: Int get() = reboundsOffensive + reboundsDefensive
    val fieldGoalPercentage: Double 
        get() = if (fieldGoalsAttempted > 0) fieldGoalsMade.toDouble() / fieldGoalsAttempted else 0.0
    val threePointPercentage: Double 
        get() = if (threePointersAttempted > 0) threePointersMade.toDouble() / threePointersAttempted else 0.0
    val freeThrowPercentage: Double 
        get() = if (freeThrowsAttempted > 0) freeThrowsMade.toDouble() / freeThrowsAttempted else 0.0
}

// --- PlayerSeasonStats Entity ---

/**
 * Represents aggregated player statistics across a season.
 * Enables historical tracking and season-over-season comparisons.
 */
@Entity(
    tableName = "player_season_stats",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["player_id"]),
        Index(value = ["team_id"]),
        Index(value = ["season_year"]),
        Index(value = ["player_id", "team_id", "season_year"], unique = true)
    ]
)
data class PlayerSeasonStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "player_id")
    val playerId: Long,
    
    @ColumnInfo(name = "team_id")
    val teamId: Long?,
    
    @ColumnInfo(name = "season_year")
    val seasonYear: Int, // e.g., 2024
    
    // Game Participation
    @ColumnInfo(name = "games_played")
    val gamesPlayed: Int = 0,
    
    @ColumnInfo(name = "games_started")
    val gamesStarted: Int = 0,
    
    @ColumnInfo(name = "total_minutes_played")
    val totalMinutesPlayed: Int = 0,
    
    // Scoring Totals
    @ColumnInfo(name = "points_total")
    val pointsTotal: Int = 0,
    
    @ColumnInfo(name = "field_goals_made")
    val fieldGoalsMade: Int = 0,
    
    @ColumnInfo(name = "field_goals_attempted")
    val fieldGoalsAttempted: Int = 0,
    
    @ColumnInfo(name = "three_pointers_made")
    val threePointersMade: Int = 0,
    
    @ColumnInfo(name = "three_pointers_attempted")
    val threePointersAttempted: Int = 0,
    
    @ColumnInfo(name = "free_throws_made")
    val freeThrowsMade: Int = 0,
    
    @ColumnInfo(name = "free_throws_attempted")
    val freeThrowsAttempted: Int = 0,
    
    // Possession Totals
    @ColumnInfo(name = "rebounds_offensive")
    val reboundsOffensive: Int = 0,
    
    @ColumnInfo(name = "rebounds_defensive")
    val reboundsDefensive: Int = 0,
    
    @ColumnInfo(name = "assists_total")
    val assistsTotal: Int = 0,
    
    @ColumnInfo(name = "steals_total")
    val stealsTotal: Int = 0,
    
    @ColumnInfo(name = "blocks_total")
    val blocksTotal: Int = 0,
    
    @ColumnInfo(name = "turnovers_total")
    val turnoversTotal: Int = 0,
    
    // Defensive Totals
    @ColumnInfo(name = "fouls_personal")
    val foulsPersonal: Int = 0,
    
    @ColumnInfo(name = "fouls_technical")
    val foulsTechnical: Int = 0,
    
    // Advanced Analytics
    @ColumnInfo(name = "plus_minus_total")
    val plusMinusTotal: Int = 0
) {
    // Computed averages
    val pointsPerGame: Double
        get() = if (gamesPlayed > 0) pointsTotal.toDouble() / gamesPlayed else 0.0
    val reboundsPerGame: Double
        get() = if (gamesPlayed > 0) totalRebounds.toDouble() / gamesPlayed else 0.0
    val assistsPerGame: Double
        get() = if (gamesPlayed > 0) assistsTotal.toDouble() / gamesPlayed else 0.0
    val fieldGoalPercentage: Double
        get() = if (fieldGoalsAttempted > 0) fieldGoalsMade.toDouble() / fieldGoalsAttempted else 0.0
    val threePointPercentage: Double
        get() = if (threePointersAttempted > 0) threePointersMade.toDouble() / threePointersAttempted else 0.0
    val freeThrowPercentage: Double
        get() = if (freeThrowsAttempted > 0) freeThrowsMade.toDouble() / freeThrowsAttempted else 0.0
    val totalRebounds: Int get() = reboundsOffensive + reboundsDefensive
    val minutesPerGame: Double
        get() = if (gamesPlayed > 0) totalMinutesPlayed.toDouble() / gamesPlayed else 0.0
}