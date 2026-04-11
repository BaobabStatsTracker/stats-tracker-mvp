package com.example.statstracker.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.statstracker.database.DatabaseProvider
import com.example.statstracker.model.PrimaryHand
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * UI State for the Players Screen
 */
sealed class PlayersUiState {
    data object Loading : PlayersUiState()
    data class Success(val players: List<Player>) : PlayersUiState()
    data class Error(val message: String) : PlayersUiState()
}

/**
 * ViewModel for managing Players screen state and operations
 */
class PlayersViewModel(
    private val repository: BasketballRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PlayersUiState>(PlayersUiState.Loading)
    val uiState: StateFlow<PlayersUiState> = _uiState.asStateFlow()
    
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    init {
        loadPlayers()
        insertSampleDataIfEmpty()
    }
    
    private fun loadPlayers() {
        viewModelScope.launch {
            try {
                repository.getAllPlayersFlow().collect { players ->
                    _uiState.value = PlayersUiState.Success(players)
                }
            } catch (e: Exception) {
                _uiState.value = PlayersUiState.Error("Failed to load players: ${e.message}")
            }
        }
    }
    
    private fun insertSampleDataIfEmpty() {
        viewModelScope.launch {
            try {
                val existingPlayers = repository.getAllPlayers()
                if (existingPlayers.isEmpty()) {
                    val samplePlayers = listOf(
                        Player(
                            firstName = "LeBron",
                            lastName = "James",
                            heightCm = 206,
                            wingspanCm = 214,
                            primaryHand = PrimaryHand.RIGHT,
                            dateOfBirth = LocalDate.of(1984, 12, 30),
                            notes = "4x NBA Champion"
                        ),
                        Player(
                            firstName = "Stephen",
                            lastName = "Curry",
                            heightCm = 188,
                            wingspanCm = 193,
                            primaryHand = PrimaryHand.RIGHT,
                            dateOfBirth = LocalDate.of(1988, 3, 14),
                            notes = "2x MVP, 3-point record holder"
                        ),
                        Player(
                            firstName = "Giannis",
                            lastName = "Antetokounmpo",
                            heightCm = 211,
                            wingspanCm = 221,
                            primaryHand = PrimaryHand.RIGHT,
                            dateOfBirth = LocalDate.of(1994, 12, 6),
                            notes = "Greek Freak, 2x MVP"
                        )
                    )
                    samplePlayers.forEach { player ->
                        repository.insertPlayer(player)
                    }
                    showMessage("Sample players added!")
                }
            } catch (e: Exception) {
                showMessage("Warning: ${e.message}")
            }
        }
    }
    
    fun insertPlayer(player: Player) {
        viewModelScope.launch {
            try {
                repository.insertPlayer(player)
                showMessage("Player added successfully!")
            } catch (e: Exception) {
                showMessage("Failed to add player: ${e.message}")
            }
        }
    }
    
    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            try {
                repository.updatePlayer(player)
                showMessage("Player updated successfully!")
            } catch (e: Exception) {
                showMessage("Failed to update player: ${e.message}")
            }
        }
    }
    
    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            try {
                repository.deletePlayer(player)
                showMessage("Player deleted successfully!")
            } catch (e: Exception) {
                showMessage("Failed to delete player: ${e.message}")
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
 * Factory for creating PlayersViewModel with repository dependency
 */
class PlayersViewModelFactory(
    private val repository: BasketballRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayersViewModel::class.java)) {
            return PlayersViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Complete self-contained Jetpack Compose screen for CRUD operations on Player entities.
 * Handles all operations: Create, Read, Update, Delete with modern Material 3 design.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayersScreen(
    onNavigateBack: (() -> Unit)? = null,
    onPlayerClick: ((Long) -> Unit)? = null,
    onCreatePlayer: (() -> Unit)? = null,
    onEditPlayer: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember { DatabaseProvider.getInstance(context) }
    val repository = remember { BasketballRepository(database) }
    
    val viewModel: PlayersViewModel = viewModel(
        factory = PlayersViewModelFactory(repository)
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
                            text = "Players",
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
                onClick = { onCreatePlayer?.invoke() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Player")
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
                is PlayersUiState.Loading -> {
                    LoadingIndicator()
                }
                is PlayersUiState.Success -> {
                    if (state.players.isEmpty()) {
                        EmptyPlayersState { onCreatePlayer?.invoke() }
                    } else {
                        PlayersList(
                            players = state.players,
                            onPlayerClick = { player -> onPlayerClick?.invoke(player.id) },
                            onEditClick = { player -> onEditPlayer?.invoke(player.id) },
                            onDeleteClick = { player -> viewModel.deletePlayer(player) }
                        )
                    }
                }
                is PlayersUiState.Error -> {
                    ErrorState(message = state.message) {
                        // Retry by recreating the viewModel or calling loadPlayers
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
                text = "Loading players...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPlayersState(onAddPlayer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No players yet. Add one!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddPlayer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Player")
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
private fun PlayersList(
    players: List<Player>,
    onPlayerClick: (Player) -> Unit,
    onEditClick: (Player) -> Unit,
    onDeleteClick: (Player) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = players,
            key = { player -> player.id }
        ) { player ->
            PlayerCard(
                player = player,
                onClick = { onPlayerClick(player) },
                onEditClick = { onEditClick(player) },
                onDeleteClick = { onDeleteClick(player) },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerCard(
    player: Player,
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
        label = "player_card_scale"
    )

    val editInteractionSource = remember { MutableInteractionSource() }
    val isEditPressed by editInteractionSource.collectIsPressedAsState()
    val editScale by animateFloatAsState(
        targetValue = if (isEditPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "player_edit_button_scale"
    )

    val deleteInteractionSource = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteScale by animateFloatAsState(
        targetValue = if (isDeletePressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "player_delete_button_scale"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "${player.firstName} ${player.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        player.heightCm?.let { height ->
                            AttributeChip(
                                label = "Height",
                                value = "${height}cm"
                            )
                        }
                        player.wingspanCm?.let { wingspan ->
                            AttributeChip(
                                label = "Wingspan", 
                                value = "${wingspan}cm"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        player.primaryHand?.let { hand ->
                            PrimaryHandBadge(hand = hand)
                        }
                        player.dateOfBirth?.let { birthDate ->
                            Text(
                                text = "Born: ${birthDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    player.notes?.let { notes ->
                        if (notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = notes,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            contentDescription = "Edit Player"
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
                            contentDescription = "Delete Player"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeChip(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PrimaryHandBadge(hand: PrimaryHand) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hand == PrimaryHand.RIGHT) 
            MaterialTheme.colorScheme.secondaryContainer 
        else 
            MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = hand.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (hand == PrimaryHand.RIGHT)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDialog(
    player: Player?,
    onDismiss: () -> Unit,
    onSave: (Player) -> Unit,
    onDelete: (Player) -> Unit
) {
    val isEditing = player != null
    
    var firstName by remember { mutableStateOf(player?.firstName ?: "") }
    var lastName by remember { mutableStateOf(player?.lastName ?: "") }
    var heightText by remember { mutableStateOf(player?.heightCm?.toString() ?: "") }
    var wingspanText by remember { mutableStateOf(player?.wingspanCm?.toString() ?: "") }
    var selectedHand by remember { mutableStateOf(player?.primaryHand) }
    var birthDate by remember { mutableStateOf(player?.dateOfBirth?.toString() ?: "") }
    var imageUrl by remember { mutableStateOf(player?.image ?: "") }
    var notes by remember { mutableStateOf(player?.notes ?: "") }
    var expanded by remember { mutableStateOf(false) }
    
    // Form validation
    val isValid = firstName.isNotBlank() && lastName.isNotBlank()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Player" else "Add New Player",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = firstName.isBlank()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = lastName.isBlank()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Height and Wingspan Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { char -> char.isDigit() } },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = wingspanText,
                    onValueChange = { wingspanText = it.filter { char -> char.isDigit() } },
                    label = { Text("Wingspan (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Primary Hand Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedHand?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primary Hand") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            selectedHand = null
                            expanded = false
                        }
                    )
                    PrimaryHand.entries.forEach { hand ->
                        DropdownMenuItem(
                            text = { Text(hand.name) },
                            onClick = {
                                selectedHand = hand
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Birth Date
            OutlinedTextField(
                value = birthDate,
                onValueChange = { 
                    // Allow only valid date format characters
                    birthDate = it.filter { char -> char.isDigit() || char == '-' }
                },
                label = { Text("Date of Birth (yyyy-mm-dd)") },
                placeholder = { Text("1995-01-15") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Image URL
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val parsedBirthDate = try {
                            if (birthDate.isNotBlank()) LocalDate.parse(birthDate) else null
                        } catch (e: DateTimeParseException) {
                            null
                        }
                        
                        val playerToSave = Player(
                            id = player?.id ?: 0,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            heightCm = heightText.toIntOrNull(),
                            wingspanCm = wingspanText.toIntOrNull(),
                            primaryHand = selectedHand,
                            dateOfBirth = parsedBirthDate,
                            image = imageUrl.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null }
                        )
                        
                        onSave(playerToSave)
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditing) "Update" else "Save")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && player != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Player") },
            text = { Text("Are you sure you want to delete ${player.firstName} ${player.lastName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(player)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
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

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    MaterialTheme {
        PlayersScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerCardPreview() {
    MaterialTheme {
        PlayerCard(
            player = Player(
                id = 1,
                firstName = "LeBron",
                lastName = "James",
                heightCm = 206,
                wingspanCm = 214,
                primaryHand = PrimaryHand.RIGHT,
                dateOfBirth = LocalDate.of(1984, 12, 30),
                notes = "4x NBA Champion"
            ),
            onEditClick = {},
            onClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}