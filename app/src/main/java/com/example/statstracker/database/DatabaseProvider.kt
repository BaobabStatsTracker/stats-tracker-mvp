package com.example.statstracker.database

import android.content.Context

/**
 * Simple database provider for consistent access across the app.
 * Provides a singleton instance of BasketballDatabase.
 */
object DatabaseProvider {
    fun getInstance(context: Context): BasketballDatabase {
        return BasketballDatabase.getInstance(context)
    }
}