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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.Player
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.ui.theme.LocalAppColors

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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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
                                    color = LocalAppColors.current.positivePoints,
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
        GameEventType.OFFENSIVE_REBOUND -> "Off Rebound"
        GameEventType.DEFENSIVE_REBOUND -> "Def Rebound"
        GameEventType.ASSIST -> "Assist"
        GameEventType.STEAL -> "Steal"
        GameEventType.BLOCK -> "Block"
        GameEventType.TURNOVER -> "Turnover"
        GameEventType.FOUL -> "Foul"
        GameEventType.SUBSTITUTION -> "Substitution"
    }
}