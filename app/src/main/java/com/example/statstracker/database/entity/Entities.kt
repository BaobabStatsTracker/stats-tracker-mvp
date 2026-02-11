package com.example.statstracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.statstracker.database.GameEventType
import com.example.statstracker.database.GameTeamSide
import com.example.statstracker.database.PlayerRole
import com.example.statstracker.database.PrimaryHand
import java.time.LocalDate

// --- Player Entity ---

/**
 * Represents a basketball player with personal information and physical attributes.
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
    val notes: String? = null
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
    val playerId: Long,
    
    @ColumnInfo(name = "team")
    val team: GameTeamSide,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Int, // seconds from game start
    
    @ColumnInfo(name = "event_type")
    val eventType: GameEventType
)