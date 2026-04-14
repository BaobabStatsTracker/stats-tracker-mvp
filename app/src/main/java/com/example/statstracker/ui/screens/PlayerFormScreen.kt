package com.example.statstracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.model.PrimaryHand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class PlayerFormUiState(
    val firstName: String = "",
    val lastName: String = "",
    val heightText: String = "",
    val wingspanText: String = "",
    val selectedHand: PrimaryHand? = null,
    val birthDateText: String = "",
    val imageUrl: String = "",
    val notes: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}

class PlayerFormViewModel(
    private val playerId: Long?,
    private val repository: BasketballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerFormUiState(isEditing = playerId != null, isLoading = playerId != null))
    val uiState: StateFlow<PlayerFormUiState> = _uiState.asStateFlow()

    private val _savedPlayerId = MutableStateFlow<Long?>(null)
    val savedPlayerId: StateFlow<Long?> = _savedPlayerId.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        if (playerId != null) {
            loadPlayer(playerId)
        }
    }

    private fun loadPlayer(id: Long) {
        viewModelScope.launch {
            try {
                val player = repository.getPlayerById(id)
                if (player == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Player not found")
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    firstName = player.firstName,
                    lastName = player.lastName,
                    heightText = player.heightCm?.toString() ?: "",
                    wingspanText = player.wingspanCm?.toString() ?: "",
                    selectedHand = player.primaryHand,
                    birthDateText = player.dateOfBirth?.toString() ?: "",
                    imageUrl = player.image ?: "",
                    notes = player.notes ?: "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load player: ${e.message}"
                )
            }
        }
    }

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(firstName = value)
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(lastName = value)
    }

    fun updateHeightText(value: String) {
        _uiState.value = _uiState.value.copy(heightText = value.filter { it.isDigit() })
    }

    fun updateWingspanText(value: String) {
        _uiState.value = _uiState.value.copy(wingspanText = value.filter { it.isDigit() })
    }

    fun updatePrimaryHand(value: PrimaryHand?) {
        _uiState.value = _uiState.value.copy(selectedHand = value)
    }

    fun updateBirthDateText(value: String) {
        _uiState.value = _uiState.value.copy(birthDateText = value.filter { it.isDigit() || it == '-' })
    }

    fun updateImageUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageUrl = value)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun savePlayer() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                val parsedBirthDate = try {
                    if (state.birthDateText.isNotBlank()) LocalDate.parse(state.birthDateText) else null
                } catch (_: DateTimeParseException) {
                    null
                }

                if (playerId != null) {
                    repository.updatePlayer(
                        Player(
                            id = playerId,
                            firstName = state.firstName.trim(),
                            lastName = state.lastName.trim(),
                            heightCm = state.heightText.toIntOrNull(),
                            wingspanCm = state.wingspanText.toIntOrNull(),
                            primaryHand = state.selectedHand,
                            dateOfBirth = parsedBirthDate,
                            image = state.imageUrl.trim().ifBlank { null },
                            notes = state.notes.trim().ifBlank { null }
                        )
                    )
                    _savedPlayerId.value = playerId
                } else {
                    val newId = repository.insertPlayer(
                        Player(
                            firstName = state.firstName.trim(),
                            lastName = state.lastName.trim(),
                            heightCm = state.heightText.toIntOrNull(),
                            wingspanCm = state.wingspanText.toIntOrNull(),
                            primaryHand = state.selectedHand,
                            dateOfBirth = parsedBirthDate,
                            image = state.imageUrl.trim().ifBlank { null },
                            notes = state.notes.trim().ifBlank { null }
                        )
                    )
                    _savedPlayerId.value = newId
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save player: ${e.message}"
                )
            }
        }
    }

    fun deletePlayer() {
        val id = playerId ?: return
        viewModelScope.launch {
            try {
                val player = repository.getPlayerById(id) ?: return@launch
                repository.deletePlayer(player)
                _deleted.value = true
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete player: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFormScreen(
    playerId: Long?,
    repository: BasketballRepository,
    onNavigateBack: () -> Unit,
    onPlayerSaved: (Long) -> Unit,
    onPlayerDeleted: () -> Unit
) {
    val viewModel = remember { PlayerFormViewModel(playerId, repository) }
    val uiState by viewModel.uiState.collectAsState()
    val savedPlayerId by viewModel.savedPlayerId.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedPlayerId) {
        savedPlayerId?.let { onPlayerSaved(it) }
    }

    LaunchedEffect(deleted) {
        if (deleted) onPlayerDeleted()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Edit Player" else "New Player",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PlayerFormContent(
                    uiState = uiState,
                    onFirstNameChange = viewModel::updateFirstName,
                    onLastNameChange = viewModel::updateLastName,
                    onHeightChange = viewModel::updateHeightText,
                    onWingspanChange = viewModel::updateWingspanText,
                    onHandChange = viewModel::updatePrimaryHand,
                    onBirthDateChange = viewModel::updateBirthDateText,
                    onImageUrlChange = viewModel::updateImageUrl,
                    onNotesChange = viewModel::updateNotes,
                    onSave = viewModel::savePlayer,
                    onDelete = viewModel::deletePlayer,
                    onCancel = onNavigateBack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerFormContent(
    uiState: PlayerFormUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWingspanChange: (String) -> Unit,
    onHandChange: (PrimaryHand?) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var handDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.firstName.isBlank()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.lastName.isBlank()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.heightText,
                    onValueChange = onHeightChange,
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.wingspanText,
                    onValueChange = onWingspanChange,
                    label = { Text("Wingspan (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = handDropdownExpanded,
                onExpandedChange = { handDropdownExpanded = !handDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedHand?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primary Hand") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = handDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = handDropdownExpanded,
                    onDismissRequest = { handDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onHandChange(null)
                            handDropdownExpanded = false
                        }
                    )
                    PrimaryHand.entries.forEach { hand ->
                        DropdownMenuItem(
                            text = { Text(hand.name) },
                            onClick = {
                                onHandChange(hand)
                                handDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.birthDateText,
                onValueChange = onBirthDateChange,
                label = { Text("Date of Birth (yyyy-mm-dd)") },
                placeholder = { Text("1995-01-15") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = uiState.imageUrl,
                onValueChange = onImageUrlChange,
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isEditing) "Update" else "Save")
            }
        }
    }

}
