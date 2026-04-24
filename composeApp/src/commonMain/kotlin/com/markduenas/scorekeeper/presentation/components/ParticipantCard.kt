package com.markduenas.scorekeeper.presentation.components

import androidx.compose.foundation.BorderStroke
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

@Composable
fun ParticipantCard(
    participant: Participant,
    increment: Double,
    isLeader: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Use a simple color based on the participant's color field (fallback to primary container)
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

fun formatScore(score: Double): String {
    return if (score == score.toLong().toDouble()) {
        score.toLong().toString()
    } else {
        String.format("%.1f", score)
    }
}
