package com.example.statstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.statstracker.ui.theme.LocalAppColors
import kotlin.math.roundToInt

/**
 * Central coordination state for drag-and-drop player swaps.
 *
 * All positions are stored in **window coordinates** so that court and bench
 * icons (which live under different parent containers) share a single
 * coordinate space. A single instance is created per TeamHalf and passed
 * to every [DraggablePlayerIcon] inside it.
 *
 * The parent composable reads [isDragging] / [dragCenter] / [draggedInfo]
 * to render a floating ghost overlay that is NOT clipped by scroll containers.
 */
class DragDropState {
    // --- Drag lifecycle flags ---
    var isDragging by mutableStateOf(false)
    var draggedPlayerId by mutableLongStateOf(-1L)

    // Window-coordinate center of the icon when the drag started
    var startCenter by mutableStateOf(Offset.Zero)

    // Accumulated finger movement since drag start
    var currentOffset by mutableStateOf(Offset.Zero)

    // Visual info about the icon being dragged (so the ghost can render it)
    var draggedJersey by mutableIntStateOf(0)
    var draggedName by mutableStateOf("")
    var draggedIsOnCourt by mutableStateOf(true)
    var draggedAccentColor by mutableStateOf(Color.Transparent)

    // --- Position registry (window coords) for every icon in this half ---
    val positions = mutableStateMapOf<Long, Offset>()

    /** Current center of the dragged icon in window coordinates. */
    val dragCenter: Offset
        get() = startCenter + currentOffset

    /**
     * Closest icon to the current drag position (excluding the dragged icon
     * itself). Returns null if nothing is within [DROP_THRESHOLD_PX].
     */
    val nearestTargetId: Long?
        get() {
            if (!isDragging) return null
            val center = dragCenter
            var bestId: Long? = null
            var bestDist = Float.MAX_VALUE
            for ((id, pos) in positions) {
                if (id == draggedPlayerId) continue
                val dist = (center - pos).getDistance()
                if (dist < bestDist) {
                    bestDist = dist
                    bestId = id
                }
            }
            return if (bestDist <= DROP_THRESHOLD_PX) bestId else null
        }

    fun beginDrag(
        playerId: Long,
        jerseyNumber: Int,
        playerName: String,
        isOnCourt: Boolean,
        accentColor: Color
    ) {
        isDragging = true
        draggedPlayerId = playerId
        startCenter = positions[playerId] ?: Offset.Zero
        currentOffset = Offset.Zero
        draggedJersey = jerseyNumber
        draggedName = playerName
        draggedIsOnCourt = isOnCourt
        draggedAccentColor = accentColor
    }

    fun reset() {
        isDragging = false
        draggedPlayerId = -1L
        currentOffset = Offset.Zero
    }

    companion object {
        /** Max distance (px) between drag center and a target to count as a valid drop. */
        const val DROP_THRESHOLD_PX = 80f
    }
}

@Composable
fun PlayerIcon(
    jerseyNumber: Int,
    playerName: String,
    isOnCourt: Boolean,
    size: Dp = if (isOnCourt) 56.dp else 40.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(if (isOnCourt) 72.dp else 52.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .background(
                    color = if (isOnCourt) accentColor else accentColor.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                // Highlight border when this icon is the nearest drop target
                .border(
                    width = if (isHighlighted) 4.dp else 2.dp,
                    color = if (isHighlighted) LocalAppColors.current.playerHighlight else accentColor,
                    shape = CircleShape
                )
        ) {
            Text(
                text = if (jerseyNumber > 0) "$jerseyNumber" else "?",
                color = MaterialTheme.colorScheme.onPrimary,
                style = if (isOnCourt) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = playerName,
            style = if (isOnCourt) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * A player icon that supports drag-to-swap with other icons sharing the same
 * [DragDropState]. The actual dragged visual (ghost) is rendered by the parent
 * composable so it is never clipped by scroll containers.
 *
 * This composable:
 *  - Registers its window-coordinate center into [dragDropState.positions]
 *  - Detects drag gestures and updates [dragDropState]
 *  - Fades to 30 % opacity while being dragged (ghost renders above)
 *  - Shows a yellow highlight border when it is the nearest drop target
 */
@Composable
fun DraggablePlayerIcon(
    playerId: Long,
    jerseyNumber: Int,
    playerName: String,
    isOnCourt: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dragDropState: DragDropState,
    onSwap: (fromId: Long, toId: Long) -> Unit,
    onClick: () -> Unit
) {
    val size = if (isOnCourt) 56.dp else 40.dp
    val isBeingDragged = dragDropState.isDragging && dragDropState.draggedPlayerId == playerId
    val isDropTarget = dragDropState.nearestTargetId == playerId

    Box(
        modifier = Modifier
            // Fade the source icon while dragging — ghost overlay shows the moving copy
            .graphicsLayer { alpha = if (isBeingDragged) 0.3f else 1f }
            // Register window-coordinate center for hit-testing
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val center = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
                dragDropState.positions[playerId] = center
            }
            .pointerInput(playerId) {
                detectDragGestures(
                    onDragStart = {
                        dragDropState.beginDrag(
                            playerId = playerId,
                            jerseyNumber = jerseyNumber,
                            playerName = playerName,
                            isOnCourt = isOnCourt,
                            accentColor = accentColor
                        )
                    },
                    onDragEnd = {
                        // Resolve the drop: swap if a valid target is nearby
                        val targetId = dragDropState.nearestTargetId
                        if (targetId != null) {
                            onSwap(playerId, targetId)
                        }
                        dragDropState.reset()
                    },
                    onDragCancel = {
                        dragDropState.reset()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.currentOffset += dragAmount
                    }
                )
            }
    ) {
        PlayerIcon(
            jerseyNumber = jerseyNumber,
            playerName = playerName,
            isOnCourt = isOnCourt,
            size = size,
            accentColor = accentColor,
            isHighlighted = isDropTarget,
            onClick = onClick
        )
    }
}
