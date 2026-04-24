package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.models.Template
import com.markduenas.scorekeeper.presentation.viewmodel.HomeViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: HomeViewModel = getScreenModel()
        val state by viewModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scorekeeper", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { navigator.push(MoreAppsScreen()) }) {
                            Icon(Icons.Default.Info, contentDescription = "More Apps")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { navigator.push(NewScoreboardScreen()) }) {
                    Icon(Icons.Default.Add, contentDescription = "New Scoreboard")
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Quick Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    TemplatesSection(
                        categories = viewModel.templateCategories,
                        onSelect = { template -> navigator.push(NewScoreboardScreen(template.id)) }
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (state.recentScoreboards.isNotEmpty()) {
                    item {
                        Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(state.recentScoreboards) { sb ->
                        ScoreboardCard(
                            scoreboard = sb,
                            onClick = { navigator.push(ScoreboardScreen(sb.id)) },
                            onDelete = { viewModel.deleteScoreboard(sb.id) }
                        )
                    }
                } else if (!state.isLoading) {
                    item { EmptyState() }
                }
            }
        }
    }
}

@Composable
fun TemplatesSection(categories: Map<String, List<Template>>, onSelect: (Template) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { (category, templates) ->
            Text(category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    SuggestionChip(onClick = { onSelect(template) }, label = { Text(template.name) })
                }
            }
        }
    }
}

@Composable
fun ScoreboardCard(scoreboard: Scoreboard, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Scoreboard?") },
            text = { Text("Permanently delete this scoreboard and all score history?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    scoreboard.name.ifEmpty { "Unnamed Scoreboard" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (scoreboard.participants.isNotEmpty()) {
                    Text(
                        scoreboard.participants.joinToString(" · ") { "${it.name}: ${formatScoreCompact(it.score)}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Text(
                    formatTimestamp(scoreboard.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🏆", style = MaterialTheme.typography.displayMedium)
        Text("No scoreboards yet", style = MaterialTheme.typography.titleMedium)
        Text("Tap + to create one or pick a template above",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatScoreCompact(score: Double): String =
    if (score == score.toLong().toDouble()) score.toLong().toString()
    else "%.1f".format(score)

fun formatTimestamp(epochMs: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.monthNumber}/${local.dayOfMonth} ${local.hour}:${local.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) { "" }
}
