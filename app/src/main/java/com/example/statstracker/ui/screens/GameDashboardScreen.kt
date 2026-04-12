package com.example.statstracker.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import com.example.statstracker.ui.components.DraggablePlayerIcon
import com.example.statstracker.ui.components.PlayerIcon
import com.example.statstracker.ui.components.PlayerIconPosition
import com.example.statstracker.ui.viewmodel.GameDashboardUiState
import com.example.statstracker.ui.viewmodel.GameDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDashboardScreen(
    gameId: Long,
    repository: BasketballRepository,
    onNavigateBack: () -> Unit
) {
    // Force landscape
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val viewModel = remember { GameDashboardViewModel(gameId, repository) }
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.game == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Game not found")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scoreboard Bar
        ScoreboardBar(
            uiState = uiState,
            onStartTimer = viewModel::startTimer,
            onPauseTimer = viewModel::pauseTimer,
            onResetTimer = viewModel::resetTimer,
            onEndQuarter = viewModel::endQuarter,
            onNavigateBack = onNavigateBack,
            formatTime = viewModel::formatTime
        )

        // Split Court Area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Home Side
            TeamHalf(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                side = GameTeamSide.HOME,
                teamName = uiState.homeTeam?.name ?: "Home",
                trackingMode = uiState.game!!.homeTrackingMode,
                players = uiState.homePlayers,
                onCourt = uiState.homeOnCourt,
                bench = uiState.homeBench,
                jerseys = uiState.homePlayerJerseys,
                accentColor = MaterialTheme.colorScheme.primary,
                score = uiState.homeScore,
                gameEvents = uiState.gameEvents,
                onPlayerClick = { viewModel.openPlayerEventModal(it, GameTeamSide.HOME) },
                onTeamClick = { viewModel.openTeamEventModal(GameTeamSide.HOME) },
                onSwap = { from, to -> viewModel.swapPlayers(GameTeamSide.HOME, from, to) }
            )

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Away Side
            TeamHalf(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                side = GameTeamSide.AWAY,
                teamName = uiState.awayTeam?.name ?: "Away",
                trackingMode = uiState.game!!.awayTrackingMode,
                players = uiState.awayPlayers,
                onCourt = uiState.awayOnCourt,
                bench = uiState.awayBench,
                jerseys = uiState.awayPlayerJerseys,
                accentColor = MaterialTheme.colorScheme.secondary,
                score = uiState.awayScore,
                gameEvents = uiState.gameEvents,
                onPlayerClick = { viewModel.openPlayerEventModal(it, GameTeamSide.AWAY) },
                onTeamClick = { viewModel.openTeamEventModal(GameTeamSide.AWAY) },
                onSwap = { from, to -> viewModel.swapPlayers(GameTeamSide.AWAY, from, to) }
            )
        }
    }

    // Event Modal
    if (uiState.showEventModal) {
        EventModal(
            uiState = uiState,
            onEventSelected = { eventType, points -> viewModel.logEventFromModal(eventType, points) },
            onDismiss = viewModel::dismissEventModal
        )
    }

    // End Quarter Dialog
    if (uiState.showEndQuarterDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndQuarterDialog,
            title = { Text("End Quarter") },
            text = {
                Text(
                    if (uiState.isInOvertime) "End OT${uiState.overtimeNumber}?"
                    else if (uiState.currentQuarter < 4) "End Quarter ${uiState.currentQuarter}?"
                    else "End the 4th quarter?"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEndQuarter) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEndQuarterDialog) { Text("Cancel") }
            }
        )
    }

    // End Game Dialog
    if (uiState.showEndGameDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndGameDialog,
            title = { Text("Game Status") },
            text = {
                Text(
                    if (uiState.homeScore == uiState.awayScore)
                        "Tied ${uiState.homeScore}-${uiState.awayScore}. Overtime or end?"
                    else "End the game or go to overtime?"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::goToOvertime) {
                    Text("OT${uiState.overtimeNumber + 1}")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::endGame) { Text("End Game") }
            }
        )
    }
}

