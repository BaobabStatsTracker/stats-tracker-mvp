package com.example.statstracker.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.statstracker.database.entity.Team
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
                    // Ensure teams exist
                    val existingTeams = repository.getAllTeams()
                    val teamMap = if (existingTeams.isEmpty()) {
                        val teams = listOf(
                            Team(name = "Los Angeles Lakers", notes = "Purple and Gold, 17 NBA Championships. Home arena: Crypto.com Arena"),
                            Team(name = "Boston Celtics", notes = "Most decorated franchise with 18 NBA Championships. Home arena: TD Garden"),
                            Team(name = "Golden State Warriors", notes = "Dynasty of the 2010s-2020s, 7 NBA Championships. Home arena: Chase Center"),
                            Team(name = "Milwaukee Bucks", notes = "2021 NBA Champions, home of Giannis. Home arena: Fiserv Forum"),
                            Team(name = "Denver Nuggets", notes = "2023 NBA Champions, home of Nikola Jokic. Home arena: Ball Arena")
                        )
                        teams.associate { it.name to repository.insertTeam(it) }
                    } else {
                        existingTeams.associate { it.name to it.id }
                    }

                    data class SamplePlayerData(
                        val firstName: String,
                        val lastName: String,
                        val heightCm: Int,
                        val wingspanCm: Int,
                        val hand: PrimaryHand,
                        val dob: LocalDate,
                        val notes: String,
                        val jersey: Int
                    )

                    val playersByTeam = mapOf(
                        "Los Angeles Lakers" to listOf(
                            SamplePlayerData("LeBron", "James", 206, 214, PrimaryHand.RIGHT, LocalDate.of(1984, 12, 30), "4x NBA Champion, 4x MVP", 23),
                            SamplePlayerData("Anthony", "Davis", 208, 227, PrimaryHand.RIGHT, LocalDate.of(1993, 3, 11), "8x All-Star, 2020 NBA Champion", 3),
                            SamplePlayerData("Austin", "Reaves", 196, 201, PrimaryHand.RIGHT, LocalDate.of(1998, 5, 29), "Undrafted guard, fan favorite", 15),
                            SamplePlayerData("D'Angelo", "Russell", 193, 198, PrimaryHand.RIGHT, LocalDate.of(1996, 2, 23), "2019 All-Star", 1),
                            SamplePlayerData("Rui", "Hachimura", 203, 213, PrimaryHand.RIGHT, LocalDate.of(1998, 2, 8), "First Japanese-born NBA lottery pick", 28),
                            SamplePlayerData("Jarred", "Vanderbilt", 206, 216, PrimaryHand.RIGHT, LocalDate.of(1999, 4, 3), "Elite rebounder and defender", 2),
                            SamplePlayerData("Gabe", "Vincent", 191, 198, PrimaryHand.RIGHT, LocalDate.of(1996, 6, 14), "2023 NBA Finals starter with Miami", 7),
                            SamplePlayerData("Taurean", "Prince", 201, 211, PrimaryHand.RIGHT, LocalDate.of(1994, 3, 22), "Versatile veteran forward", 12),
                            SamplePlayerData("Jaxson", "Hayes", 211, 224, PrimaryHand.RIGHT, LocalDate.of(2000, 5, 23), "Athletic rim-running center", 11),
                            SamplePlayerData("Christian", "Wood", 208, 221, PrimaryHand.RIGHT, LocalDate.of(1995, 9, 27), "Skilled stretch big man", 35),
                            SamplePlayerData("Cam", "Reddish", 203, 213, PrimaryHand.RIGHT, LocalDate.of(1999, 9, 1), "Former top-10 pick, athletic wing", 5),
                            SamplePlayerData("Max", "Christie", 196, 203, PrimaryHand.RIGHT, LocalDate.of(2003, 2, 10), "Young developing guard", 10),
                            SamplePlayerData("Spencer", "Dinwiddie", 196, 201, PrimaryHand.RIGHT, LocalDate.of(1993, 4, 6), "Veteran playmaker", 26),
                            SamplePlayerData("Jalen", "Hood-Schifino", 188, 196, PrimaryHand.RIGHT, LocalDate.of(2003, 6, 19), "2023 first-round pick from Indiana", 0),
                            SamplePlayerData("Maxwell", "Lewis", 196, 213, PrimaryHand.RIGHT, LocalDate.of(2003, 4, 12), "2023 second-round pick, lengthy wing", 21)
                        ),
                        "Boston Celtics" to listOf(
                            SamplePlayerData("Jayson", "Tatum", 203, 208, PrimaryHand.RIGHT, LocalDate.of(1998, 3, 3), "3x All-NBA, franchise cornerstone", 0),
                            SamplePlayerData("Jaylen", "Brown", 198, 211, PrimaryHand.RIGHT, LocalDate.of(1996, 10, 24), "2024 Finals MVP", 7),
                            SamplePlayerData("Jrue", "Holiday", 191, 198, PrimaryHand.RIGHT, LocalDate.of(1990, 6, 12), "2021 NBA Champion, elite defender", 4),
                            SamplePlayerData("Derrick", "White", 193, 198, PrimaryHand.RIGHT, LocalDate.of(1994, 7, 2), "Two-way guard, 2023 All-Defense", 9),
                            SamplePlayerData("Kristaps", "Porzingis", 221, 231, PrimaryHand.RIGHT, LocalDate.of(1995, 8, 2), "Unicorn, elite rim protector and shooter", 8),
                            SamplePlayerData("Al", "Horford", 206, 218, PrimaryHand.RIGHT, LocalDate.of(1986, 6, 3), "5x All-Star, veteran leader", 42),
                            SamplePlayerData("Payton", "Pritchard", 185, 191, PrimaryHand.RIGHT, LocalDate.of(1998, 1, 28), "Sharpshooter off the bench", 11),
                            SamplePlayerData("Sam", "Hauser", 201, 208, PrimaryHand.RIGHT, LocalDate.of(1997, 12, 8), "Elite 3-point specialist", 30),
                            SamplePlayerData("Luke", "Kornet", 218, 224, PrimaryHand.RIGHT, LocalDate.of(1995, 7, 15), "Shot-blocking backup center", 40),
                            SamplePlayerData("Oshae", "Brissett", 198, 213, PrimaryHand.RIGHT, LocalDate.of(1998, 6, 20), "Versatile forward, strong defender", 12),
                            SamplePlayerData("Dalano", "Banton", 201, 208, PrimaryHand.LEFT, LocalDate.of(1999, 9, 11), "Long playmaking guard", 45),
                            SamplePlayerData("Lamar", "Stevens", 201, 211, PrimaryHand.RIGHT, LocalDate.of(1997, 9, 9), "Energetic two-way forward", 17),
                            SamplePlayerData("Svi", "Mykhailiuk", 201, 193, PrimaryHand.RIGHT, LocalDate.of(1997, 6, 10), "Ukrainian sharpshooter", 14),
                            SamplePlayerData("Jaden", "Springer", 193, 201, PrimaryHand.RIGHT, LocalDate.of(2002, 9, 25), "Defensive-minded young guard", 25),
                            SamplePlayerData("Neemias", "Queta", 213, 224, PrimaryHand.RIGHT, LocalDate.of(1999, 7, 13), "Portuguese center, rim protector", 88)
                        ),
                        "Golden State Warriors" to listOf(
                            SamplePlayerData("Stephen", "Curry", 188, 193, PrimaryHand.RIGHT, LocalDate.of(1988, 3, 14), "Greatest shooter ever, 4x Champion, 2x MVP", 30),
                            SamplePlayerData("Klay", "Thompson", 198, 203, PrimaryHand.RIGHT, LocalDate.of(1990, 2, 8), "Splash Brother, 4x Champion", 11),
                            SamplePlayerData("Andrew", "Wiggins", 201, 213, PrimaryHand.RIGHT, LocalDate.of(1995, 2, 23), "2022 All-Star, 2022 Champion", 22),
                            SamplePlayerData("Draymond", "Green", 198, 213, PrimaryHand.RIGHT, LocalDate.of(1990, 3, 4), "4x Champion, DPOY, elite playmaker", 23),
                            SamplePlayerData("Kevon", "Looney", 206, 218, PrimaryHand.RIGHT, LocalDate.of(1996, 2, 6), "Ironman, 4x Champion, elite rebounder", 5),
                            SamplePlayerData("Jonathan", "Kuminga", 201, 211, PrimaryHand.RIGHT, LocalDate.of(2002, 10, 6), "Athletic young forward with star potential", 0),
                            SamplePlayerData("Moses", "Moody", 196, 208, PrimaryHand.RIGHT, LocalDate.of(2002, 5, 31), "3-and-D wing, 2022 Champion", 4),
                            SamplePlayerData("Brandin", "Podziemski", 196, 198, PrimaryHand.LEFT, LocalDate.of(2003, 2, 25), "2024 All-Rookie selection", 2),
                            SamplePlayerData("Chris", "Paul", 183, 188, PrimaryHand.RIGHT, LocalDate.of(1985, 5, 6), "Point God, 12x All-Star", 3),
                            SamplePlayerData("Gary", "Payton II", 191, 201, PrimaryHand.RIGHT, LocalDate.of(1992, 12, 1), "Elite perimeter defender, 2022 Champion", 8),
                            SamplePlayerData("Cory", "Joseph", 191, 196, PrimaryHand.RIGHT, LocalDate.of(1991, 8, 20), "Veteran backup point guard", 7),
                            SamplePlayerData("Dario", "Saric", 208, 213, PrimaryHand.RIGHT, LocalDate.of(1994, 4, 8), "Skilled Croatian big man", 20),
                            SamplePlayerData("Trayce", "Jackson-Davis", 206, 218, PrimaryHand.RIGHT, LocalDate.of(2000, 2, 22), "Energetic young big man", 32),
                            SamplePlayerData("Gui", "Santos", 198, 206, PrimaryHand.RIGHT, LocalDate.of(2002, 7, 1), "Brazilian wing prospect", 15),
                            SamplePlayerData("Lester", "Quinones", 193, 201, PrimaryHand.RIGHT, LocalDate.of(2000, 7, 14), "Two-way guard", 25)
                        ),
                        "Milwaukee Bucks" to listOf(
                            SamplePlayerData("Giannis", "Antetokounmpo", 211, 221, PrimaryHand.RIGHT, LocalDate.of(1994, 12, 6), "Greek Freak, 2x MVP, 2021 Champion", 34),
                            SamplePlayerData("Damian", "Lillard", 188, 196, PrimaryHand.RIGHT, LocalDate.of(1990, 7, 15), "7x All-Star, Dame Time", 0),
                            SamplePlayerData("Khris", "Middleton", 201, 208, PrimaryHand.RIGHT, LocalDate.of(1991, 8, 12), "3x All-Star, 2021 Champion", 22),
                            SamplePlayerData("Brook", "Lopez", 213, 218, PrimaryHand.RIGHT, LocalDate.of(1988, 4, 1), "All-Star center, elite shot blocker", 11),
                            SamplePlayerData("Bobby", "Portis", 208, 216, PrimaryHand.RIGHT, LocalDate.of(1995, 2, 10), "Fan favorite, 2021 Champion", 9),
                            SamplePlayerData("Malik", "Beasley", 193, 198, PrimaryHand.RIGHT, LocalDate.of(1996, 11, 26), "Sharpshooting guard", 5),
                            SamplePlayerData("Pat", "Connaughton", 196, 198, PrimaryHand.RIGHT, LocalDate.of(1993, 1, 6), "Versatile veteran wing", 24),
                            SamplePlayerData("Cameron", "Payne", 185, 193, PrimaryHand.RIGHT, LocalDate.of(1994, 8, 8), "Speedy backup point guard", 1),
                            SamplePlayerData("MarJon", "Beauchamp", 198, 208, PrimaryHand.RIGHT, LocalDate.of(2001, 4, 15), "Athletic young wing", 3),
                            SamplePlayerData("Andre", "Jackson Jr", 198, 211, PrimaryHand.RIGHT, LocalDate.of(2001, 9, 11), "Versatile defensive wing", 44),
                            SamplePlayerData("Thanasis", "Antetokounmpo", 198, 213, PrimaryHand.RIGHT, LocalDate.of(1992, 7, 17), "Energy big brother of Giannis", 43),
                            SamplePlayerData("Jae", "Crowder", 198, 206, PrimaryHand.RIGHT, LocalDate.of(1990, 7, 6), "Tough veteran 3-and-D forward", 99),
                            SamplePlayerData("Robin", "Lopez", 213, 218, PrimaryHand.RIGHT, LocalDate.of(1988, 4, 1), "Twin brother of Brook, veteran center", 33),
                            SamplePlayerData("AJ", "Green", 193, 196, PrimaryHand.RIGHT, LocalDate.of(2000, 1, 10), "Sharpshooter from Northern Iowa", 20),
                            SamplePlayerData("Lindell", "Wigginton", 183, 191, PrimaryHand.RIGHT, LocalDate.of(1999, 1, 22), "Quick scoring guard", 6)
                        ),
                        "Denver Nuggets" to listOf(
                            SamplePlayerData("Nikola", "Jokic", 211, 216, PrimaryHand.RIGHT, LocalDate.of(1995, 2, 19), "3x MVP, 2023 Champion and Finals MVP", 15),
                            SamplePlayerData("Jamal", "Murray", 193, 198, PrimaryHand.RIGHT, LocalDate.of(1997, 2, 23), "Playoff performer, 2023 Champion", 27),
                            SamplePlayerData("Michael", "Porter Jr", 208, 218, PrimaryHand.RIGHT, LocalDate.of(1998, 6, 29), "Elite scorer, 2023 Champion", 1),
                            SamplePlayerData("Aaron", "Gordon", 203, 211, PrimaryHand.RIGHT, LocalDate.of(1995, 9, 16), "Athletic forward, 2023 Champion", 50),
                            SamplePlayerData("Kentavious", "Caldwell-Pope", 196, 203, PrimaryHand.RIGHT, LocalDate.of(1993, 2, 18), "3-and-D guard, 2x Champion", 5),
                            SamplePlayerData("Reggie", "Jackson", 191, 201, PrimaryHand.RIGHT, LocalDate.of(1990, 4, 16), "Veteran scoring guard", 7),
                            SamplePlayerData("Christian", "Braun", 198, 201, PrimaryHand.RIGHT, LocalDate.of(2001, 4, 17), "2023 Champion, tough competitor", 0),
                            SamplePlayerData("Peyton", "Watson", 203, 211, PrimaryHand.RIGHT, LocalDate.of(2003, 4, 18), "Athletic young wing with upside", 8),
                            SamplePlayerData("Zeke", "Nnaji", 211, 218, PrimaryHand.RIGHT, LocalDate.of(2001, 1, 9), "Young stretch big", 22),
                            SamplePlayerData("DeAndre", "Jordan", 211, 224, PrimaryHand.RIGHT, LocalDate.of(1988, 7, 21), "Veteran center, former All-Star", 6),
                            SamplePlayerData("Vlatko", "Cancar", 203, 208, PrimaryHand.RIGHT, LocalDate.of(1997, 4, 10), "Slovenian versatile forward", 31),
                            SamplePlayerData("Julian", "Strawther", 196, 203, PrimaryHand.RIGHT, LocalDate.of(2002, 3, 30), "Sharpshooter from Gonzaga", 4),
                            SamplePlayerData("Jalen", "Pickett", 196, 198, PrimaryHand.RIGHT, LocalDate.of(1999, 5, 30), "Crafty undrafted point guard", 14),
                            SamplePlayerData("Hunter", "Tyson", 203, 208, PrimaryHand.RIGHT, LocalDate.of(2000, 4, 17), "Stretch forward from Clemson", 9),
                            SamplePlayerData("Collin", "Gillespie", 188, 193, PrimaryHand.RIGHT, LocalDate.of(1999, 5, 9), "Tough guard from Villanova", 32)
                        )
                    )

                    playersByTeam.forEach { (teamName, players) ->
                        val teamId = teamMap[teamName]
                        players.forEach { data ->
                            val playerId = repository.insertPlayer(
                                Player(
                                    firstName = data.firstName,
                                    lastName = data.lastName,
                                    heightCm = data.heightCm,
                                    wingspanCm = data.wingspanCm,
                                    primaryHand = data.hand,
                                    dateOfBirth = data.dob,
                                    notes = data.notes
                                )
                            )
                            teamId?.let {
                                repository.addPlayerToTeam(playerId, it, data.jersey)
                            }
                        }
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
        topBar = {
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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
                                shape = MaterialTheme.shapes.medium,
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
        shape = MaterialTheme.shapes.medium,
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
        shape = MaterialTheme.shapes.medium,
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