package com.example.statstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.example.statstracker.ui.components.AppHeader
import com.example.statstracker.ui.components.AppDrawerContent
import com.example.statstracker.ui.components.AppSidebar
import com.example.statstracker.ui.screens.PlayersScreen
import com.example.statstracker.ui.screens.TeamsScreen
import com.example.statstracker.ui.screens.NewGameScreen
import com.example.statstracker.ui.screens.GameDashboardScreen
import com.example.statstracker.ui.screens.GamesScreen
import com.example.statstracker.ui.screens.GameScreen
import com.example.statstracker.ui.screens.DashboardScreen
import com.example.statstracker.ui.screens.TeamDetailScreen
import com.example.statstracker.ui.screens.TeamFormScreen
import com.example.statstracker.ui.screens.PlayerDetailScreen
import com.example.statstracker.ui.screens.PlayerFormScreen
import com.example.statstracker.ui.screens.SettingsScreen
import com.example.statstracker.ui.theme.StatsTrackerTheme
import com.example.statstracker.database.DatabaseProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StatsTrackerTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }
                var gameId by remember { mutableStateOf<Long?>(null) }
                var selectedGameId by remember { mutableStateOf<Long?>(null) }
                var selectedTeamId by remember { mutableStateOf<Long?>(null) }
                var editingTeamId by remember { mutableStateOf<Long?>(null) }
                var teamFormOpenedFromDetail by remember { mutableStateOf(false) }
                var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
                var editingPlayerId by remember { mutableStateOf<Long?>(null) }
                var playerFormOpenedFromDetail by remember { mutableStateOf(false) }
                
                // Initialize database
                val database = DatabaseProvider.getInstance(this)
                val repository = remember { 
                    com.example.statstracker.database.repository.BasketballRepository(database) 
                }
                
                var screenWidthDp by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                val isTablet = screenWidthDp >= 600.dp
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            screenWidthDp = with(density) { size.width.toDp() }
                        }
                ) {
                when (currentScreen) {
                    "dashboard" -> {
                        if (isTablet) {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0),
                                topBar = { AppHeader(isTablet = true, onMenuClick = {}) }
                            ) { innerPadding ->
                                Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                    AppSidebar(
                                        currentRoute = currentScreen,
                                        onItemClick = { route -> currentScreen = route }
                                    )
                                    DashboardScreen(
                                        repository = repository,
                                        onNavigateToGame = { gameId ->
                                            selectedGameId = gameId
                                            currentScreen = "game_detail"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color(0xFFF2F2F2))
                                    )
                                }
                            }
                        } else {
                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    AppDrawerContent(
                                        currentRoute = currentScreen,
                                        onItemClick = { route ->
                                            currentScreen = route
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            ) {
                                Scaffold(
                                    contentWindowInsets = WindowInsets(0),
                                    topBar = {
                                        AppHeader(
                                            isTablet = false,
                                            onMenuClick = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                ) { innerPadding ->
                                    DashboardScreen(
                                        repository = repository,
                                        onNavigateToGame = { gameId ->
                                            selectedGameId = gameId
                                            currentScreen = "game_detail"
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                    "players" -> PlayersScreen(
                        onNavigateBack = { currentScreen = "dashboard" },
                        onPlayerClick = { playerId ->
                            selectedPlayerId = playerId
                            currentScreen = "player_detail"
                        },
                        onCreatePlayer = {
                            editingPlayerId = null
                            playerFormOpenedFromDetail = false
                            currentScreen = "player_form"
                        },
                        onEditPlayer = { playerId ->
                            editingPlayerId = playerId
                            playerFormOpenedFromDetail = false
                            currentScreen = "player_form"
                        }
                    )
                    "player_detail" -> {
                        selectedPlayerId?.let { id ->
                            key(id) {
                                PlayerDetailScreen(
                                    playerId = id,
                                    repository = repository,
                                    onNavigateBack = {
                                        currentScreen = "players"
                                        selectedPlayerId = null
                                    },
                                    onEditPlayer = { editId ->
                                        editingPlayerId = editId
                                        playerFormOpenedFromDetail = true
                                        currentScreen = "player_form"
                                    }
                                )
                            }
                        }
                    }
                    "player_form" -> {
                        PlayerFormScreen(
                            playerId = editingPlayerId,
                            repository = repository,
                            onNavigateBack = {
                                if (playerFormOpenedFromDetail && selectedPlayerId != null) {
                                    currentScreen = "player_detail"
                                } else {
                                    currentScreen = "players"
                                }
                                editingPlayerId = null
                                playerFormOpenedFromDetail = false
                            },
                            onPlayerSaved = { savedId ->
                                selectedPlayerId = savedId
                                currentScreen = "player_detail"
                                editingPlayerId = null
                                playerFormOpenedFromDetail = false
                            },
                            onPlayerDeleted = {
                                currentScreen = "players"
                                selectedPlayerId = null
                                editingPlayerId = null
                                playerFormOpenedFromDetail = false
                            }
                        )
                    }
                    "teams" -> TeamsScreen(
                        onNavigateBack = { currentScreen = "dashboard" },
                        onTeamClick = { teamId ->
                            selectedTeamId = teamId
                            currentScreen = "team_detail"
                        },
                        onCreateTeam = {
                            editingTeamId = null
                            teamFormOpenedFromDetail = false
                            currentScreen = "team_form"
                        },
                        onEditTeam = { teamId ->
                            editingTeamId = teamId
                            teamFormOpenedFromDetail = false
                            currentScreen = "team_form"
                        }
                    )
                    "team_detail" -> {
                        selectedTeamId?.let { id ->
                            key(id) {
                                TeamDetailScreen(
                                    teamId = id,
                                    repository = repository,
                                    onNavigateBack = {
                                        currentScreen = "teams"
                                        selectedTeamId = null
                                    },
                                    onEditTeam = { editId ->
                                        editingTeamId = editId
                                        teamFormOpenedFromDetail = true
                                        currentScreen = "team_form"
                                    }
                                )
                            }
                        }
                    }
                    "team_form" -> {
                        TeamFormScreen(
                            teamId = editingTeamId,
                            repository = repository,
                            onNavigateBack = {
                                if (teamFormOpenedFromDetail && selectedTeamId != null) {
                                    currentScreen = "team_detail"
                                } else {
                                    currentScreen = "teams"
                                }
                                editingTeamId = null
                                teamFormOpenedFromDetail = false
                            },
                            onTeamSaved = { savedId ->
                                selectedTeamId = savedId
                                currentScreen = "team_detail"
                                editingTeamId = null
                                teamFormOpenedFromDetail = false
                            }
                        )
                    }
                    "new_game" -> NewGameScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = "dashboard" },
                        onGameCreated = { createdGameId ->
                            gameId = createdGameId
                            currentScreen = "game_dashboard"
                        }
                    )
                    "game_dashboard" -> {
                        gameId?.let { id ->
                            GameDashboardScreen(
                                gameId = id,
                                repository = repository,
                                onNavigateBack = { 
                                    currentScreen = "dashboard"
                                    gameId = null
                                }
                            )
                        }
                    }
                    "games" -> GamesScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = "dashboard" },
                        onGameSelected = { selectedId ->
                            selectedGameId = selectedId
                            currentScreen = "game_detail"
                        },
                        onCreateGame = { currentScreen = "new_game" }
                    )
                    "game_detail" -> {
                        selectedGameId?.let { id ->
                            GameScreen(
                                gameId = id,
                                repository = repository,
                                onNavigateBack = { 
                                    currentScreen = "games"
                                    selectedGameId = null
                                }
                            )
                        }
                    }
                    "settings" -> SettingsScreen(
                        onNavigateBack = { currentScreen = "dashboard" }
                    )
                }
                }
            }
        }
    }
}

