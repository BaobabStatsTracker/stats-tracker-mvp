# Basketball Stats Database - Integration Guide

## Overview

This is a complete Room database implementation for basketball statistics tracking, built with:

- **Room 2.6+** with Kotlin coroutines
- **Jetpack Compose** integration
- **Repository pattern** for clean architecture
- **Dependency injection** ready (Hilt)
- **Flow-based reactive data**

## Database Structure

### Core Entities

#### Player

- Personal information (name, height, wingspan, primary hand)
- Physical attributes and birth date
- Notes and optional image

#### Team

- Team metadata (name, logo, notes)
- Supports multiple teams per player via TeamPlayer relationship

#### TeamPlayer (Join Table)

- Links players to teams with additional context
- Jersey numbers and player roles (starter, bench, coach)
- Supports many-to-many relationships

#### Game

- Game information with home/away teams
- Date, location, and notes
- Tracking modes per team (by player vs. by team)

#### GameEvent (Enhanced)

- Individual events during games
- Enhanced with location tracking (X/Y coordinates)
- Shot distance, result, and assist tracking
- Foul types and point values
- Support for advanced analytics

### Statistics Entities

#### GameStats

- Aggregated statistics at game/team level
- Full game and quarter-by-quarter breakdown
- Comprehensive scoring, possession, and defensive stats
- Computed shooting percentages

#### PlayerGameStats

- Individual player performance per game
- Same statistical categories as GameStats
- Plus/minus tracking and playing time
- Shot chart data support (JSON)

#### PlayerSeasonStats

- Season-long aggregated statistics
- Games played/started and total minutes
- Career tracking across multiple seasons
- Computed averages and percentages

### Entities

- **Player**: Personal info, physical attributes, birth date
- **Team**: Team information and branding
- **TeamPlayer**: Join table linking players to teams with jersey numbers and roles
- **Game**: Game information with home/away teams and date
- **GameEvent**: Individual game events (shots, fouls, assists, etc.) with enhanced tracking
- **GameStats**: Aggregated game-level statistics by team and quarter
- **PlayerGameStats**: Individual player performance statistics per game
- **PlayerSeasonStats**: Aggregated player statistics across seasons

### Enums

- **PrimaryHand**: LEFT, RIGHT
- **PlayerRole**: STARTER, BENCH, COACH, OTHER
- **GameTeamSide**: HOME, AWAY
- **GameEventType**: Various basketball events (shots, fouls, etc.)

## Required Dependencies

Add these to your `build.gradle.kts (Module: app)`:

```kotlin
dependencies {
    // Room (already added to your project)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // For dependency injection (optional but recommended)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // For ViewModel integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}

// Also add this plugin at the top of your build.gradle.kts
plugins {
    // ... existing plugins
    id("dagger.hilt.android.plugin")
}
```

And add this to your project-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

## Setup Instructions

### 1. Application Class (if using Hilt)

Create or modify your Application class:

```kotlin
@HiltAndroidApp
class StatsTrackerApplication : Application()
```

Update your `AndroidManifest.xml`:

```xml
<application
    android:name=".StatsTrackerApplication"
    ... >
```

### 2. Basic Usage (without Hilt)

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var database: BasketballDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database
        database = BasketballDatabase.getInstance(this)

        // Use database in composable
        setContent {
            StatsTrackerTheme {
                MyBasketballApp(database)
            }
        }
    }
}
```

### 3. Using with Repository Pattern

```kotlin
@Composable
fun MyBasketballApp(database: BasketballDatabase) {
    val repository = BasketballRepository(database)
    val viewModel = PlayersViewModel(repository)

    PlayersScreen(viewModel = viewModel)
}
```

## Key Features

### 1. Reactive Data with Flow

All DAOs provide Flow-based methods for reactive UI updates:

```kotlin
// Observe teams with automatic UI updates
val teams by repository.getAllTeamsFlow().collectAsState(initial = emptyList())

// Observe game statistics in real-time
val gameStats by repository.getGameOverallStatsFlow(gameId).collectAsState(initial = null)
```

### 2. Complex Relationships

Pre-built relationship queries for common use cases:

```kotlin
// Get team with all its players
val teamWithPlayers = repository.getTeamWithPlayers(teamId)

// Get game with teams and all events
val gameDetails = repository.getGameWithTeamsAndEvents(gameId)

// Get player with all their stats across all games
val playerStats = repository.getPlayerWithEvents(playerId)
```

### 3. Comprehensive Statistics

Multi-level statistics tracking:

```kotlin
// Game-level statistics
val gameStats = repository.getGameOverallStats(gameId)
val teamStats = repository.getTeamGameStats(gameId, teamId)

// Player performance per game
val playerGameStats = repository.getPlayerGameStats(gameId, playerId)

