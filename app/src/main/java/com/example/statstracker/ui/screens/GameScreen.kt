package com.example.statstracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.statstracker.ui.theme.LocalAppColors
import androidx.compose.ui.window.Dialog
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.GameStats
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.PlayerGameStats
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import com.example.statstracker.ui.viewmodel.GameDetailViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameId: Long,
    repository: BasketballRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { GameDetailViewModel(gameId, repository) }
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedPlayer by remember { mutableStateOf<Pair<Player, PlayerGameStats?>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Game Details",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
                
                uiState.gameWithDetails == null -> {
                    Text(
                        text = "Game not found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Game Info Header
                        GameInfoCard(
                            gameWithDetails = uiState.gameWithDetails!!,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        // Tab Row
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text(uiState.gameWithDetails!!.homeTeam.name) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text(uiState.gameWithDetails!!.awayTeam.name) }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                text = { Text("Game Events") }
                            )
                        }
                        
                        // Tab Content
                        when (selectedTabIndex) {
                            0 -> TeamTab(
                                teamName = uiState.gameWithDetails!!.homeTeam.name,
                                players = uiState.homePlayers,
                                playerStats = uiState.playerStats,
                                teamStats = uiState.teamStats.find { it.teamId == uiState.gameWithDetails!!.homeTeam.id },
                                onPlayerClick = { player, stats -> selectedPlayer = Pair(player, stats) }
                            )
                            1 -> TeamTab(
                                teamName = uiState.gameWithDetails!!.awayTeam.name,
                                players = uiState.awayPlayers,
                                playerStats = uiState.playerStats,
                                teamStats = uiState.teamStats.find { it.teamId == uiState.gameWithDetails!!.awayTeam.id },
                                onPlayerClick = { player, stats -> selectedPlayer = Pair(player, stats) }
                            )
                            2 -> GameEventsTab(
                                    events = uiState.gameWithDetails!!.events,
                                    homeTeamName = uiState.gameWithDetails!!.homeTeam.name,
                                    awayTeamName = uiState.gameWithDetails!!.awayTeam.name,
                                    homePlayers = uiState.homePlayers,
                                    awayPlayers = uiState.awayPlayers,
                                    homeTrackingMode = uiState.gameWithDetails!!.game.homeTrackingMode,
                                    awayTrackingMode = uiState.gameWithDetails!!.game.awayTrackingMode
                                )
                        }
                    }
                }
            }
        }
    }

    // Player Stats Dialog
    selectedPlayer?.let { (player, stats) ->
        PlayerStatsDialog(
            player = player,
            stats = stats,
            onDismiss = { selectedPlayer = null }
        )
    }
}

