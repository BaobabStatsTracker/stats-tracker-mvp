package com.example.statstracker.database

import androidx.room.TypeConverter
import java.time.LocalDate

// --- Enums ---

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

// --- Type Converters ---

/**
 * Type converters for Room database to handle custom types.
 * Converts enums to/from String and LocalDate to/from epoch days (INTEGER).
 */
class Converters {
    
    // PrimaryHand converters
    @TypeConverter
    fun fromPrimaryHand(hand: PrimaryHand?): String? {
        return hand?.name
    }
    
    @TypeConverter
    fun toPrimaryHand(handString: String?): PrimaryHand? {
        return handString?.let { PrimaryHand.valueOf(it) }
    }
    
    // PlayerRole converters
    @TypeConverter
    fun fromPlayerRole(role: PlayerRole?): String? {
        return role?.name
    }
    
    @TypeConverter
    fun toPlayerRole(roleString: String?): PlayerRole? {
        return roleString?.let { PlayerRole.valueOf(it) }
    }
    
    // GameTeamSide converters
    @TypeConverter
    fun fromGameTeamSide(side: GameTeamSide): String {
        return side.name
    }
    
    @TypeConverter
    fun toGameTeamSide(sideString: String): GameTeamSide {
        return GameTeamSide.valueOf(sideString)
    }
    
    // GameEventType converters
    @TypeConverter
    fun fromGameEventType(eventType: GameEventType): String {
        return eventType.name
    }
    
    @TypeConverter
    fun toGameEventType(eventTypeString: String): GameEventType {
        return GameEventType.valueOf(eventTypeString)
    }
    
    // LocalDate converters - stored as epoch days (INTEGER)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
    
    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}