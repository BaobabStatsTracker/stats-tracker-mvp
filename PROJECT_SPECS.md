```markdown
# EasyBuckets – Project Specification

> Internal spec for GitHub Copilot and contributors.  
> This file describes the scope, architecture, and constraints of the EasyBuckets Android app.

---

## 1. Project Overview

**Name:** EasyBuckets  
**Platform:** Android (phone + tablet)  
**Tech stack (target):**

- UI: Jetpack Compose + Material 3
- Language: Kotlin
- Local persistence: Room (SQLite)
- Sync: REST API client (suspending HTTP client, e.g. Retrofit/OkHttp)
- Concurrency: Kotlin coroutines + Flows
- DI (optional but recommended): Hilt or Koin

**Goal:**  
EasyBuckets is a basketball statistics tracking app that allows users to record game stats for teams and players, and to view advanced insights about team performance and individual player performance. Data is stored locally first and can be synchronized to an external API backing an existing web application.

---

## 2. Core Use Cases

### 2.1 Game and roster management

- Create, edit, delete teams.
- Create, edit, delete players.
- Assign players to one or more teams (many-to-many).
- Create, edit, delete games between two teams (home vs away).
- For a given game, define:
  - Home team
  - Away team
  - Date and time
  - Location
  - Notes

### 2.2 In-game stat tracking

- Select a game and open a “Live Stats” or “Scorekeeper” screen.
- Record events during the game for:
  - **Per-player stats**: shots, rebounds, assists, steals, blocks, fouls, turnovers, etc.
  - **Per-team stats only** (no player breakdown) when user prefers a simpler mode.
- The user can choose for each game:
  - Track home team by player and away team by team only.
  - Track both teams by player.
  - Track both teams by team only.
- Support basic timer mode (optional at first version): the app can store a “timestamp” (seconds from game start) for each event.

### 2.3 Stats and insights

- For a single game:
  - Show box score by team and by player (if tracked per player).
  - Show shooting percentages, points, rebounds, assists, etc.
- Across games:
  - Show per-player aggregates (e.g., averages per game, total points, shooting splits).
  - Show team aggregates (e.g., offensive/defensive rating, win–loss record, scoring distribution).
- These insights can be progressively implemented; initial version can focus on:
  - Per-game box score
  - Simple per-player and per-team totals and averages

### 2.4 Data sync

- Local data is the source of truth.
- Provide a manual or scheduled synchronization feature to:
  - Push local teams, players, games, and events to the remote API.
  - Receive confirmation or updated remote IDs.
- Handle offline-first behavior:
  - The app must be fully usable offline.
  - Sync should be tolerant of temporary network failures and retry safely.

---

## 3. Data Model (ER Diagram – Extended)

> This section describes the conceptual data model. Room entities will reflect this structure.

### 3.1 Player

Represents a single basketball player.

- `id` (PK, Long, auto-generated)
- `first_name` (String, non-null)
- `last_name` (String, non-null)
- `image` (String?, URL/path)
- `height_cm` (Int?)
- `wingspan_cm` (Int?)
- `primary_hand` (enum `PrimaryHand?` – LEFT, RIGHT)
- `date_of_birth` (LocalDate?)
- `notes` (String?)
- Optional extensions:
  - `number_preferred` (Int?) – generic jersey number preference
  - `external_id` (String?) – remote identifier for sync

### 3.2 Team

Represents a team (club, pickup team, etc.).

- `id` (PK, Long, auto-generated)
- `name` (String, non-null)
- `logo` (String?, URL/path)
- `notes` (String?)
- Optional extensions:
  - `external_id` (String?) – remote identifier for sync
  - `color_primary` (String?) – hex color for UI styling
  - `color_secondary` (String?)

### 3.3 TeamPlayer (join table)

Many-to-many relation between players and teams, with per-team metadata.

- `id` (PK, Long, auto-generated)
- `player_id` (FK → Player.id)
- `team_id` (FK → Team.id)
- `jersey_num` (Int?)
- `role` (enum `PlayerRole?` – STARTER, BENCH, COACH, OTHER)
- Optional extensions:
  - `is_active` (Boolean, default true) – mark inactive without deletion

### 3.4 Game

Represents a single game between two teams.

- `id` (PK, Long, auto-generated)
- `home_team_id` (FK → Team.id)
- `away_team_id` (FK → Team.id)
- `date` (LocalDate, non-null)
- `place` (String?)
- `notes` (String?)
- Required tracking mode fields:
  - `home_tracking_mode` (enum `TrackingMode` – BY_PLAYER, BY_TEAM)
  - `away_tracking_mode` (enum `TrackingMode` – BY_PLAYER, BY_TEAM)
- Optional extensions:
  - `final_score_home` (Int?)
  - `final_score_away` (Int?)
  - `external_id` (String?) – remote identifier for sync
  - `is_synced` (Boolean, default false)
  - `last_synced_at` (Instant?/Long?) – timestamp of last successful sync

### 3.5 GameEvent

Represents a single stat event during a game.

- `id` (PK, Long, auto-generated)
- `game_id` (FK → Game.id)
- `player_id` (FK → Player.id, nullable for team-only events)
- `team` (enum `GameTeamSide` – HOME, AWAY)
- `timestamp` (Int, non-null, seconds from game start; can be 0 if not used)
- `event_type` (enum `GameEventType`, non-null)
  - Examples:
    - TWO_POINTER_MADE, TWO_POINTER_MISSED
    - THREE_POINTER_MADE, THREE_POINTER_MISSED
    - FREE_THROW_MADE, FREE_THROW_MISSED
    - REBOUND, ASSIST, STEAL, BLOCK
    - TURNOVER, FOUL, SUBSTITUTION, OTHER
- Optional extensions:
  - `period` (Int?) – quarter/period number
  - `meta_json` (String?) – flexible JSON field for future details (e.g., shot location)

### 3.6 Sync metadata (optional, internal)

If needed, we can add a generic sync metadata table instead of `external_id` on each entity:

- `SyncRecord`
  - `id` (PK)
  - `entity_type` (String, e.g., "PLAYER", "TEAM", "GAME", "GAME_EVENT")
  - `entity_local_id` (Long)
  - `remote_id` (String)
  - `last_synced_at` (Long)
  - `sync_status` (enum – PENDING, SUCCESS, ERROR)

For now, using `external_id` and status fields on the main entities is sufficient.

---

## 4. Application Architecture

### 4.1 Layers

- **UI layer (Compose)**
  - Screens and components:
    - Team list / Team detail / Edit team
    - Player list / Player detail / Edit player
    - Game list / Game detail
    - Live stats screen (event recording)
    - Stats/analytics screen (per game, per player, per team)
- **State & business logic**
  - ViewModels handle:
    - Loading data via repositories.
    - Exposing UI state as `StateFlow` / `LiveData`.
    - Handling user actions (CRUD, record event, start sync).
- **Data layer**
  - Repositories that mediate between:
    - Room DAOs (local database)
    - Remote API client (for sync)
  - Implement offline-first logic and conflict resolution as needed.

### 4.2 Local database (Room)

- Room database `BasketballDatabase` with entities:
  - Player
  - Team
  - TeamPlayer
  - Game
  - GameEvent
- Use type converters for:
  - Enums (`PrimaryHand`, `PlayerRole`, `GameTeamSide`, `GameEventType`, `TrackingMode`)
  - `LocalDate` → persisted as ISO string or epoch day
- DAOs expose:
  - CRUD for each entity.
  - Relationship queries:
    - Team with players.
    - Player with teams.
    - Game with teams.
    - Game with events.

### 4.3 Sync and networking

- Implement a repository that knows how to:
  - Collect unsynced entities.
  - Map them to the remote API payloads.
  - Send them via HTTP (e.g., Retrofit).
  - Update local sync state on success.
- Sync can be triggered:
  - Manually by the user (e.g., “Sync now” button).
  - Automatically on app start or periodically (optional).

### 4.4 UI responsiveness (phone & tablet)

- Use responsive Compose layouts:
  - For narrow screens (phones):
    - Single-column navigation: list → detail.
  - For wider screens (tablets):
    - Two-pane or multi-pane layouts:
      - E.g., teams/games list on the left, details or live stats on the right.
- Avoid hard-coded sizes; use `Modifier.fillMaxSize()`, `weight`, etc.
- Consider using window size classes or breakpoints to adapt layout.

---

## 5. Screens (High-Level Specification)

### 5.1 Teams

**Teams List Screen**

- Displays all teams with:
  - Name
  - Logo (if available)
- Actions:
  - Add new team
  - Tap team → open Team Detail screen

**Team Detail Screen**

- Show team info:
  - Name, logo, notes
- Show roster:
  - List of players (via TeamPlayer join)
- Actions:
  - Edit team
  - Add/remove players from team
  - View games where this team participated

### 5.2 Players

**Players List Screen**

- Displays all players with:
  - Full name
  - Optionally key stats (e.g., PPG) returned from aggregates
- Actions:
  - Add new player
  - Tap player → Player Detail

**Player Detail Screen**

- Show bio info:
  - Name, photo, height, wingspan, hand, notes
- Show attached teams (TeamPlayer records)
- Show key stats over time (later iterations):
  - Points/game, rebounds/game, etc.
- Actions:
  - Edit player
  - Remove from team(s) (via TeamPlayer)
  - Archive/delete player (soft delete optional)

### 5.3 Games

**Games List Screen**

- List games with:
  - Date, home team, away team, score (if finished)
- Filters:
  - By team
  - By date
- Actions:
  - Add new game
  - Tap → Game Detail or Live Stats

**Game Detail Screen**

- Show:
  - Teams
  - Date, place
  - Game status (planned, in progress, finished)
- Show:
  - Box score by team and player
- Actions:
  - Start/continue Live Stats recording
  - Edit game info
  - View advanced analytics (later)

### 5.4 Live Stats (Scorekeeper)

**Live Stats Screen**

- Layout should support quick interaction (especially on tablets):
  - Show home and away scores.
  - Show player list for home team (if tracking by player).
  - For away team:
    - List of players if tracking by player OR a simplified interface if tracking by team only.
- Actions:
  - For home team player:
    - Tap player, then choose event type (e.g., 2PT made/missed, rebound).
  - For team-only tracking:
    - Tap team side, then event type.
  - Undo last event (important for usability).
- The screen writes `GameEvent` records in the database with:
  - Correct `game_id`
  - `team` = HOME/AWAY
  - `player_id` = selected player or null
  - `timestamp` if available

---

## 6. Non-Functional Requirements

- **Performance:**
  - Efficient list rendering with `LazyColumn` and derived stats computed on-demand or cached.
- **Offline-first:**
  - All CRUD and stat viewing fully functional without network.
- **Data integrity:**
  - Enforce foreign keys in Room.
  - Use transactions when creating/deleting complex relationships (e.g., game with events).
- **Testability:**
  - Repositories and ViewModels designed for unit testing (interfaces, dependency injection).
- **Extensibility:**
  - The data model should allow:
    - New event types.
    - Additional per-game or per-player metrics.

---

## 7. Guidance for GitHub Copilot

When generating code for this repository, follow these rules:

1. **Respect the data model:**
   - Use the entities and enums described in this spec.
   - Use Room with proper annotations and type converters.

2. **Architecture:**
   - Prefer a clean separation of concerns:
     - UI (Compose) → ViewModel → Repository → DAO/API.
   - Use coroutines and Flows for async and reactive streams.

3. **UI:**
   - Use Jetpack Compose + Material 3 components.
   - Design for both phone and tablet; avoid fixed pixel sizes.

4. **Sync:**
   - Implement sync logic in repositories, not directly in the UI.
   - Do not block the main thread during network or disk operations.

5. **Code style:**
   - Idiomatic Kotlin.
   - Nullability aligned with the spec (nullable fields only when they can actually be missing).
   - Use sealed classes or data classes for UI state where helpful.

This spec file is the single source of truth for the scope and architecture of the EasyBuckets app.  
If there is a conflict between existing code and this spec, prefer updating the code to match the spec (or propose updates to the spec explicitly).
```
