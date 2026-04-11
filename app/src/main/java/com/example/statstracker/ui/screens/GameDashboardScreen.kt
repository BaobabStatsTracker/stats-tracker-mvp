package com.example.statstracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.ui.viewmodel.GameDashboardTab
import com.example.statstracker.ui.viewmodel.GameDashboardViewModel
import com.example.statstracker.ui.components.EventLoggingSection
import com.example.statstracker.ui.components.GameLogSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDashboardScreen(
    gameId: Long,
    repository: BasketballRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { GameDashboardViewModel(gameId, repository) }
    val uiState by viewModel.uiState.collectAsState()

    // Show error if present
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // You might want to show a snackbar or dialog here
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.game == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Game not found")
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "${uiState.homeTeam?.name} vs ${uiState.awayTeam?.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                uiState.game!!.date.toString(),
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scoreboard and Timer
            ScoreboardSection(
                homeTeam = uiState.homeTeam?.name ?: "Home",
                awayTeam = uiState.awayTeam?.name ?: "Away",
                homeScore = uiState.homeScore,
                awayScore = uiState.awayScore,
                currentQuarter = uiState.currentQuarter,
                quarterTimeRemaining = viewModel.formatTime(uiState.quarterTimeRemaining),
                isTimerRunning = uiState.isTimerRunning,
                isInOvertime = uiState.isInOvertime,
                overtimeNumber = uiState.overtimeNumber,
                onStartTimer = viewModel::startTimer,
                onPauseTimer = viewModel::pauseTimer,
                onResetTimer = viewModel::resetTimer,
                onEndQuarter = viewModel::endQuarter
            )

            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == GameDashboardTab.HOME_EVENTS,
                    onClick = { viewModel.selectTab(GameDashboardTab.HOME_EVENTS) },
                    text = { Text("${uiState.homeTeam?.name ?: "Home"} Events") }
                )
                Tab(
                    selected = uiState.selectedTab == GameDashboardTab.AWAY_EVENTS,
                    onClick = { viewModel.selectTab(GameDashboardTab.AWAY_EVENTS) },
                    text = { Text("${uiState.awayTeam?.name ?: "Away"} Events") }
                )
                Tab(
                    selected = uiState.selectedTab == GameDashboardTab.GAME_LOG,
                    onClick = { viewModel.selectTab(GameDashboardTab.GAME_LOG) },
                    text = { Text("Game Log") }
                )
            }

            // Content based on selected tab
            when (uiState.selectedTab) {
                GameDashboardTab.HOME_EVENTS -> {
                    EventLoggingSection(
                        teamSide = GameTeamSide.HOME,
                        teamName = uiState.homeTeam?.name ?: "Home",
                        players = uiState.homePlayers,
                        canLogPlayerEvents = uiState.homeCanLogPlayerEvents,
                        onLogPlayerEvent = { playerId, eventType, pointsValue ->
                            viewModel.logPlayerEvent(
                                playerId = playerId,
                                teamSide = GameTeamSide.HOME,
                                eventType = eventType,
                                pointsValue = pointsValue
                            )
                        },
                        onLogTeamEvent = { eventType, pointsValue ->
                            viewModel.logTeamEvent(
                                teamSide = GameTeamSide.HOME,
                                eventType = eventType,
                                pointsValue = pointsValue
                            )
                        }
                    )
                }
                GameDashboardTab.AWAY_EVENTS -> {
                    EventLoggingSection(
                        teamSide = GameTeamSide.AWAY,
                        teamName = uiState.awayTeam?.name ?: "Away",
                        players = uiState.awayPlayers,
                        canLogPlayerEvents = uiState.awayCanLogPlayerEvents,
                        onLogPlayerEvent = { playerId, eventType, pointsValue ->
                            viewModel.logPlayerEvent(
                                playerId = playerId,
                                teamSide = GameTeamSide.AWAY,
                                eventType = eventType,
                                pointsValue = pointsValue
                            )
                        },
                        onLogTeamEvent = { eventType, pointsValue ->
                            viewModel.logTeamEvent(
                                teamSide = GameTeamSide.AWAY,
                                eventType = eventType,
                                pointsValue = pointsValue
                            )
                        }
                    )
                }
                GameDashboardTab.GAME_LOG -> {
                    GameLogSection(
                        events = uiState.gameEvents,
                        homeTeam = uiState.homeTeam?.name ?: "Home",
                        awayTeam = uiState.awayTeam?.name ?: "Away",
                        homePlayers = uiState.homePlayers,
                        awayPlayers = uiState.awayPlayers,
                        onDeleteEvent = viewModel::deleteEvent,
                        formatTime = viewModel::formatTime
                    )
                }
            }
        }
    }
    
    // End Game Dialog
    if (uiState.showEndGameDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndGameDialog,
            title = { Text("Game Status") },
            text = {
                Text(
                    if (uiState.homeScore == uiState.awayScore) {
                        "Game is tied ${uiState.homeScore}-${uiState.awayScore}. Do you want to go to overtime or end the game?"
                    } else {
                        "End the game or continue to overtime?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.goToOvertime()
                    }
                ) {
                    Text("Go to ${if (uiState.overtimeNumber == 0) "OT1" else "OT${uiState.overtimeNumber + 1}"}")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::endGame
                ) {
                    Text("End Game")
                }
            }
        )
    }
    
    // End Quarter Dialog
    if (uiState.showEndQuarterDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndQuarterDialog,
            title = { Text("End Quarter") },
            text = {
                Text(
                    if (uiState.isInOvertime) {
                        "Are you sure you want to end OT${uiState.overtimeNumber}?"
                    } else if (uiState.currentQuarter < 4) {
                        "Are you sure you want to end Quarter ${uiState.currentQuarter}?"
                    } else {
                        "Are you sure you want to end the 4th quarter? This will determine if the game goes to overtime."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmEndQuarter
                ) {
                    Text("Yes, End Quarter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissEndQuarterDialog
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScoreboardSection(
    homeTeam: String,
    awayTeam: String,
    homeScore: Int,
    awayScore: Int,
    currentQuarter: Int,
    quarterTimeRemaining: String,
    isTimerRunning: Boolean,
    isInOvertime: Boolean,
    overtimeNumber: Int,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onEndQuarter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = homeTeam,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = homeScore.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quarter/OT and Timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isInOvertime) "OT$overtimeNumber" else "Q$currentQuarter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = quarterTimeRemaining,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "TIME LEFT",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Away Team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = awayTeam,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = awayScore.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (!isTimerRunning) {
                    ElevatedButton(
                        onClick = onStartTimer,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                } else {
                    ElevatedButton(
                        onClick = onPauseTimer,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                }

                ElevatedButton(
                    onClick = onEndQuarter,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isInOvertime) "End OT" else if (currentQuarter < 4) "End Quarter" else "End Game")
                }

                ElevatedButton(
                    onClick = onResetTimer
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}