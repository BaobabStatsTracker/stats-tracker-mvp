package com.example.statstracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.Player
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLoggingSection(
    teamSide: GameTeamSide,
    teamName: String,
    players: List<Player>,
    canLogPlayerEvents: Boolean,
    onLogPlayerEvent: (Long, GameEventType, Int?) -> Unit,
    onLogTeamEvent: (GameEventType, Int?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Scoring Buttons
        item {
            ScoringSection(
                canLogPlayerEvents = canLogPlayerEvents,
                players = players,
                onLogPlayerEvent = onLogPlayerEvent,
                onLogTeamEvent = onLogTeamEvent
            )
        }

        // Other Events Section
        item {
            OtherEventsSection(
                canLogPlayerEvents = canLogPlayerEvents,
                players = players,
                onLogPlayerEvent = onLogPlayerEvent,
                onLogTeamEvent = onLogTeamEvent
            )
        }

        // Player-specific events (if tracking by player)
        if (canLogPlayerEvents && players.isNotEmpty()) {
            item {
                PlayerSpecificEventsSection(
                    players = players,
                    onLogPlayerEvent = onLogPlayerEvent
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoringSection(
    canLogPlayerEvents: Boolean,
    players: List<Player>,
    onLogPlayerEvent: (Long, GameEventType, Int?) -> Unit,
    onLogTeamEvent: (GameEventType, Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Scoring",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (canLogPlayerEvents && players.isNotEmpty()) {
                PlayerScoringGrid(
                    players = players,
                    onLogPlayerEvent = onLogPlayerEvent
                )
            } else {
                TeamScoringGrid(onLogTeamEvent = onLogTeamEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScoringGrid(
    players: List<Player>,
    onLogPlayerEvent: (Long, GameEventType, Int?) -> Unit
) {
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Player Selection
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedPlayer?.let { "${it.firstName} ${it.lastName}" } ?: "Select Player",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text("${player.firstName} ${player.lastName}") },
                    onClick = {
                        selectedPlayer = player
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Scoring Buttons
    if (selectedPlayer != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.TWO_POINTER_MADE, 2) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.Green.copy(alpha = 0.8f),
                    contentColor = Color.White
                )
            ) {
                Text("2PT ✓", fontWeight = FontWeight.Bold)
            }
            
            ElevatedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.THREE_POINTER_MADE, 3) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.Blue.copy(alpha = 0.8f),
                    contentColor = Color.White
                )
            ) {
                Text("3PT ✓", fontWeight = FontWeight.Bold)
            }
            
            ElevatedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.FREE_THROW_MADE, 1) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f),
                    contentColor = Color.White
                )
            ) {
                Text("FT ✓", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.TWO_POINTER_MISSED, null) },
                modifier = Modifier.weight(1f)
            ) {
                Text("2PT ✗")
            }
            
            OutlinedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.THREE_POINTER_MISSED, null) },
                modifier = Modifier.weight(1f)
            ) {
                Text("3PT ✗")
            }
            
            OutlinedButton(
                onClick = { onLogPlayerEvent(selectedPlayer!!.id, GameEventType.FREE_THROW_MISSED, null) },
                modifier = Modifier.weight(1f)
            ) {
                Text("FT ✗")
            }
        }
    }
}

@Composable
private fun TeamScoringGrid(
    onLogTeamEvent: (GameEventType, Int?) -> Unit
) {
    Text(
        text = "Team-level scoring tracking",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedButton(
            onClick = { onLogTeamEvent(GameEventType.TWO_POINTER_MADE, 2) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color.Green.copy(alpha = 0.8f),
                contentColor = Color.White
            )
        ) {
            Text("2PT ✓", fontWeight = FontWeight.Bold)
        }
        
        ElevatedButton(
            onClick = { onLogTeamEvent(GameEventType.THREE_POINTER_MADE, 3) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color.Blue.copy(alpha = 0.8f),
                contentColor = Color.White
            )
        ) {
            Text("3PT ✓", fontWeight = FontWeight.Bold)
        }
        
        ElevatedButton(
            onClick = { onLogTeamEvent(GameEventType.FREE_THROW_MADE, 1) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color.Red.copy(alpha = 0.8f),
                contentColor = Color.White
            )
        ) {
            Text("FT ✓", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onLogTeamEvent(GameEventType.TWO_POINTER_MISSED, null) },
            modifier = Modifier.weight(1f)
        ) {
            Text("2PT ✗")
        }
        
        OutlinedButton(
            onClick = { onLogTeamEvent(GameEventType.THREE_POINTER_MISSED, null) },
            modifier = Modifier.weight(1f)
        ) {
            Text("3PT ✗")
        }
        
        OutlinedButton(
            onClick = { onLogTeamEvent(GameEventType.FREE_THROW_MISSED, null) },
            modifier = Modifier.weight(1f)
        ) {
            Text("FT ✗")
        }
    }
}

