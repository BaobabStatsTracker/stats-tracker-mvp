package com.example.statstracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.TrackingMode
import com.example.statstracker.ui.viewmodel.NewGameViewModel
import java.time.LocalDate
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
    val availableTeams by viewModel.availableTeams.collectAsState()

    // Show error if present
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // You might want to show a snackbar or dialog here
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "New Game",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Team Selection Section
            item {
                TeamSelectionSection(
                    uiState = uiState,
                    availableTeams = availableTeams,
                    onHomeTeamSelected = viewModel::selectHomeTeam,
                    onAwayTeamSelected = viewModel::selectAwayTeam
                )
            }

            // Game Details Section
            item {
                GameDetailsSection(
                    uiState = uiState,
                    onDateChanged = viewModel::updateGameDate,
                    onPlaceChanged = viewModel::updateGamePlace,
                    onNotesChanged = viewModel::updateGameNotes
                )
            }

            // Tracking Mode Info
            item {
                TrackingModeInfoSection(uiState = uiState)
            }

            // Error Display
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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

            // Create Game Button
            item {
                Button(
                    onClick = { viewModel.createGame(onGameCreated) },
                    enabled = uiState.canCreateGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Game",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TeamSelectionSection(
    uiState: com.example.statstracker.ui.viewmodel.NewGameUiState,
    availableTeams: List<Team>,
    onHomeTeamSelected: (Team) -> Unit,
    onAwayTeamSelected: (Team) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Teams",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Home Team Selection
            TeamSelectionCard(
                title = "Home Team",
                selectedTeam = uiState.homeTeam,
                availableTeams = availableTeams.filter { it.id != uiState.awayTeam?.id },
                onTeamSelected = onHomeTeamSelected,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Away Team Selection
            TeamSelectionCard(
                title = "Away Team",
                selectedTeam = uiState.awayTeam,
                availableTeams = availableTeams.filter { it.id != uiState.homeTeam?.id },
                onTeamSelected = onAwayTeamSelected,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelectionCard(
    title: String,
    selectedTeam: Team?,
    availableTeams: List<Team>,
    onTeamSelected: (Team) -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedTeam?.name ?: "Select a team...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = contentColor,
                        unfocusedBorderColor = contentColor.copy(alpha = 0.7f),
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableTeams.forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name) },
                            onClick = {
                                onTeamSelected(team)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameDetailsSection(
    uiState: com.example.statstracker.ui.viewmodel.NewGameUiState,
    onDateChanged: (LocalDate) -> Unit,
    onPlaceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Game Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Date field 
            OutlinedTextField(
                value = uiState.gameDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onValueChange = { },
                label = { Text("Game Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Place field
            OutlinedTextField(
                value = uiState.gamePlace ?: "",
                onValueChange = onPlaceChanged,
                label = { Text("Location (Optional)") },
                placeholder = { Text("e.g., Home Court, Away Arena...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notes field
            OutlinedTextField(
                value = uiState.gameNotes ?: "",
                onValueChange = onNotesChanged,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Any additional notes about the game...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun TrackingModeInfoSection(
    uiState: com.example.statstracker.ui.viewmodel.NewGameUiState
) {
    if (uiState.homeTeam != null && uiState.awayTeam != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Event Tracking Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TrackingModeRow(
                    teamName = uiState.homeTeam.name,
                    trackingMode = uiState.homeTrackingMode,
                    isHome = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                TrackingModeRow(
                    teamName = uiState.awayTeam.name,
                    trackingMode = uiState.awayTrackingMode,
                    isHome = false
                )
            }
        }
    }
}

@Composable
private fun TrackingModeRow(
    teamName: String,
    trackingMode: TrackingMode,
    isHome: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$teamName:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (trackingMode == TrackingMode.BY_PLAYER) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (trackingMode == TrackingMode.BY_PLAYER) "Player Events" else "Team Events",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}