@Composable
private fun GameInfoCard(
    gameWithDetails: com.example.statstracker.database.relation.GameWithTeamsAndEvents,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Teams matchup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gameWithDetails.homeTeam.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "vs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                Text(
                    text = gameWithDetails.awayTeam.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Game details
            Text(
                text = "Date: ${gameWithDetails.game.date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            gameWithDetails.game.place?.let { place ->
                Text(
                    text = "Location: $place",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            gameWithDetails.game.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: $notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Events: ${gameWithDetails.events.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TeamTab(
    teamName: String,
    players: List<Player>,
    playerStats: List<PlayerGameStats>,
    teamStats: GameStats?,
    onPlayerClick: (Player, PlayerGameStats?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Team Stats Card
        teamStats?.let { stats ->
            item {
                TeamStatsCard(teamName = teamName, stats = stats)
            }
        }
        
        // Players Header
        item {
            Text(
                text = "Players",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Players List
        if (players.isEmpty()) {
            item {
                Text(
                    text = "No players found for this team",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(players) { player ->
                val stats = playerStats.find { it.playerId == player.id }
                PlayerCard(
                    player = player,
                    stats = stats,
                    onClick = { onPlayerClick(player, stats) }
                )
            }
        }
    }
}

@Composable
private fun TeamStatsCard(
    teamName: String,
    stats: GameStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$teamName Team Stats",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Points", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.points}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Field Goals", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.fieldGoalsMade}/${stats.fieldGoalsAttempted}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("3-Pointers", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.threePointersMade}/${stats.threePointersAttempted}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Free Throws", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.freeThrowsMade}/${stats.freeThrowsAttempted}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rebounds", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.totalRebounds}", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Assists", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.assists}", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    stats: PlayerGameStats?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${player.firstName} ${player.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (stats != null) {
                    val summary = buildString {
                        append("PTS: ${stats.points} | REB: ${stats.reboundsOffensive + stats.reboundsDefensive} | AST: ${stats.assists}")
                        if (stats.timePlayedSeconds > 0) {
                            val min = stats.timePlayedSeconds / 60
                            val sec = stats.timePlayedSeconds % 60
                            append(" | MIN: $min:${String.format("%02d", sec)}")
                        }
                    }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Click to view details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GameEventsTab(
    events: List<GameEvent>,
    homeTeamName: String,
    awayTeamName: String,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    homeTrackingMode: TrackingMode,
    awayTrackingMode: TrackingMode
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (events.isEmpty()) {
            item {
                Text(
                    text = "No events recorded for this game",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val sortedEvents = events.sortedBy { it.timestamp }

            items(sortedEvents) { event ->
                GameEventItem(
                    event = event,
                    homeTeamName = homeTeamName,
                    awayTeamName = awayTeamName,
                    homePlayers = homePlayers,
                    awayPlayers = awayPlayers,
                    homeTrackingMode = homeTrackingMode,
                    awayTrackingMode = awayTrackingMode
                )
            }
        }
    }
}

@Composable
private fun PlayerStatsDialog(
    player: Player,
    stats: PlayerGameStats?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${player.firstName} ${player.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (stats != null) {
                    // Player Stats
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Show if stats were calculated from events
                        if (stats.id == 0L) {
                            Text(
                                text = "Stats calculated from game events",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        StatRow("Points", "${stats.points}")
                        StatRow("Field Goals", "${stats.fieldGoalsMade}/${stats.fieldGoalsAttempted}")
                        StatRow("3-Pointers", "${stats.threePointersMade}/${stats.threePointersAttempted}")
                        StatRow("Free Throws", "${stats.freeThrowsMade}/${stats.freeThrowsAttempted}")
                        StatRow("Rebounds", "${stats.reboundsOffensive + stats.reboundsDefensive}")
                        StatRow("Offensive Reb", "${stats.reboundsOffensive}")
                        StatRow("Defensive Reb", "${stats.reboundsDefensive}")
                        StatRow("Assists", "${stats.assists}")
                        StatRow("Steals", "${stats.steals}")
                        StatRow("Blocks", "${stats.blocks}")
                        StatRow("Turnovers", "${stats.turnovers}")
                        StatRow("Personal Fouls", "${stats.foulsPersonal}")
                        if (stats.timePlayedSeconds > 0) {
                            StatRow("Time Played", "${stats.timePlayedSeconds / 60}:${String.format("%02d", stats.timePlayedSeconds % 60)}")
                        }
                    }
                } else {
                    Text(
                        text = "No statistics available for this player in this game. Make sure to log events for this player in the Game Dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun eventLabel(eventType: GameEventType): String = when (eventType) {
    GameEventType.TWO_POINTER_MADE -> "2 Points"
    GameEventType.TWO_POINTER_MISSED -> "2PT Missed"
    GameEventType.THREE_POINTER_MADE -> "3 Points"
    GameEventType.THREE_POINTER_MISSED -> "3PT Missed"
    GameEventType.FREE_THROW_MADE -> "Free Throw"
    GameEventType.FREE_THROW_MISSED -> "Free Throw Missed"
    GameEventType.REBOUND -> "Rebound"
    GameEventType.ASSIST -> "Assist"
    GameEventType.STEAL -> "Steal"
    GameEventType.BLOCK -> "Block"
    GameEventType.TURNOVER -> "Turnover"
    GameEventType.FOUL -> "Foul"
    GameEventType.SUBSTITUTION -> "Substitution"
}

@Composable
private fun GameEventItem(
    event: GameEvent,
    homeTeamName: String,
    awayTeamName: String,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    homeTrackingMode: TrackingMode,
    awayTrackingMode: TrackingMode
) {
    val isHome = event.team == GameTeamSide.HOME
    val teamName = if (isHome) homeTeamName else awayTeamName
    val trackingMode = if (isHome) homeTrackingMode else awayTrackingMode
    val players = if (isHome) homePlayers else awayPlayers
    val player = if (trackingMode == TrackingMode.BY_PLAYER) {
        event.playerId?.let { id -> players.find { it.id == id } }
    } else null

    val madeEvents = setOf(
        GameEventType.TWO_POINTER_MADE,
        GameEventType.THREE_POINTER_MADE,
        GameEventType.FREE_THROW_MADE
    )
    val missedEvents = setOf(
        GameEventType.TWO_POINTER_MISSED,
        GameEventType.THREE_POINTER_MISSED,
        GameEventType.FREE_THROW_MISSED
    )
    val eventIcon = when (event.eventType) {
        in madeEvents -> Icons.Default.CheckCircle to LocalAppColors.current.madeShot
        in missedEvents -> Icons.Default.Cancel to LocalAppColors.current.missedShot
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHome)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon column
            if (eventIcon != null) {
                Icon(
                    imageVector = eventIcon.first,
                    contentDescription = null,
                    tint = eventIcon.second,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 0.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Main content
            Column(modifier = Modifier.weight(1f)) {
                // Event label
                Text(
                    text = eventLabel(event.eventType),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                // Team name
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                // Player name (BY_PLAYER only)
                if (player != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${player.firstName} ${player.lastName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Timestamp
            Text(
                text = "${event.timestamp / 60}:${String.format("%02d", event.timestamp % 60)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}