package com.example.statstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statstracker.database.relation.GameWithTeams
import com.example.statstracker.database.repository.BasketballRepository
import com.example.statstracker.ui.viewmodel.DashboardViewModel
import com.example.statstracker.ui.viewmodel.RecentResultData
import com.example.statstracker.ui.viewmodel.TeamAveragesData
import com.example.statstracker.ui.viewmodel.TopPerformerData
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    repository: BasketballRepository,
    onNavigateToGame: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember { DashboardViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // No club teams prompt
        if (!uiState.hasOurTeams) {
            item {
                NoClubTeamsBanner()
            }
        }

        // Top Performer
        if (uiState.topPerformer != null) {
            item {
                InsightCard(
                    title = "Top Performer",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    TopPerformerContent(data = uiState.topPerformer!!)
                }
            }
        }

        // Recent Results
        if (uiState.recentResults.isNotEmpty()) {
            item {
                InsightCard(
                    title = "Recent Results",
                    icon = Icons.Default.History,
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentResults.take(3).forEach { result ->
                            RecentResultItem(
                                data = result,
                                onClick = { onNavigateToGame(result.gameWithTeams.game.id) }
                            )
                        }
                    }
                }
            }
        }

        // Next Game
        if (uiState.nextGame != null) {
            item {
                InsightCard(
                    title = "Next Game",
                    icon = Icons.Default.Schedule,
                    iconTint = MaterialTheme.colorScheme.secondary
                ) {
                    NextGameContent(
                        game = uiState.nextGame!!,
                        onClick = { onNavigateToGame(uiState.nextGame!!.game.id) }
                    )
                }
            }
        }

        // Team Averages
        uiState.teamAverages.forEach { averages ->
            item(key = "avg_${averages.team.id}") {
                InsightCard(
                    title = averages.team.name,
                    subtitle = "Season averages · ${averages.gamesPlayed} GP",
                    icon = Icons.Default.Groups,
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    TeamAveragesContent(data = averages)
                }
            }
        }

        // Bottom spacer so FABs don't overlap last card
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// No club teams banner
// ─────────────────────────────────────────────────────────────

@Composable
private fun NoClubTeamsBanner() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Column {
                Text(
                    text = "No club teams set up",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Open a team and enable \"Our club team\" to see insights here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable card shell
// ─────────────────────────────────────────────────────────────

@Composable
private fun InsightCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Top Performer
// ─────────────────────────────────────────────────────────────

@Composable
private fun TopPerformerContent(data: TopPerformerData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = data.player.firstName.first().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${data.player.firstName} ${data.player.lastName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = data.teamName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBubble(label = "PPG", value = "%.1f".format(data.stats.pointsPerGame))
            StatBubble(label = "RPG", value = "%.1f".format(data.stats.reboundsPerGame))
            StatBubble(label = "APG", value = "%.1f".format(data.stats.assistsPerGame))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Recent Results
// ─────────────────────────────────────────────────────────────

@Composable
private fun RecentResultItem(data: RecentResultData, onClick: () -> Unit) {
    val gwt = data.gameWithTeams
    val ourTeam = if (data.ourTeamIsHome) gwt.homeTeam else gwt.awayTeam
    val opponent = if (data.ourTeamIsHome) gwt.awayTeam else gwt.homeTeam
    val formatter = DateTimeFormatter.ofPattern("MMM d")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // W/L chip
        val won = data.won
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (won) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(width = 28.dp, height = 24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (won) "W" else "L",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (won) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Teams
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ourTeam.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "vs ${opponent.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Score
        Text(
            text = "${data.ourScore} – ${data.opponentScore}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Date
        Text(
            text = gwt.game.date.format(formatter),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Next Game
// ─────────────────────────────────────────────────────────────

@Composable
private fun NextGameContent(game: GameWithTeams, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.homeTeam.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "vs ${game.awayTeam.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = game.game.date.format(formatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                if (!game.game.place.isNullOrBlank()) {
                    Text(
                        text = game.game.place,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Team Averages
// ─────────────────────────────────────────────────────────────

@Composable
private fun TeamAveragesContent(data: TeamAveragesData) {
    val stats = listOf(
        Triple("PPG", "%.1f".format(data.pointsPerGame), MaterialTheme.colorScheme.primary),
        Triple("RPG", "%.1f".format(data.reboundsPerGame), MaterialTheme.colorScheme.secondary),
        Triple("APG", "%.1f".format(data.assistsPerGame), MaterialTheme.colorScheme.tertiary),
        Triple("FG%", "%.0f%%".format(data.fieldGoalPercentage * 100), MaterialTheme.colorScheme.primary),
        Triple("3P%", "%.0f%%".format(data.threePointPercentage * 100), MaterialTheme.colorScheme.secondary),
        Triple("FT%", "%.0f%%".format(data.freeThrowPercentage * 100), MaterialTheme.colorScheme.tertiary)
    )

    // 2 rows × 3 columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value, tint) ->
                    StatTile(label = label, value = value, tint = tint, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shared stat components
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatBubble(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
