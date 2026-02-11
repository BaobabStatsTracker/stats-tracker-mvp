package com.example.statstracker.database.di

import android.content.Context
import com.example.statstracker.database.BasketballDatabase
import com.example.statstracker.database.dao.*
import com.example.statstracker.database.repository.BasketballRepository

/**
 * Simple database factory for manual dependency injection.
 * Use this instead of Hilt when you want to avoid additional dependencies.
 * 
 * Usage:
 * ```
 * val database = DatabaseFactory.createDatabase(context)
 * val repository = DatabaseFactory.createRepository(database)
 * ```
 */
object DatabaseFactory {
    
    fun createDatabase(context: Context): BasketballDatabase {
        return BasketballDatabase.getInstance(context)
    }
    
    fun createRepository(database: BasketballDatabase): BasketballRepository {
        return BasketballRepository(database)
    }
    
    fun createRepository(context: Context): BasketballRepository {
        val database = createDatabase(context)
        return BasketballRepository(database)
    }
}