@Composable
private fun OtherEventsSection(
    canLogPlayerEvents: Boolean,
    players: List<Player>,
    onLogPlayerEvent: (Long, GameEventType, Int?) -> Unit,
    onLogTeamEvent: (GameEventType, Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Other Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val eventTypes = listOf(
                GameEventType.REBOUND to "Rebound",
                GameEventType.ASSIST to "Assist",
                GameEventType.STEAL to "Steal",
                GameEventType.BLOCK to "Block",
                GameEventType.TURNOVER to "Turnover",
                GameEventType.FOUL to "Foul"
            )

            eventTypes.chunked(3).forEach { rowEvents ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowEvents.forEach { (eventType, displayName) ->
                        OutlinedButton(
                            onClick = { 
                                if (canLogPlayerEvents && players.isNotEmpty()) {
                                    // For now, log as team event - could be enhanced to allow player selection
                                    onLogTeamEvent(eventType, null)
                                } else {
                                    onLogTeamEvent(eventType, null)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = displayName, fontSize = 12.sp)
                        }
                    }
                    
                    // Fill remaining space if needed
                    repeat(3 - rowEvents.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                if (rowEvents != eventTypes.chunked(3).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSpecificEventsSection(
    players: List<Player>,
    onLogPlayerEvent: (Long, GameEventType, Int?) -> Unit
) {
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Player-Specific Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Player Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPlayer?.let { "${it.firstName} ${it.lastName}" } ?: "Select Player for Event",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    players.forEach { player ->
                        DropdownMenuItem(
                            text = { Text("${player.firstName} ${player.lastName}") },
                            onClick = {
                                selectedPlayer = player
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedPlayer != null) {
                Spacer(modifier = Modifier.height(12.dp))

                val eventTypes = listOf(
                    GameEventType.REBOUND to "Rebound",
                    GameEventType.ASSIST to "Assist",
                    GameEventType.STEAL to "Steal",
                    GameEventType.BLOCK to "Block",
                    GameEventType.TURNOVER to "Turnover",
                    GameEventType.FOUL to "Foul"
                )

                eventTypes.chunked(2).forEach { rowEvents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEvents.forEach { (eventType, displayName) ->
                            ElevatedButton(
                                onClick = { onLogPlayerEvent(selectedPlayer!!.id, eventType, null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = displayName)
                            }
                        }
                        
                        // Fill remaining space if needed
                        repeat(2 - rowEvents.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    
                    if (rowEvents != eventTypes.chunked(2).last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GameLogSection(
    events: List<GameEvent>,
    homeTeam: String,
    awayTeam: String,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    onDeleteEvent: (Long) -> Unit,
    formatTime: (Long) -> String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true, // Show most recent events first
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(events.reversed()) { event ->
            GameEventCard(
                event = event,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                homePlayers = homePlayers,
                awayPlayers = awayPlayers,
                onDeleteEvent = onDeleteEvent,
                formatTime = formatTime
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No events logged yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Start logging events using the team tabs above",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameEventCard(
    event: GameEvent,
    homeTeam: String,
    awayTeam: String,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    onDeleteEvent: (Long) -> Unit,
    formatTime: (Long) -> String
) {
    val teamName = if (event.team == GameTeamSide.HOME) homeTeam else awayTeam
    val players = if (event.team == GameTeamSide.HOME) homePlayers else awayPlayers
    val player = event.playerId?.let { playerId -> 
        players.find { it.id == playerId }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.team == GameTeamSide.HOME) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            }
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(event.timestamp.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = teamName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (player != null) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player.firstName} ${player.lastName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = formatEventType(event.eventType),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    event.pointsValue?.let { points ->
                        if (points > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+$points",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            IconButton(
                onClick = { onDeleteEvent(event.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete event",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatEventType(eventType: GameEventType): String {
    return when (eventType) {
        GameEventType.TWO_POINTER_MADE -> "2PT Made"
        GameEventType.TWO_POINTER_MISSED -> "2PT Missed"
        GameEventType.THREE_POINTER_MADE -> "3PT Made"
        GameEventType.THREE_POINTER_MISSED -> "3PT Missed"
        GameEventType.FREE_THROW_MADE -> "Free Throw Made"
        GameEventType.FREE_THROW_MISSED -> "Free Throw Missed"
        GameEventType.REBOUND -> "Rebound"
        GameEventType.ASSIST -> "Assist"
        GameEventType.STEAL -> "Steal"
        GameEventType.BLOCK -> "Block"
        GameEventType.TURNOVER -> "Turnover"
        GameEventType.FOUL -> "Foul"
        GameEventType.SUBSTITUTION -> "Substitution"
    }
}