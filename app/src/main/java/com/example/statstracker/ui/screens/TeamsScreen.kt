package com.example.statstracker.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
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
                            notes = "Purple and Gold, 17 NBA Championships. Home arena: Crypto.com Arena"
                        ),
                        Team(
                            name = "Boston Celtics",
                            notes = "Most decorated franchise with 18 NBA Championships. Home arena: TD Garden"
                        ),
                        Team(
                            name = "Golden State Warriors",
                            notes = "Dynasty of the 2010s-2020s, 7 NBA Championships. Home arena: Chase Center"
                        ),
                        Team(
                            name = "Milwaukee Bucks",
                            notes = "2021 NBA Champions, home of Giannis. Home arena: Fiserv Forum"
                        ),
                        Team(
                            name = "Denver Nuggets",
                            notes = "2023 NBA Champions, home of Nikola Jokic. Home arena: Ball Arena"
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
                showMessage("Team deleted successfully!")
            } catch (e: Exception) {
                showMessage("Failed to delete team: ${e.message}")
            }
        }
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
fun TeamsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onTeamClick: ((Long) -> Unit)? = null,
    onCreateTeam: (() -> Unit)? = null,
    onEditTeam: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember { DatabaseProvider.getInstance(context) }
    val repository = remember { BasketballRepository(database) }
    
    val viewModel: TeamsViewModel = viewModel(
        factory = TeamsViewModelFactory(repository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
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
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(shadowElevation = 4.dp) {
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
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTeam?.invoke() },
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
                        EmptyTeamsState { onCreateTeam?.invoke() }
                    } else {
                        TeamsList(
                            teams = state.teams,
                            onTeamClick = { team -> onTeamClick?.invoke(team.id) },
                            onEditClick = { team -> onEditTeam?.invoke(team.id) },
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
                imageVector = Icons.Default.Groups,
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
    onTeamClick: (Team) -> Unit,
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
                onClick = { onTeamClick(team) },
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
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "team_card_scale"
    )

    val editInteractionSource = remember { MutableInteractionSource() }
    val isEditPressed by editInteractionSource.collectIsPressedAsState()
    val editScale by animateFloatAsState(
        targetValue = if (isEditPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "edit_button_scale"
    )

    val deleteInteractionSource = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteScale by animateFloatAsState(
        targetValue = if (isDeletePressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "delete_button_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 5.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        interactionSource = cardInteractionSource,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    team.notes?.let { notes ->
                        if (notes.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = notes,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = editScale
                                scaleY = editScale
                            },
                        interactionSource = editInteractionSource,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Team"
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = deleteScale
                                scaleY = deleteScale
                            },
                        interactionSource = deleteInteractionSource,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Team"
                        )
                    }
                }
            }
        }
    }
}