// Season-long player statistics
val seasonStats = repository.getPlayerSeasonStats(playerId, year)
val careerStats = repository.getPlayerCareerStats(playerId)
```

### 4. Enhanced Event Tracking

Advanced game event recording:

```kotlin
val event = GameEvent(
    gameId = 1L,
    playerId = 1L,
    team = GameTeamSide.HOME,
    timestamp = 120, // 2 minutes into game
    eventType = GameEventType.THREE_POINTER_MADE,
    locationX = 0.75, // Court X coordinate
    locationY = 0.23, // Court Y coordinate
    shotDistance = 23.5, // Distance in feet
    shotResult = "made",
    pointsValue = 3
)
```

### 5. Type Safety

Strongly typed enums with automatic conversion:

```kotlin
val event = GameEvent(
    gameId = 1L,
    playerId = 1L,
    team = GameTeamSide.HOME,
    timestamp = 120, // 2 minutes into game
    eventType = GameEventType.THREE_POINTER_MADE
)
```

### 6. Date Handling

LocalDate support with efficient storage:

```kotlin
val game = Game(
    homeTeamId = 1L,
    awayTeamId = 2L,
    date = LocalDate.now(),
    place = "Madison Square Garden"
)
```

## Example Queries

### Basic Operations

```kotlin
// Add a player
val playerId = repository.insertPlayer(
    Player(firstName = "LeBron", lastName = "James", heightCm = 206)
)

// Create a team
val teamId = repository.insertTeam(
    Team(name = "Lakers", logo = "https://example.com/logo.png")
)

// Add player to team
repository.addPlayerToTeam(
    playerId = playerId,
    teamId = teamId,
    jerseyNum = 23,
    role = PlayerRole.STARTER
)
```

### Game Statistics

```kotlin
// Record enhanced game events
repository.insertGameEvent(GameEvent(
    gameId = gameId,
    playerId = playerId,
    team = GameTeamSide.HOME,
    timestamp = 300, // 5 minutes
    eventType = GameEventType.TWO_POINTER_MADE,
    locationX = 0.45,
    locationY = 0.32,
    shotDistance = 15.2,
    shotResult = "made",
    pointsValue = 2
))

// Get team statistics for a game
val teamStats = repository.getTeamGameStats(gameId, teamId)
val fieldGoalPercentage = teamStats?.fieldGoalPercentage

// Get player's performance in a game
val playerStats = repository.getPlayerGameStats(gameId, playerId)
val playerPoints = playerStats?.points

// Get all players' stats for a game
val allPlayerStats = repository.getAllPlayerStatsForGame(gameId)
```

### Season and Career Statistics

```kotlin
// Get player's season statistics
val seasonStats = repository.getPlayerSeasonStats(playerId, 2024)
val pointsPerGame = seasonStats?.pointsPerGame
val fieldGoalPct = seasonStats?.fieldGoalPercentage

// Get player's career statistics
val careerStats = repository.getPlayerCareerStats(playerId)
careerStats.forEach { season ->
    println("${season.seasonYear}: ${season.pointsPerGame} PPG")
}

// Get team roster statistics for a season
val teamSeasonStats = repository.getTeamSeasonStats(teamId, 2024)
teamSeasonStats.sortedByDescending { it.pointsPerGame }.forEach { player ->
    println("${player.pointsPerGame} PPG")
}

// Get available seasons in the database
val seasons = repository.getAvailableSeasons()
```

### Advanced Analytics

```kotlin
// Get top performers from DAO directly
val topScorers = database.playerSeasonStatsDao().getTopScorers(2024, 10)
val topRebounders = database.playerSeasonStatsDao().getTopRebounders(2024, 10)
val bestShooters = database.playerSeasonStatsDao()
    .getTopFieldGoalPercentage(2024, minAttempts = 100, limit = 10)

// Quarter-by-quarter analysis
val quarterStats = database.gameStatsDao().getTeamQuarterStats(gameId, teamId)
quarterStats.forEach { quarter ->
    println("Q${quarter.quarter}: ${quarter.points} points")
}
```

## Database Schema Notes

- **Primary Keys**: All entities use `Long` auto-generated primary keys
- **Foreign Keys**: Proper cascade delete relationships configured
- **Indexes**: Optimized for common query patterns including statistics lookups
- **Type Converters**: Enums stored as TEXT, LocalDate as INTEGER (epoch days)
- **Null Safety**: Kotlin null-safety throughout, optional fields properly marked
- **Statistics**: Multi-level aggregation with computed properties for percentages
- **Unique Constraints**: Prevent duplicate statistics for same game/player/quarter combinations
- **Enhanced Events**: GameEvent table includes shot tracking and advanced metrics

## Testing

The database is ready for testing with:

- In-memory database for unit tests
- Transaction-based operations for data integrity
- Suspending functions for easy coroutine testing

```kotlin
@Test
fun testPlayerInsertion() = runTest {
    val player = Player(firstName = "Test", lastName = "Player")
    val id = playerDao.insert(player)
    val retrievedPlayer = playerDao.getPlayerById(id)

    assertEquals("Test", retrievedPlayer?.firstName)
}
```

## Performance Considerations

- Use `Flow` methods for UI-bound data (automatic updates)
- Use regular `suspend` methods for one-time operations
- Leverage relationship queries instead of multiple separate queries
- Consider pagination for large datasets
- Use transactions for bulk operations

## Migration Strategy

When you need to modify the database schema:

1. Increment version number in `@Database`
2. Add migration strategies to `Room.databaseBuilder()`
3. Test migrations thoroughly

This database is production-ready and follows Android best practices for local data storage in basketball statistics applications.
