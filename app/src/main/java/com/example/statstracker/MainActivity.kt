package com.example.statstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.ui.screens.PlayersScreen
import com.example.statstracker.ui.screens.TeamsScreen
import com.example.statstracker.ui.screens.NewGameScreen
import com.example.statstracker.ui.screens.GameDashboardScreen
import com.example.statstracker.ui.screens.GamesScreen
import com.example.statstracker.ui.screens.GameScreen
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
                
                // Initialize database
                val database = DatabaseProvider.getInstance(this)
                val repository = remember { 
                    com.example.statstracker.database.repository.BasketballRepository(database) 
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "dashboard" -> Dashboard(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToPlayers = { currentScreen = "players" },
                            onNavigateToTeams = { currentScreen = "teams" },
                            onNavigateToNewGame = { currentScreen = "new_game" },
                            onNavigateToGames = { currentScreen = "games" }
                        )
                        "players" -> PlayersScreen(
                            onNavigateBack = { currentScreen = "dashboard" }
                        )
                        "teams" -> TeamsScreen(
                            onNavigateBack = { currentScreen = "dashboard" }
                        )
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
                            }
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
                    }
                }
            }
        }
    }
}

@Composable
fun Dashboard(
    modifier: Modifier = Modifier,
    onNavigateToPlayers: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onNavigateToNewGame: () -> Unit,
    onNavigateToGames: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "Stats Tracker",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Welcome message
        Text(
            text = "Welcome to your basketball stats dashboard",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Players button
        ElevatedButton(
            onClick = onNavigateToPlayers,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Players",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Teams button
        ElevatedButton(
            onClick = onNavigateToTeams,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Teams",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // New Game button
        ElevatedButton(
            onClick = onNavigateToNewGame,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Game",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // View Games button
        ElevatedButton(
            onClick = onNavigateToGames,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Games",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Placeholder for future features
        Text(
            text = "More features coming soon...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}