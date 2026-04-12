package com.example.statstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.GameTeamSide
import com.example.statstracker.model.TrackingMode
import com.example.statstracker.ui.viewmodel.NewGameUiState
import com.example.statstracker.ui.viewmodel.NewGameViewModel
import com.example.statstracker.ui.viewmodel.PlayerWithJersey
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameScreen(
    repository: BasketballRepository,
    onNavigateBack: () -> Unit,
    onGameCreated: (Long) -> Unit
) {
    val viewModel = remember { NewGameViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = {
                        Text("New Game", fontWeight = FontWeight.Medium)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Home Team Section
            item {
                TeamSetupSection(
                    title = "Home Team",
                    searchQuery = uiState.homeSearchQuery,
                    searchResults = uiState.homeSearchResults,
                    selectedTeam = uiState.homeTeam,
                    isNewTeam = uiState.homeIsNewTeam,
                    trackingMode = uiState.homeTrackingMode,
                    hasPlayers = uiState.homeTeamHasPlayers,
                    players = uiState.homePlayers,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onSearchQueryChanged = viewModel::updateHomeSearchQuery,
                    onTeamSelected = viewModel::selectHomeTeam,
                    onNewTeamSelected = viewModel::selectNewHomeTeam,
                    onClearTeam = viewModel::clearHomeTeam,
                    onTrackingModeChanged = { viewModel.setTrackingMode(GameTeamSide.HOME, it) },
                    onTogglePlayer = { viewModel.togglePlayerSelection(GameTeamSide.HOME, it) },
                    onJerseyChanged = { pid, j -> viewModel.updatePlayerJersey(GameTeamSide.HOME, pid, j) }
                )
            }

            // Away Team Section
            item {
                TeamSetupSection(
                    title = "Away Team",
                    searchQuery = uiState.awaySearchQuery,
                    searchResults = uiState.awaySearchResults,
                    selectedTeam = uiState.awayTeam,
                    isNewTeam = uiState.awayIsNewTeam,
                    trackingMode = uiState.awayTrackingMode,
                    hasPlayers = uiState.awayTeamHasPlayers,
                    players = uiState.awayPlayers,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onSearchQueryChanged = viewModel::updateAwaySearchQuery,
                    onTeamSelected = viewModel::selectAwayTeam,
                    onNewTeamSelected = viewModel::selectNewAwayTeam,
                    onClearTeam = viewModel::clearAwayTeam,
                    onTrackingModeChanged = { viewModel.setTrackingMode(GameTeamSide.AWAY, it) },
                    onTogglePlayer = { viewModel.togglePlayerSelection(GameTeamSide.AWAY, it) },
                    onJerseyChanged = { pid, j -> viewModel.updatePlayerJersey(GameTeamSide.AWAY, pid, j) }
                )
            }

            // Game Details
            item {
                GameDetailsCard(
                    uiState = uiState,
                    onPlaceChanged = viewModel::updateGamePlace,
                    onNotesChanged = viewModel::updateGameNotes
                )
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Start Game Button
            item {
                Button(
                    onClick = { viewModel.createGame(onGameCreated) },
                    enabled = uiState.canCreateGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Game", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSetupSection(
    title: String,
    searchQuery: String,
    searchResults: List<Team>,
    selectedTeam: Team?,
    isNewTeam: Boolean,
    trackingMode: TrackingMode,
    hasPlayers: Boolean,
    players: List<PlayerWithJersey>,
    containerColor: Color,
    onSearchQueryChanged: (String) -> Unit,
    onTeamSelected: (Team) -> Unit,
    onNewTeamSelected: (String) -> Unit,
    onClearTeam: () -> Unit,
    onTrackingModeChanged: (TrackingMode) -> Unit,
    onTogglePlayer: (Long) -> Unit,
    onJerseyChanged: (Long, Int) -> Unit
) {
    val teamIsChosen = selectedTeam != null || isNewTeam

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = containerColor.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            if (teamIsChosen) {
                // Show selected team as chip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNewTeam) "${searchQuery.trim()} (new)" else selectedTeam!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearTeam, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tracking mode toggle
                Text(
                    text = "Stats Tracking",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = trackingMode == TrackingMode.BY_TEAM,
                        onClick = { onTrackingModeChanged(TrackingMode.BY_TEAM) },
                        label = { Text("By Team") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = trackingMode == TrackingMode.BY_PLAYER,
                        onClick = {
                            if (hasPlayers) onTrackingModeChanged(TrackingMode.BY_PLAYER)
                        },
                        label = { Text("By Player") },
                        enabled = hasPlayers,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!hasPlayers && !isNewTeam) {
                    Text(
                        text = "This team has no players. Add players to enable per-player tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Player selection (when BY_PLAYER)
                if (trackingMode == TrackingMode.BY_PLAYER && players.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Players & Jersey Numbers",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val selectedCount = players.count { it.isSelected }
                    if (selectedCount < 5) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚠ $selectedCount player(s) selected. At least 5 recommended.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    players.forEach { pwj ->
                        PlayerSelectionRow(
                            playerWithJersey = pwj,
                            onToggle = { onTogglePlayer(pwj.player.id) },
                            onJerseyChanged = { jersey -> onJerseyChanged(pwj.player.id, jersey) }
                        )
                    }
                }
            } else {
                // Search field
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded && (searchResults.isNotEmpty() || searchQuery.isNotBlank()),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            onSearchQueryChanged(it)
                            expanded = true
                        },
                        placeholder = { Text("Search or type new team name...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && (searchResults.isNotEmpty() || searchQuery.length >= 2),
                        onDismissRequest = { expanded = false }
                    ) {
                        searchResults.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    onTeamSelected(team)
                                    expanded = false
                                }
                            )
                        }
                        if (searchQuery.length >= 2) {
                            val exactMatch = searchResults.any {
                                it.name.equals(searchQuery.trim(), ignoreCase = true)
                            }
                            if (!exactMatch) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Create new: \"${searchQuery.trim()}\"",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        onNewTeamSelected(searchQuery.trim())
                                        expanded = false
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
private fun PlayerSelectionRow(
    playerWithJersey: PlayerWithJersey,
    onToggle: () -> Unit,
    onJerseyChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = playerWithJersey.isSelected,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = "${playerWithJersey.player.firstName} ${playerWithJersey.player.lastName}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (playerWithJersey.isSelected) {
            OutlinedTextField(
                value = if (playerWithJersey.jerseyNumber == 0) "" else playerWithJersey.jerseyNumber.toString(),
                onValueChange = { text ->
                    val num = text.filter { it.isDigit() }.take(2).toIntOrNull() ?: 0
                    onJerseyChanged(num)
                },
                label = { Text("#") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

@Composable
private fun GameDetailsCard(
    uiState: NewGameUiState,
    onPlaceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Game Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            OutlinedTextField(
                value = uiState.gameDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onValueChange = {},
                label = { Text("Game Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.gamePlace ?: "",
                onValueChange = onPlaceChanged,
                label = { Text("Location (Optional)") },
                placeholder = { Text("e.g., Home Court, Away Arena...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.gameNotes ?: "",
                onValueChange = onNotesChanged,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Any additional notes...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
    }
}