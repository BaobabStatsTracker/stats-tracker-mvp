# Basketball Stats Database - Integration Guide

## Overview

This is a complete Room database implementation for basketball statistics tracking, built with:

- **Room 2.6+** with Kotlin coroutines
- **Jetpack Compose** integration
- **Repository pattern** for clean architecture
- **Dependency injection** ready (Hilt)
- **Flow-based reactive data**

## Database Structure

### Entities

- **Player**: Personal info, physical attributes, birth date
- **Team**: Team information and branding
- **TeamPlayer**: Join table linking players to teams with jersey numbers and roles
- **Game**: Game information with home/away teams and date
- **GameEvent**: Individual game events (shots, fouls, assists, etc.)

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

### 3. Type Safety

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

### 4. Date Handling

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
// Record game events
repository.insertGameEvent(GameEvent(
    gameId = gameId,
    playerId = playerId,
    team = GameTeamSide.HOME,
    timestamp = 300, // 5 minutes
    eventType = GameEventType.TWO_POINTER_MADE
))

// Get all events for a game
val events = repository.getEventsForGame(gameId)

// Get player performance in a specific game
val playerEvents = database.gameEventDao()
    .getEventsForPlayerInGame(gameId, playerId)
```

## Database Schema Notes

- **Primary Keys**: All entities use `Long` auto-generated primary keys
- **Foreign Keys**: Proper cascade delete relationships configured
- **Indexes**: Optimized for common query patterns
- **Type Converters**: Enums stored as TEXT, LocalDate as INTEGER (epoch days)
- **Null Safety**: Kotlin null-safety throughout, optional fields properly marked

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
