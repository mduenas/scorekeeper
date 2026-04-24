package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.scorekeeper.data.BuiltInTemplates
import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import kotlin.random.Random

val playerColors = listOf(
    "#2196F3", "#F44336", "#4CAF50", "#FF9800",
    "#9C27B0", "#00BCD4", "#FF5722", "#607D8B"
)

fun generateId(): String =
    (1..16).map { "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)] }.joinToString("")

@OptIn(ExperimentalMaterial3Api::class)
class NewScoreboardScreen(private val templateId: String? = null) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: ScorekeeperRepository = koinInject()
        val template = templateId?.let { id -> BuiltInTemplates.all.firstOrNull { it.id == id } }

        var name by remember { mutableStateOf("") }
        var players by remember {
            mutableStateOf((1..(template?.defaultParticipantCount ?: 2)).map { "Player $it" })
        }
        var scoringMode by remember { mutableStateOf(template?.scoringMode ?: ScoringMode.HIGHEST_WINS) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(template?.name ?: "New Scoreboard") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 100.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Scoreboard name (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Players", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                itemsIndexed(players) { index, player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = player,
                            onValueChange = { newName ->
                                players = players.toMutableList().also { it[index] = newName }
                            },
                            label = { Text("Player ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (players.size > 1) {
                            IconButton(onClick = {
                                players = players.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { players = players + "Player ${players.size + 1}" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Player")
                    }
                }
                item {
                    Text("Win Condition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Column {
                        listOf(
                            ScoringMode.HIGHEST_WINS to "Highest score wins",
                            ScoringMode.LOWEST_WINS to "Lowest score wins",
                            ScoringMode.NO_WINNER to "No winner tracking"
                        ).forEach { (mode, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = scoringMode == mode, onClick = { scoringMode = mode })
                                Text(label)
                            }
                        }
                    }
                }
                item {
                    val now = Clock.System.now().toEpochMilliseconds()
                    Button(
                        onClick = {
                            val scoreboardId = generateId()
                            val participants = players.mapIndexed { i, playerName ->
                                Participant(
                                    id = generateId(),
                                    scoreboardId = scoreboardId,
                                    name = playerName.ifBlank { "Player ${i + 1}" },
                                    sortOrder = i,
                                    color = playerColors[i % playerColors.size]
                                )
                            }
                            val scoreboard = Scoreboard(
                                id = scoreboardId,
                                name = name,
                                createdAt = now,
                                updatedAt = now,
                                templateId = templateId,
                                scoringMode = scoringMode,
                                defaultIncrement = template?.defaultIncrement ?: 1.0,
                                customIncrements = template?.customIncrements ?: emptyList(),
                                structureType = template?.structureType ?: StructureType.NONE,
                                structureLabel = template?.structureLabel ?: "Round",
                                participants = participants
                            )
                            repository.saveScoreboard(scoreboard)
                            navigator.replace(ScoreboardScreen(scoreboardId))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Start Game", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
