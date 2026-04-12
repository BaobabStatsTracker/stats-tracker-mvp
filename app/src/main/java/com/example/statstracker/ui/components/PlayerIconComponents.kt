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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun PlayerIcon(
    jerseyNumber: Int,
    playerName: String,
    isOnCourt: Boolean,
    size: Dp = if (isOnCourt) 56.dp else 40.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
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
                .border(2.dp, accentColor, CircleShape)
        ) {
            Text(
                text = if (jerseyNumber > 0) "$jerseyNumber" else "?",
                color = Color.White,
                fontSize = if (isOnCourt) 20.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = playerName,
            fontSize = if (isOnCourt) 10.sp else 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

data class PlayerIconPosition(
    val playerId: Long,
    val center: Offset
)

@Composable
fun DraggablePlayerIcon(
    playerId: Long,
    jerseyNumber: Int,
    playerName: String,
    isOnCourt: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    allPositions: List<PlayerIconPosition>,
    onSwap: (fromId: Long, toId: Long) -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var myCenter by remember { mutableStateOf(Offset.Zero) }

    val size = if (isOnCourt) 56.dp else 40.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(if (isDragging) 10f else 0f)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInParent()
                myCenter = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
            }
            .pointerInput(playerId) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // Find drop target
                        val draggedCenter = Offset(
                            myCenter.x + offsetX,
                            myCenter.y + offsetY
                        )
                        val threshold = 60f
                        val target = allPositions
                            .filter { it.playerId != playerId }
                            .minByOrNull { (draggedCenter - it.center).getDistance() }

                        if (target != null && (draggedCenter - target.center).getDistance() < threshold) {
                            onSwap(playerId, target.playerId)
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        PlayerIcon(
            jerseyNumber = jerseyNumber,
            playerName = playerName,
            isOnCourt = isOnCourt,
            size = size,
            accentColor = if (isDragging) MaterialTheme.colorScheme.tertiary else accentColor,
            onClick = onClick
        )
    }
}
