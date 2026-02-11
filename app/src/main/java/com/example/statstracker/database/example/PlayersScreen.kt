package com.example.statstracker.database.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.statstracker.database.BasketballDatabase
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.repository.BasketballRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Example ViewModel demonstrating how to use the BasketballRepository
 * in a real application with proper state management and coroutines.
 */
class PlayersViewModel(
    private val repository: BasketballRepository
) : ViewModel() {
    
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadPlayers()
    }
    
    private fun loadPlayers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _players.value = repository.getAllPlayers()
            } catch (e: Exception) {
                // Handle error (e.g., show snackbar, log error)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addPlayer(firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                val newPlayer = Player(
                    firstName = firstName,
                    lastName = lastName,
                    dateOfBirth = null // You could add date picker here
                )
                repository.insertPlayer(newPlayer)
                // Refresh the list
                loadPlayers()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
    
    fun searchPlayers(query: String) {
        if (query.isBlank()) {
            loadPlayers()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _players.value = repository.searchPlayersByName(query)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * ViewModelFactory for manual dependency injection
 */
class PlayersViewModelFactory(private val repository: BasketballRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayersViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Example Composable screen showing how to use the Room database
 * with Jetpack Compose and the repository pattern.
 * 
 * This demonstrates:
 * - Loading data from Room database
 * - Displaying data in a LazyColumn
 * - Adding new players
 * - Search functionality
 * - Loading states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen() {
    val context = LocalContext.current
    val database = BasketballDatabase.getInstance(context)
    val repository = BasketballRepository(database)
    
    val viewModel: PlayersViewModel = viewModel(
        factory = PlayersViewModelFactory(repository)
    )
    val players by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.searchPlayers(it)
            },
            label = { Text("Search players...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add player button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add New Player")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Players list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players) { player ->
                    PlayerCard(player = player)
                }
            }
        }
    }
    
    // Add player dialog
    if (showAddDialog) {
        AddPlayerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { firstName, lastName ->
                viewModel.addPlayer(firstName, lastName)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PlayerCard(player: Player) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${player.firstName} ${player.lastName}",
                style = MaterialTheme.typography.titleMedium
            )
            player.dateOfBirth?.let { birthDate ->
                Text(
                    text = "Born: $birthDate",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            player.heightCm?.let { height ->
                Text(
                    text = "Height: ${height}cm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Player") },
        text = {
            Column {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(firstName, lastName) },
                enabled = firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}