package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.scorekeeper.data.models.ScoreEvent
import com.markduenas.scorekeeper.presentation.components.formatScore
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
class HistoryScreen(private val scoreboardId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ScoreboardViewModel = getScreenModel { parametersOf(scoreboardId) }
        val state by viewModel.uiState.collectAsState()
        val scoreboard = state.scoreboard
        val participants = scoreboard?.participants?.associateBy { it.id } ?: emptyMap()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Score History") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (state.scoreEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No scoring history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp, start = 16.dp, end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.scoreEvents) { event ->
                        HistoryEventRow(
                            event = event,
                            participantName = participants[event.participantId]?.name ?: "Unknown"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEventRow(event: ScoreEvent, participantName: String) {
    val isUndone = event.undoneAt != null
    val deltaText = if (event.delta >= 0) "+${formatScore(event.delta)}" else formatScore(event.delta)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUndone) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participantName,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUndone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.actionType.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatTimestamp(event.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = deltaText,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isUndone -> MaterialTheme.colorScheme.onSurfaceVariant
                        event.delta > 0 -> MaterialTheme.colorScheme.primary
                        event.delta < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "-> ${formatScore(event.newScore)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isUndone) {
                Spacer(Modifier.width(8.dp))
                Text("(undone)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
