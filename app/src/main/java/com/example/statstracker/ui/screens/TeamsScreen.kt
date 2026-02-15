package com.example.statstracker.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.statstracker.database.DatabaseProvider
import com.example.statstracker.database.entity.Team
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI State for the Teams Screen
 */
sealed class TeamsUiState {
    data object Loading : TeamsUiState()
    data class Success(val teams: List<Team>) : TeamsUiState()
    data class Error(val message: String) : TeamsUiState()
}

/**
 * ViewModel for managing Teams screen state and operations
 */
class TeamsViewModel(
    private val repository: BasketballRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()
    
    private val _selectedTeam = MutableStateFlow<Team?>(null)
    val selectedTeam: StateFlow<Team?> = _selectedTeam.asStateFlow()
    
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()
    
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    init {
        loadTeams()
        insertSampleDataIfEmpty()
    }
    
    private fun loadTeams() {
        viewModelScope.launch {
            try {
                repository.getAllTeamsFlow().collect { teams ->
                    _uiState.value = TeamsUiState.Success(teams)
                }
            } catch (e: Exception) {
                _uiState.value = TeamsUiState.Error("Failed to load teams: ${e.message}")
            }
        }
    }
    
    private fun insertSampleDataIfEmpty() {
        viewModelScope.launch {
            try {
                val existingTeams = repository.getAllTeams()
                if (existingTeams.isEmpty()) {
                    val sampleTeams = listOf(
                        Team(
                            name = "Los Angeles Lakers",
                            notes = "Purple and Gold, 17 NBA Championships"
                        ),
                        Team(
                            name = "Golden State Warriors",
                            notes = "Bay Area team, known for their 3-point shooting"
                        ),
                        Team(
                            name = "Boston Celtics",
                            notes = "Green and white, one of the most successful franchises"
                        ),
                        Team(
                            name = "Chicago Bulls",
                            notes = "Michael Jordan's team, 6 championships in the 90s"
                        )
                    )
                    sampleTeams.forEach { team ->
                        repository.insertTeam(team)
                    }
                    showMessage("Sample teams added!")
                }
            } catch (e: Exception) {
                showMessage("Warning: ${e.message}")
            }
        }
    }
    
    fun insertTeam(team: Team) {
        viewModelScope.launch {
            try {
                repository.insertTeam(team)
                closeDialog()
                showMessage("Team added successfully!")
            } catch (e: Exception) {
                showMessage("Failed to add team: ${e.message}")
            }
        }
    }
    
    fun updateTeam(team: Team) {
        viewModelScope.launch {
            try {
                repository.updateTeam(team)
                closeDialog()
                showMessage("Team updated successfully!")
            } catch (e: Exception) {
                showMessage("Failed to update team: ${e.message}")
            }
        }
    }
    
    fun deleteTeam(team: Team) {
        viewModelScope.launch {
            try {
                repository.deleteTeam(team)
                closeDialog()
                showMessage("Team deleted successfully!")
            } catch (e: Exception) {
                showMessage("Failed to delete team: ${e.message}")
            }
        }
    }
    
    fun openCreateDialog() {
        _selectedTeam.value = null
        _showDialog.value = true
    }
    
    fun openEditDialog(team: Team) {
        _selectedTeam.value = team
        _showDialog.value = true
    }
    
    fun closeDialog() {
        _showDialog.value = false
        _selectedTeam.value = null
    }
    
    private fun showMessage(message: String) {
        _snackbarMessage.value = message
    }
    
    fun clearMessage() {
        _snackbarMessage.value = null
    }
}

/**
 * Factory for creating TeamsViewModel with repository dependency
 */
class TeamsViewModelFactory(
    private val repository: BasketballRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamsViewModel::class.java)) {
            return TeamsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Complete self-contained Jetpack Compose screen for CRUD operations on Team entities.
 * Handles all operations: Create, Read, Update, Delete with modern Material 3 design.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TeamsScreen(onNavigateBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val database = remember { DatabaseProvider.getInstance(context) }
    val repository = remember { BasketballRepository(database) }
    
    val viewModel: TeamsViewModel = viewModel(
        factory = TeamsViewModelFactory(repository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Teams",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    onNavigateBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Team")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TeamsUiState.Loading -> {
                    LoadingIndicator()
                }
                is TeamsUiState.Success -> {
                    if (state.teams.isEmpty()) {
                        EmptyTeamsState { viewModel.openCreateDialog() }
                    } else {
                        TeamsList(
                            teams = state.teams,
                            onEditClick = { team -> viewModel.openEditDialog(team) },
                            onDeleteClick = { team -> viewModel.deleteTeam(team) }
                        )
                    }
                }
                is TeamsUiState.Error -> {
                    ErrorState(message = state.message) {
                        // Retry by recreating the viewModel or calling loadTeams
                    }
                }
            }
        }
    }
    
    // Team Edit/Create Dialog
    if (showDialog) {
        TeamDialog(
            team = selectedTeam,
            onDismiss = { viewModel.closeDialog() },
            onSave = { team ->
                if (selectedTeam != null) {
                    viewModel.updateTeam(team)
                } else {
                    viewModel.insertTeam(team)
                }
            },
            onDelete = { team ->
                viewModel.deleteTeam(team)
            }
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading teams...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyTeamsState(onAddTeam: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No teams yet. Add one!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddTeam,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Team")
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeamsList(
    teams: List<Team>,
    onEditClick: (Team) -> Unit,
    onDeleteClick: (Team) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = teams,
            key = { team -> team.id }
        ) { team ->
            TeamCard(
                team = team,
                onEditClick = { onEditClick(team) },
                onDeleteClick = { onDeleteClick(team) },
                modifier = Modifier.animateItem(
                    fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<IntOffset>(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamCard(
    team: Team,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onEditClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Notes
                    team.notes?.let { notes ->
                        if (notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Action buttons
                Column {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Edit Team",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Team",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamDialog(
    team: Team?,
    onDismiss: () -> Unit,
    onSave: (Team) -> Unit,
    onDelete: (Team) -> Unit
) {
    val isEditing = team != null
    
    var teamName by remember { mutableStateOf(team?.name ?: "") }
    var logoUrl by remember { mutableStateOf(team?.logo ?: "") }
    var notes by remember { mutableStateOf(team?.notes ?: "") }
    
    // Form validation
    val isValid = teamName.isNotBlank()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = if (isEditing) "Edit Team" else "Add New Team",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Team Name
            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Team Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = teamName.isBlank(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logo URL (optional)
            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isEditing) Arrangement.SpaceBetween else Arrangement.End
            ) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedTeam = if (isEditing) {
                                team!!.copy(
                                    name = teamName.trim(),
                                    logo = logoUrl.trim().takeIf { it.isNotBlank() },
                                    notes = notes.trim().takeIf { it.isNotBlank() }
                                )
                            } else {
                                Team(
                                    name = teamName.trim(),
                                    logo = logoUrl.trim().takeIf { it.isNotBlank() },
                                    notes = notes.trim().takeIf { it.isNotBlank() }
                                )
                            }
                            onSave(updatedTeam)
                        },
                        enabled = isValid
                    ) {
                        Text(if (isEditing) "Update" else "Add")
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && team != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Team") },
            text = { Text("Are you sure you want to delete \"${team.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(team)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}