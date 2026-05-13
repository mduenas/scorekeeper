package com.markduenas.scorekeeper.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markduenas.scorekeeper.data.models.Participant
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParticipantCard(
    participant: Participant,
    increment: Double,
    isLeader: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onLongPress: () -> Unit = {},
    customIncrements: List<Double> = emptyList(),
    onAdjust: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBorderColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.padding(4.dp),
        border = if (isLeader) BorderStroke(2.dp, cardBorderColor) else null,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isLeader) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLeader) {
                Text("👑", fontSize = 16.sp)
            }
            Text(
                text = participant.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = formatScore(participant.score),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (participant.score < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )

            if (customIncrements.isNotEmpty() && onAdjust != null) {
                // Multi-increment buttons row: long-press to subtract
                IncrementButtonsRow(
                    increments = customIncrements,
                    onAdjust = onAdjust,
                    onLongPress = onLongPress
                )
            } else {
                // Default single +/- buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(52.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(52.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "±${formatScore(increment)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IncrementButtonsRow(
    increments: List<Double>,
    onAdjust: (Double) -> Unit,
    onLongPress: () -> Unit = {},
    compact: Boolean = false
) {
    val buttonHeight = if (compact) 36.dp else 44.dp
    val fontSize = if (compact) 11.sp else 13.sp
    val hPad = if (compact) 6.dp else 8.dp

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        increments.forEach { inc ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                OutlinedButton(
                    onClick = { onAdjust(-inc) },
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 2.dp, bottomEnd = 2.dp)
                ) {
                    Text("-${formatScore(inc)}", fontSize = fontSize, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = { onAdjust(inc) },
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
                    shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                ) {
                    Text("+${formatScore(inc)}", fontSize = fontSize, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun formatScore(score: Double): String {
    if (score == score.toLong().toDouble()) return score.toLong().toString()
    val s = score.toString()
    val dot = s.indexOf('.')
    return if (dot < 0 || s.length <= dot + 2) s else s.substring(0, dot + 2)
}
