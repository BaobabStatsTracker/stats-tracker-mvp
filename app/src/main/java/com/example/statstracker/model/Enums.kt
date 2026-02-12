package com.example.statstracker.model

/**
 * Player's dominant hand preference for shooting/playing.
 */
enum class PrimaryHand {
    LEFT,
    RIGHT
}

/**
 * Player's role on a team.
 */
enum class PlayerRole {
    STARTER,
    BENCH,
    COACH,
    OTHER
}

/**
 * Team designation in a game context.
 */
enum class GameTeamSide {
    HOME,
    AWAY
}

/**
 * Types of events that can occur during a basketball game.
 */
enum class GameEventType {
    TWO_POINTER_MADE,
    TWO_POINTER_MISSED,
    THREE_POINTER_MADE,
    THREE_POINTER_MISSED,
    FREE_THROW_MADE,
    FREE_THROW_MISSED,
    REBOUND,
    ASSIST,
    STEAL,
    BLOCK,
    TURNOVER,
    FOUL,
    SUBSTITUTION
}

/**
 * Defines how stats are tracked for a team in a game.
 */
enum class TrackingMode {
    BY_PLAYER,
    BY_TEAM
}

/**
 * Data class to represent event count query results.
 * Used by Room to map query results from event count queries.
 */
data class EventCount(
    val eventType: GameEventType,
    val count: Int
)