package com.example.statstracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamFormUiState(
    val teamName: String = "",
    val logoUrl: String = "",
    val notes: String = "",
    val isOurTeam: Boolean = false,
    val isEditing: Boolean = false,
    val teamId: Long? = null,
    val allPlayers: List<Player> = emptyList(),
    val teamPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean get() = teamName.isNotBlank()
}

class TeamFormViewModel(
    private val teamId: Long?,
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamFormUiState(isEditing = teamId != null, teamId = teamId))
    val uiState: StateFlow<TeamFormUiState> = _uiState.asStateFlow()

    private val _savedTeamId = MutableStateFlow<Long?>(null)
    val savedTeamId: StateFlow<Long?> = _savedTeamId.asStateFlow()

    init {
        if (teamId != null) {
            loadTeamData(teamId)
        }
        loadAllPlayers()
    }

    private fun loadTeamData(teamId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val team = repository.getTeamById(teamId)
                val players = repository.getPlayersForTeam(teamId)
                if (team != null) {
                    _uiState.value = _uiState.value.copy(
                        teamName = team.name,
                        logoUrl = team.logo ?: "",
                        notes = team.notes ?: "",
                        isOurTeam = team.isOurTeam,
                        teamPlayers = players,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Team not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load team: ${e.message}"
                )
            }
        }
    }

    private fun loadAllPlayers() {
        viewModelScope.launch {
            try {
                repository.getAllPlayersFlow().collect { players ->
                    _uiState.value = _uiState.value.copy(allPlayers = players)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(teamName = name)
    }

    fun updateLogoUrl(url: String) {
        _uiState.value = _uiState.value.copy(logoUrl = url)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun toggleIsOurTeam() {
        _uiState.value = _uiState.value.copy(isOurTeam = !_uiState.value.isOurTeam)
    }

    fun addPlayerToTeam(player: Player) {
        val id = teamId ?: return
        viewModelScope.launch {
            try {
                repository.addPlayerToTeam(player.id, id)
                val players = repository.getPlayersForTeam(id)
                _uiState.value = _uiState.value.copy(teamPlayers = players)
            } catch (_: Exception) {}
        }
    }

    fun removePlayerFromTeam(player: Player) {
        val id = teamId ?: return
        viewModelScope.launch {
            try {
                repository.removePlayerFromTeam(player.id, id)
                val players = repository.getPlayersForTeam(id)
                _uiState.value = _uiState.value.copy(teamPlayers = players)
            } catch (_: Exception) {}
        }
    }

    fun saveTeam() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                if (state.isEditing && teamId != null) {
                    val updated = Team(
                        id = teamId,
                        name = state.teamName.trim(),
                        logo = state.logoUrl.trim().takeIf { it.isNotBlank() },
                        notes = state.notes.trim().takeIf { it.isNotBlank() },
                        isOurTeam = state.isOurTeam
                    )
                    repository.updateTeam(updated)
                    _savedTeamId.value = teamId
                } else {
                    val newTeam = Team(
                        name = state.teamName.trim(),
                        logo = state.logoUrl.trim().takeIf { it.isNotBlank() },
                        notes = state.notes.trim().takeIf { it.isNotBlank() },
                        isOurTeam = state.isOurTeam
                    )
                    val newId = repository.insertTeam(newTeam)
                    _savedTeamId.value = newId
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save team: ${e.message}"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamFormScreen(
    teamId: Long?,
    repository: BasketballRepository,
    onNavigateBack: () -> Unit,
    onTeamSaved: (Long) -> Unit
) {
    val viewModel = remember { TeamFormViewModel(teamId, repository) }
    val uiState by viewModel.uiState.collectAsState()
    val savedTeamId by viewModel.savedTeamId.collectAsState()

    LaunchedEffect(savedTeamId) {
        savedTeamId?.let { id -> onTeamSaved(id) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isEditing) "Edit Team" else "New Team",
                            fontWeight = FontWeight.Bold
                        )
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                TeamFormContent(
                    uiState = uiState,
                    onNameChange = viewModel::updateName,
                    onLogoUrlChange = viewModel::updateLogoUrl,
                    onNotesChange = viewModel::updateNotes,
                    onToggleIsOurTeam = viewModel::toggleIsOurTeam,
                    onAddPlayer = viewModel::addPlayerToTeam,
                    onRemovePlayer = viewModel::removePlayerFromTeam,
                    onSave = viewModel::saveTeam
                )
            }
        }
    }
}

@Composable
private fun TeamFormContent(
    uiState: TeamFormUiState,
    onNameChange: (String) -> Unit,
    onLogoUrlChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleIsOurTeam: () -> Unit,
    onAddPlayer: (Player) -> Unit,
    onRemovePlayer: (Player) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Team Name
        item {
            OutlinedTextField(
                value = uiState.teamName,
                onValueChange = onNameChange,
                label = { Text("Team Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.teamName.isBlank(),
                singleLine = true
            )
        }

        // Logo URL
        item {
            OutlinedTextField(
                value = uiState.logoUrl,
                onValueChange = onLogoUrlChange,
                label = { Text("Logo URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Notes
        item {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
        }

        // Our club toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Our club team",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Show this team's insights on the dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isOurTeam,
                    onCheckedChange = { onToggleIsOurTeam() }
                )
            }
        }

        // Player management (only in edit mode)
        if (uiState.isEditing) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Team Players",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                FormPlayerManagementSection(
                    allPlayers = uiState.allPlayers,
                    teamPlayers = uiState.teamPlayers,
                    onAddPlayer = onAddPlayer,
                    onRemovePlayer = onRemovePlayer
                )
            }
        }

        // Save button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isValid && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isEditing) "Update Team" else "Create Team")
            }
        }
    }
}

@Composable
private fun FormPlayerManagementSection(
    allPlayers: List<Player>,
    teamPlayers: List<Player>,
    onAddPlayer: (Player) -> Unit,
    onRemovePlayer: (Player) -> Unit
) {
    val availablePlayers = allPlayers.filter { player ->
        !teamPlayers.any { it.id == player.id }
    }

    var showAddPlayersDialog by remember { mutableStateOf(false) }

    Column {
        // Current team players
        if (teamPlayers.isNotEmpty()) {
            Text(
                text = "Current Players (${teamPlayers.size})",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            teamPlayers.forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${player.firstName} ${player.lastName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = { onRemovePlayer(player) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove player",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add players button
        if (availablePlayers.isNotEmpty()) {
            OutlinedButton(
                onClick = { showAddPlayersDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Players")
            }
        } else {
            Text(
                text = if (teamPlayers.isEmpty()) "No players available" else "All players added",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    // Add Players Dialog
    if (showAddPlayersDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlayersDialog = false },
            title = { Text("Add Players") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(availablePlayers) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${player.firstName} ${player.lastName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                player.heightCm?.let { height ->
                                    Text(
                                        text = "${height}cm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { onAddPlayer(player) },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPlayersDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
