package com.example.statstracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.statstracker.database.dao.*
import com.example.statstracker.database.entity.*

/**
 * Main Room database for the Basketball Stats Tracker app.
 * Manages all basketball-related data including players, teams, games, and events.
 */
@Database(
    entities = [
        Player::class,
        Team::class,
        TeamPlayer::class,
        Game::class,
        GameEvent::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BasketballDatabase : RoomDatabase() {
    
    abstract fun playerDao(): PlayerDao
    abstract fun teamDao(): TeamDao
    abstract fun teamPlayerDao(): TeamPlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gameEventDao(): GameEventDao
    abstract fun relationDao(): RelationDao
    
    companion object {
        @Volatile
        private var INSTANCE: BasketballDatabase? = null
        
        /**
         * Returns the singleton instance of the database.
         * Thread-safe implementation using double-checked locking pattern.
         */
        fun getInstance(context: Context): BasketballDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BasketballDatabase::class.java,
                    "basketball_stats.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}