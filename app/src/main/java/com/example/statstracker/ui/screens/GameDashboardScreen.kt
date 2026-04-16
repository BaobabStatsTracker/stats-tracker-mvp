package com.example.statstracker.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.statstracker.ui.theme.LocalAppColors
import kotlin.math.roundToInt
import com.example.statstracker.database.entity.GameEvent
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameEventType
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import com.example.statstracker.ui.components.DragDropState
import com.example.statstracker.ui.components.DraggablePlayerIcon
import com.example.statstracker.ui.components.PlayerIcon
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

    Column(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Scoreboard Bar
        ScoreboardBar(
            uiState = uiState,
            onStartTimer = viewModel::startTimer,
            onPauseTimer = viewModel::pauseTimer,
            onEndQuarter = viewModel::endQuarter,
            onNavigateBack = onNavigateBack,
            onOpenEventsReview = viewModel::openEventsReview,
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

    // Events Review Dialog
    if (uiState.showEventsReview) {
        EventsReviewDialog(
            uiState = uiState,
            onClose = viewModel::closeEventsReview,
            onRequestDelete = viewModel::requestDeleteEvent
        )
    }

    // Delete Event Confirmation
    if (uiState.eventPendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteEvent,
            title = { Text("Delete Event") },
            text = { Text("Remove this logged event? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteEvent) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteEvent) { Text("Cancel") }
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
    onEndQuarter: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenEventsReview: () -> Unit,
    formatTime: (Long) -> String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                    color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(uiState.quarterTimeRemaining),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isTimerRunning) LocalAppColors.current.timerRunning else MaterialTheme.colorScheme.onSurface
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
                            containerColor = if (uiState.isTimerRunning) LocalAppColors.current.timerPaused else LocalAppColors.current.timerRunning
                        )
                    ) {
                        Icon(
                            if (uiState.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
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
                        Icon(Icons.Default.Stop, contentDescription = "End Quarter", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }

            // Review events button
            IconButton(onClick = onOpenEventsReview, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.List, contentDescription = "Review Events", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                    color = MaterialTheme.colorScheme.onSurface
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
    // Shared drag state — all court + bench icons register their window-coordinate
    // centers here so cross-container (court ↔ bench) drag-and-drop works correctly.
    val dragDropState = remember { DragDropState() }

    // Window-coordinate origin of this Box, used to convert the ghost's
    // window-space position into a local offset for rendering.
    var containerWindowOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                containerWindowOffset = Offset(
                    coords.positionInWindow().x,
                    coords.positionInWindow().y
                )
            }
    ) {
        // --- Main layout: court formation + bench row ---
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            DraggablePlayerIcon(
                                playerId = player.id,
                                jerseyNumber = jerseys[player.id] ?: 0,
                                playerName = player.firstName,
                                isOnCourt = true,
                                accentColor = accentColor,
                                dragDropState = dragDropState,
                                onSwap = onSwap,
                                onClick = { onPlayerClick(player.id) }
                            )
                        }
                    }

                    // Row 2: 1 player (center)
                    if (courtPlayers.size > 2) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val player = courtPlayers[2]
                            DraggablePlayerIcon(
                                playerId = player.id,
                                jerseyNumber = jerseys[player.id] ?: 0,
                                playerName = player.firstName,
                                isOnCourt = true,
                                accentColor = accentColor,
                                dragDropState = dragDropState,
                                onSwap = onSwap,
                                onClick = { onPlayerClick(player.id) }
                            )
                        }
                    }

                    // Row 3: 2 players
                    if (courtPlayers.size > 3) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            courtPlayers.drop(3).take(2).forEach { player ->
                                DraggablePlayerIcon(
                                    playerId = player.id,
                                    jerseyNumber = jerseys[player.id] ?: 0,
                                    playerName = player.firstName,
                                    isOnCourt = true,
                                    accentColor = accentColor,
                                    dragDropState = dragDropState,
                                    onSwap = onSwap,
                                    onClick = { onPlayerClick(player.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Bench row
            if (bench.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bench.forEach { pid ->
                        val player = players.find { it.id == pid } ?: return@forEach
                        DraggablePlayerIcon(
                            playerId = pid,
                            jerseyNumber = jerseys[pid] ?: 0,
                            playerName = player.firstName,
                            isOnCourt = false,
                            accentColor = accentColor.copy(alpha = 0.6f),
                            dragDropState = dragDropState,
                            onSwap = onSwap,
                            // Bench players: tap is a no-op (only on-court players open the event modal)
                            onClick = { }
                        )
                    }
                }
            }
        }

        // --- Drag ghost overlay ---
        // Rendered at the Box level (above both court and bench), so it is
        // never clipped by the horizontalScroll on the bench row.
        if (dragDropState.isDragging) {
            val ghostCenter = dragDropState.dragCenter
            // Convert from window coords to local coords inside this Box
            val localX = ghostCenter.x - containerWindowOffset.x
            val localY = ghostCenter.y - containerWindowOffset.y
            val ghostSize = if (dragDropState.draggedIsOnCourt) 56.dp else 40.dp

            Box(
                modifier = Modifier
                    .zIndex(100f)
                    .offset {
                        IntOffset(
                            // Center the ghost on the finger position
                            (localX - ghostSize.toPx() / 2f).roundToInt(),
                            (localY - ghostSize.toPx() / 2f).roundToInt()
                        )
                    }
                    // Make the ghost slightly transparent so the drop target is visible beneath
                    .graphicsLayer { alpha = 0.85f }
            ) {
                PlayerIcon(
                    jerseyNumber = dragDropState.draggedJersey,
                    playerName = dragDropState.draggedName,
                    isOnCourt = dragDropState.draggedIsOnCourt,
                    size = ghostSize,
                    accentColor = dragDropState.draggedAccentColor,
                    onClick = { }
                )
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
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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

                Spacer(modifier = Modifier.height(8.dp))

                val twoM = events.count { it.eventType == GameEventType.TWO_POINTER_MADE }
                val twoA = twoM + events.count { it.eventType == GameEventType.TWO_POINTER_MISSED }
                val threeM = events.count { it.eventType == GameEventType.THREE_POINTER_MADE }
                val threeA = threeM + events.count { it.eventType == GameEventType.THREE_POINTER_MISSED }
                val fgm = twoM + threeM
                val fga = twoA + threeA
                val ftm = events.count { it.eventType == GameEventType.FREE_THROW_MADE }
                val fta = ftm + events.count { it.eventType == GameEventType.FREE_THROW_MISSED }
                val oreb = events.count { it.eventType == GameEventType.OFFENSIVE_REBOUND }
                val dreb = events.count { it.eventType == GameEventType.DEFENSIVE_REBOUND }
                val ast = events.count { it.eventType == GameEventType.ASSIST }
                val stl = events.count { it.eventType == GameEventType.STEAL }
                val blk = events.count { it.eventType == GameEventType.BLOCK }
                val to = events.count { it.eventType == GameEventType.TURNOVER }
                val pf = events.count { it.eventType == GameEventType.FOUL }

                // Row 1: shooting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("FG", "$fgm/$fga")
                    StatChip("3PT", "$threeM/$threeA")
                    StatChip("FT", "$ftm/$fta")
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: other stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("OREB", "$oreb")
                    StatChip("DREB", "$dreb")
                    StatChip("AST", "$ast")
                    StatChip("STL", "$stl")
                    StatChip("BLK", "$blk")
                    StatChip("TO", "$to")
                    StatChip("PF", "$pf")
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
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
                Text("Scoring", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventButton("2PT ✓", LocalAppColors.current.twoPointer, Modifier.weight(1f)) {
                        onEventSelected(GameEventType.TWO_POINTER_MADE, 2)
                    }
                    EventButton("3PT ✓", LocalAppColors.current.threePointer, Modifier.weight(1f)) {
                        onEventSelected(GameEventType.THREE_POINTER_MADE, 3)
                    }
                    EventButton("FT ✓", LocalAppColors.current.freeThrow, Modifier.weight(1f)) {
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
                Text("Other", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedEventButton("Off Reb", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.OFFENSIVE_REBOUND, null)
                    }
                    OutlinedEventButton("Def Reb", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.DEFENSIVE_REBOUND, null)
                    }
                    OutlinedEventButton("Assist", Modifier.weight(1f)) {
                        onEventSelected(GameEventType.ASSIST, null)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
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
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

private fun eventLabel(eventType: GameEventType): String = when (eventType) {
    GameEventType.TWO_POINTER_MADE -> "2PT Made"
    GameEventType.TWO_POINTER_MISSED -> "2PT Missed"
    GameEventType.THREE_POINTER_MADE -> "3PT Made"
    GameEventType.THREE_POINTER_MISSED -> "3PT Missed"
    GameEventType.FREE_THROW_MADE -> "FT Made"
    GameEventType.FREE_THROW_MISSED -> "FT Missed"
    GameEventType.OFFENSIVE_REBOUND -> "Off Rebound"
    GameEventType.DEFENSIVE_REBOUND -> "Def Rebound"
    GameEventType.ASSIST -> "Assist"
    GameEventType.STEAL -> "Steal"
    GameEventType.BLOCK -> "Block"
    GameEventType.TURNOVER -> "Turnover"
    GameEventType.FOUL -> "Foul"
    GameEventType.SUBSTITUTION -> "Substitution"
}

@Composable
private fun EventsReviewDialog(
    uiState: GameDashboardUiState,
    onClose: () -> Unit,
    onRequestDelete: (GameEvent) -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Events (${uiState.gameEvents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.gameEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No events logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    val sortedEvents = uiState.gameEvents.sortedByDescending { it.timestamp }
                    val allPlayers = uiState.homePlayers + uiState.awayPlayers
                    val homeTeamName = uiState.homeTeam?.name ?: "Home"
                    val awayTeamName = uiState.awayTeam?.name ?: "Away"

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(sortedEvents, key = { it.id }) { event ->
                            val teamName = if (event.team == GameTeamSide.HOME) homeTeamName else awayTeamName
                            val player = event.playerId?.let { pid -> allPlayers.find { it.id == pid } }
                            val timeStr = "%d:%02d".format(event.timestamp / 60, event.timestamp % 60)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.width(44.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = eventLabel(event.eventType),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (player != null) "${player.firstName} ${player.lastName} · $teamName"
                                               else teamName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(
                                    onClick = { onRequestDelete(event) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete event",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}