@Composable
private fun ScoreboardBar(
    uiState: GameDashboardUiState,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onEndQuarter: () -> Unit,
    onNavigateBack: () -> Unit,
    formatTime: (Long) -> String
) {
    Surface(
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color.Black)
            }

            // Home team + score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uiState.homeTeam?.name ?: "Home",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = Color.Black
                )
                Text(
                    text = "${uiState.homeScore}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Timer + controls center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.5f)
            ) {
                Text(
                    text = if (uiState.isInOvertime) "OT${uiState.overtimeNumber}" else "Q${uiState.currentQuarter}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = formatTime(uiState.quarterTimeRemaining),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isTimerRunning) Color(0xFF4CAF50) else Color.Black
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start/Pause
                    FilledIconButton(
                        onClick = { if (uiState.isTimerRunning) onPauseTimer() else onStartTimer() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.isTimerRunning) Color(0xFFF44336) else Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            if (uiState.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                    // End Quarter
                    FilledIconButton(
                        onClick = onEndQuarter,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "End Quarter", modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                    // Reset
                    FilledIconButton(
                        onClick = onResetTimer,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }

            // Away team + score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uiState.awayTeam?.name ?: "Away",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = Color.Black
                )
                Text(
                    text = "${uiState.awayScore}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun TeamHalf(
    modifier: Modifier,
    uiState: GameDashboardUiState,
    side: GameTeamSide,
    teamName: String,
    trackingMode: TrackingMode,
    players: List<Player>,
    onCourt: List<Long>,
    bench: List<Long>,
    jerseys: Map<Long, Int>,
    accentColor: Color,
    score: Int,
    gameEvents: List<com.example.statstracker.database.entity.GameEvent>,
    onPlayerClick: (Long) -> Unit,
    onTeamClick: () -> Unit,
    onSwap: (Long, Long) -> Unit
) {
    if (trackingMode == TrackingMode.BY_PLAYER) {
        PlayerTrackingHalf(
            modifier = modifier,
            players = players,
            onCourt = onCourt,
            bench = bench,
            jerseys = jerseys,
            accentColor = accentColor,
            onPlayerClick = onPlayerClick,
            onSwap = onSwap
        )
    } else {
        TeamTrackingHalf(
            modifier = modifier,
            teamName = teamName,
            score = score,
            events = gameEvents.filter { it.team == side },
            accentColor = accentColor,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
private fun PlayerTrackingHalf(
    modifier: Modifier,
    players: List<Player>,
    onCourt: List<Long>,
    bench: List<Long>,
    jerseys: Map<Long, Int>,
    accentColor: Color,
    onPlayerClick: (Long) -> Unit,
    onSwap: (Long, Long) -> Unit
) {
    // Track positions of all player icons for drag-drop
    val positions = remember { mutableStateListOf<PlayerIconPosition>() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        // On-court players (5) — arranged in 2-1-2 formation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val courtPlayers = onCourt.mapNotNull { id -> players.find { it.id == id } }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                // Row 1: 2 players
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    courtPlayers.take(2).forEach { player ->
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInParent()
                                val center = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f
                                )
                                val existing = positions.indexOfFirst { it.playerId == player.id }
                                val entry = PlayerIconPosition(player.id, center)
                                if (existing >= 0) positions[existing] = entry
                                else positions.add(entry)
                            }
                        ) {
                            DraggablePlayerIcon(
                                playerId = player.id,
                                jerseyNumber = jerseys[player.id] ?: 0,
                                playerName = player.firstName,
                                isOnCourt = true,
                                accentColor = accentColor,
                                allPositions = positions.toList(),
                                onSwap = onSwap,
                                onClick = { onPlayerClick(player.id) }
                            )
                        }
                    }
                }

                // Row 2: 1 player (center)
                if (courtPlayers.size > 2) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val player = courtPlayers[2]
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInParent()
                                val center = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f
                                )
                                val existing = positions.indexOfFirst { it.playerId == player.id }
                                val entry = PlayerIconPosition(player.id, center)
                                if (existing >= 0) positions[existing] = entry
                                else positions.add(entry)
                            }
                        ) {
                            DraggablePlayerIcon(
                                playerId = player.id,
                                jerseyNumber = jerseys[player.id] ?: 0,
                                playerName = player.firstName,
                                isOnCourt = true,
                                accentColor = accentColor,
                                allPositions = positions.toList(),
                                onSwap = onSwap,
                                onClick = { onPlayerClick(player.id) }
                            )
                        }
                    }
                }

                // Row 3: 2 players
                if (courtPlayers.size > 3) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        courtPlayers.drop(3).take(2).forEach { player ->
                            Box(
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val pos = coords.positionInParent()
                                    val center = Offset(
                                        pos.x + coords.size.width / 2f,
                                        pos.y + coords.size.height / 2f
                                    )
                                    val existing = positions.indexOfFirst { it.playerId == player.id }
                                    val entry = PlayerIconPosition(player.id, center)
                                    if (existing >= 0) positions[existing] = entry
                                    else positions.add(entry)
                                }
                            ) {
                                DraggablePlayerIcon(
                                    playerId = player.id,
                                    jerseyNumber = jerseys[player.id] ?: 0,
                                    playerName = player.firstName,
                                    isOnCourt = true,
                                    accentColor = accentColor,
                                    allPositions = positions.toList(),
                                    onSwap = onSwap,
                                    onClick = { onPlayerClick(player.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bench row
        if (bench.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bench.forEach { pid ->
                    val player = players.find { it.id == pid } ?: return@forEach
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.positionInParent()
                            val center = Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                            val existing = positions.indexOfFirst { it.playerId == pid }
                            val entry = PlayerIconPosition(pid, center)
                            if (existing >= 0) positions[existing] = entry
                            else positions.add(entry)
                        }
                    ) {
                        DraggablePlayerIcon(
                            playerId = pid,
                            jerseyNumber = jerseys[pid] ?: 0,
                            playerName = player.firstName,
                            isOnCourt = false,
                            accentColor = accentColor.copy(alpha = 0.6f),
                            allPositions = positions.toList(),
                            onSwap = onSwap,
                            onClick = { onPlayerClick(pid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamTrackingHalf(
    modifier: Modifier,
    teamName: String,
    score: Int,
    events: List<com.example.statstracker.database.entity.GameEvent>,
    accentColor: Color,
    onTeamClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable { onTeamClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "$score pts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                val fgm = events.count { it.eventType == GameEventType.TWO_POINTER_MADE || it.eventType == GameEventType.THREE_POINTER_MADE }
                val fga = fgm + events.count { it.eventType == GameEventType.TWO_POINTER_MISSED || it.eventType == GameEventType.THREE_POINTER_MISSED }
                val reb = events.count { it.eventType == GameEventType.REBOUND }
                val ast = events.count { it.eventType == GameEventType.ASSIST }
                val to = events.count { it.eventType == GameEventType.TURNOVER }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("FG", "$fgm/$fga")
                    StatChip("REB", "$reb")
                    StatChip("AST", "$ast")
                    StatChip("TO", "$to")
                }

                Text(
                    text = "Tap to log event",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun EventModal(
    uiState: GameDashboardUiState,
    onEventSelected: (GameEventType, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val playerInfo = uiState.selectedPlayerForEvent?.let { (pid, side) ->
        val players = if (side == GameTeamSide.HOME) uiState.homePlayers else uiState.awayPlayers
        val jerseys = if (side == GameTeamSide.HOME) uiState.homePlayerJerseys else uiState.awayPlayerJerseys
        val player = players.find { it.id == pid }
        if (player != null) "#${jerseys[pid] ?: "?"} ${player.firstName} ${player.lastName}" else "Player"
    }
    val teamInfo = uiState.selectedTeamForEvent?.let { side ->
        if (side == GameTeamSide.HOME) uiState.homeTeam?.name ?: "Home" else uiState.awayTeam?.name ?: "Away"
    }
    val title = playerInfo ?: "$teamInfo (Team Event)"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scoring buttons
                Text("Scoring", style = MaterialTheme.typography.labelMedium, color = Color.Black.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventButton("2PT ✓", Color(0xFF4CAF50), Modifier.weight(1f)) {
                        onEventSelected(GameEventType.TWO_POINTER_MADE, 2)
                    }
                    EventButton("3PT ✓", Color(0xFF2196F3), Modifier.weight(1f)) {
                        onEventSelected(GameEventType.THREE_POINTER_MADE, 3)
                    }
                    EventButton("FT ✓", Color(0xFFFF9800), Modifier.weight(1f)) {
                        onEventSelected(GameEventType.FREE_THROW_MADE, 1)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedEventButton("2PT ✗", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.TWO_POINTER_MISSED, null)
                    }
                    OutlinedEventButton("3PT ✗", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.THREE_POINTER_MISSED, null)
                    }
                    OutlinedEventButton("FT ✗", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.FREE_THROW_MISSED, null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Other events
                Text("Other", style = MaterialTheme.typography.labelMedium, color = Color.Black.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedEventButton("Rebound", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.REBOUND, null)
                    }
                    OutlinedEventButton("Assist", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.ASSIST, null)
                    }
                    OutlinedEventButton("Steal", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.STEAL, null)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedEventButton("Block", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.BLOCK, null)
                    }
                    OutlinedEventButton("Turnover", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.TURNOVER, null)
                    }
                    OutlinedEventButton("Foul", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.FOUL, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun OutlinedEventButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}