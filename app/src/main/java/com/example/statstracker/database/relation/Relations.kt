package com.example.statstracker.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.statstracker.database.entity.*

/**
 * Team with its associated players through the TeamPlayer join table.
 * Useful for displaying team rosters.
 */
data class TeamWithPlayers(
    @Embedded val team: Team,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            TeamPlayer::class,
            parentColumn = "team_id",
            entityColumn = "player_id"
        )
    )
    val players: List<Player>
)

/**
 * Player with their associated teams through the TeamPlayer join table.
 * Useful for showing which teams a player belongs to.
 */
data class PlayerWithTeams(
    @Embedded val player: Player,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            TeamPlayer::class,
            parentColumn = "player_id",
            entityColumn = "team_id"
        )
    )
    val teams: List<Team>
)

/**
 * Team with detailed player information including jersey numbers and roles.
 * More comprehensive than TeamWithPlayers.
 */
data class TeamWithDetailedPlayers(
    @Embedded val team: Team,
    @Relation(
        parentColumn = "id",
        entityColumn = "team_id"
    )
    val teamPlayers: List<TeamPlayer>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            TeamPlayer::class,
            parentColumn = "team_id",
            entityColumn = "player_id"
        )
    )
    val players: List<Player>
)

/**
 * Game with both home and away team information.
 * Essential for displaying game matchups.
 */
data class GameWithTeams(
    @Embedded val game: Game,
    @Relation(
        parentColumn = "home_team_id",
        entityColumn = "id"
    )
    val homeTeam: Team,
    @Relation(
        parentColumn = "away_team_id",
        entityColumn = "id"
    )
    val awayTeam: Team
)

/**
 * Game with all its events.
 * Useful for displaying game statistics and play-by-play.
 */
data class GameWithEvents(
    @Embedded val game: Game,
    @Relation(
        parentColumn = "id",
        entityColumn = "game_id"
    )
    val events: List<GameEvent>
)

/**
 * Complete game information including teams and events.
 * Comprehensive view for detailed game analysis.
 */
data class GameWithTeamsAndEvents(
    @Embedded val game: Game,
    @Relation(
        parentColumn = "home_team_id",
        entityColumn = "id"
    )
    val homeTeam: Team,
    @Relation(
        parentColumn = "away_team_id",
        entityColumn = "id"
    )
    val awayTeam: Team,
    @Relation(
        parentColumn = "id",
        entityColumn = "game_id"
    )
    val events: List<GameEvent>
)

/**
 * Player with all their game events across all games.
 * Useful for player statistics and career tracking.
 */
data class PlayerWithEvents(
    @Embedded val player: Player,
    @Relation(
        parentColumn = "id",
        entityColumn = "player_id"
    )
    val events: List<GameEvent>
)

/**
 * Game event with associated player and game information.
 * Useful for displaying event details with context.
 */
data class GameEventWithPlayerAndGame(
    @Embedded val event: GameEvent,
    @Relation(
        parentColumn = "player_id",
        entityColumn = "id"
    )
    val player: Player,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "id"
    )
    val game: Game
)