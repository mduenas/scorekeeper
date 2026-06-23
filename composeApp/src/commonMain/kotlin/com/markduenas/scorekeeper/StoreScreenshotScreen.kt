package com.markduenas.scorekeeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markduenas.scorekeeper.data.BuiltInTemplates
import com.markduenas.scorekeeper.data.models.Participant
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.models.ScoringMode
import com.markduenas.scorekeeper.data.models.StructureType
import com.markduenas.scorekeeper.presentation.components.ParticipantCard
import com.markduenas.scorekeeper.presentation.components.formatScore
import com.markduenas.scorekeeper.presentation.screens.GridScoreLayout
import com.markduenas.scorekeeper.presentation.screens.ScoreboardCard
import com.markduenas.scorekeeper.presentation.screens.TemplatesSection

private const val StoreScoreboardId = "store-scoreboard"

@Composable
fun StoreScreenshotScreen(screenshotName: String) {
    when (screenshotName.lowercase()) {
        "templates" -> StoreTemplatesShot()
        "history" -> StoreHistoryShot()
        "landscape" -> StoreLandscapeShot()
        else -> StoreScoreboardShot()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreScoreboardShot() {
    val scoreboard = demoScoreboard()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scoreboard.name, fontWeight = FontWeight.Bold)
                        Text("Set 3", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.padding(horizontal = 12.dp))
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(horizontal = 12.dp))
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.padding(horizontal = 12.dp))
                }
            )
        }
    ) { padding ->
        GridScoreLayout(
            scoreboard = scoreboard,
            leader = scoreboard.participants.first(),
            padding = padding,
            onAdjust = { _, _ -> },
            onLongPress = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreTemplatesShot() {
    val recents = listOf(
        demoScoreboard(name = "Family Game Night", participantScores = listOf(120.0, 115.0, 98.0, 84.0)),
        demoScoreboard(name = "Practice Match", participantScores = listOf(21.0, 19.0)),
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Scorr", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StoreHero(
                    title = "Track any game",
                    subtitle = "Quick templates, custom increments, and reusable scoreboards."
                )
            }
            item {
                Text("Quick Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TemplatesSection(categories = BuiltInTemplates.categories, onSelect = {})
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(recents) { scoreboard ->
                ScoreboardCard(scoreboard = scoreboard, onClick = {}, onDelete = {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreHistoryShot() {
    val events = listOf(
        HistoryItem("Alex", "+5", "85", "Score adjusted · 7:42"),
        HistoryItem("Jordan", "+10", "78", "Three-pointer bonus · 7:39"),
        HistoryItem("Mia", "-2", "72", "Correction · 7:35"),
        HistoryItem("Sam", "+5", "66", "Round 3 · 7:31"),
        HistoryItem("Alex", "+10", "80", "Round 3 · 7:28"),
        HistoryItem("Mia", "+5", "74", "Round 2 · 7:22"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Score History", fontWeight = FontWeight.Bold) },
                actions = {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 12.dp,
                bottom = 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StoreHero(
                    title = "Every point remembered",
                    subtitle = "Undo mistakes and review exactly how the game changed."
                )
            }
            items(events) { event ->
                HistoryPreviewRow(event)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreLandscapeShot() {
    val scoreboard = demoScoreboard(participantScores = listOf(85.0, 78.0, 72.0, 66.0))
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tournament Night", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StoreHero(
                title = "Built for the table",
                subtitle = "Landscape scoring keeps everyone visible.",
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scoreboard.participants) { participant ->
                    ParticipantCard(
                        participant = participant,
                        increment = scoreboard.defaultIncrement,
                        isLeader = participant.id == "p1",
                        onIncrement = {},
                        onDecrement = {},
                        customIncrements = scoreboard.customIncrements,
                        onAdjust = {},
                        modifier = Modifier
                            .width(190.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = {}, label = { Text("Templates") })
                    SuggestionChip(onClick = {}, label = { Text("Undo") })
                    SuggestionChip(onClick = {}, label = { Text("Share") })
                }
            }
        }
    }
}

@Composable
private fun HistoryPreviewRow(event: HistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(event.name.take(1), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.SemiBold)
                Text(event.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    event.delta,
                    fontWeight = FontWeight.Bold,
                    color = if (event.delta.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text("-> ${event.total}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private data class HistoryItem(
    val name: String,
    val delta: String,
    val total: String,
    val detail: String
)

private fun demoScoreboard(
    name: String = "Tournament Night",
    participantScores: List<Double> = listOf(85.0, 78.0, 72.0, 66.0)
): Scoreboard {
    val names = listOf("Alex", "Jordan", "Mia", "Sam", "Riley", "Taylor")
    val colors = listOf("#1565C0", "#2E7D32", "#C62828", "#6A1B9A", "#EF6C00", "#00838F")
    return Scoreboard(
        id = StoreScoreboardId,
        name = name,
        createdAt = 1_780_000_000_000,
        updatedAt = 1_780_000_120_000,
        scoringMode = ScoringMode.HIGHEST_WINS,
        defaultIncrement = 5.0,
        customIncrements = listOf(1.0, 5.0, 10.0),
        structureType = StructureType.SETS,
        structureLabel = "Set",
        currentStructureIndex = 2,
        participants = participantScores.mapIndexed { index, score ->
            Participant(
                id = "p${index + 1}",
                scoreboardId = StoreScoreboardId,
                name = names[index],
                score = score,
                color = colors[index],
                sortOrder = index
            )
        }
    )
}
