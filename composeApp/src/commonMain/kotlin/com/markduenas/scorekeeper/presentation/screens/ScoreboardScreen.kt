package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.scorekeeper.data.models.Participant
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.models.ScoringMode
import com.markduenas.scorekeeper.data.models.StructureType
import com.markduenas.scorekeeper.presentation.components.CustomScoreDialog
import com.markduenas.scorekeeper.presentation.components.ParticipantCard
import com.markduenas.scorekeeper.presentation.components.formatScore
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
class ScoreboardScreen(private val scoreboardId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ScoreboardViewModel = getScreenModel { parametersOf(scoreboardId) }
        val state by viewModel.uiState.collectAsState()
        var showMenu by remember { mutableStateOf(false) }
        var customScoreTarget by remember { mutableStateOf<Participant?>(null) }

        val scoreboard = state.scoreboard
        if (scoreboard == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        customScoreTarget?.let { participant ->
            CustomScoreDialog(
                participant = participant,
                onApplyDelta = { delta -> viewModel.adjustScore(participant, delta) },
                onSetScore = { score -> viewModel.setScore(participant, score) },
                onDismiss = { customScoreTarget = null }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(scoreboard.name.ifEmpty { "Scoreboard" }, fontWeight = FontWeight.Bold)
                            if (scoreboard.structureType != StructureType.NONE) {
                                Text(
                                    "${scoreboard.structureLabel} ${scoreboard.currentStructureIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (scoreboard.structureType != StructureType.NONE) {
                                    DropdownMenuItem(
                                        text = { Text("Next ${scoreboard.structureLabel}") },
                                        onClick = { viewModel.advanceStructure(); showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.SkipNext, null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    onClick = { navigator.push(HistoryScreen(scoreboardId)); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.History, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("End Game") },
                                    onClick = { viewModel.completeGame(); navigator.pop(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Flag, null) }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            val leader = when (scoreboard.scoringMode) {
                ScoringMode.HIGHEST_WINS -> scoreboard.participants.maxByOrNull { it.score }
                ScoringMode.LOWEST_WINS -> scoreboard.participants.minByOrNull { it.score }
                ScoringMode.NO_WINNER -> null
            }
            when {
                scoreboard.participants.size <= 2 -> LargeScoreLayout(
                    scoreboard = scoreboard, leader = leader, padding = padding,
                    onIncrement = { p -> viewModel.adjustScore(p, scoreboard.defaultIncrement) },
                    onDecrement = { p -> viewModel.adjustScore(p, -scoreboard.defaultIncrement) },
                    onLongPress = { p -> customScoreTarget = p }
                )
                scoreboard.participants.size <= 6 -> GridScoreLayout(
                    scoreboard = scoreboard, leader = leader, padding = padding,
                    onIncrement = { p -> viewModel.adjustScore(p, scoreboard.defaultIncrement) },
                    onDecrement = { p -> viewModel.adjustScore(p, -scoreboard.defaultIncrement) },
                    onLongPress = { p -> customScoreTarget = p }
                )
                else -> ListScoreLayout(
                    scoreboard = scoreboard, leader = leader, padding = padding,
                    onIncrement = { p -> viewModel.adjustScore(p, scoreboard.defaultIncrement) },
                    onDecrement = { p -> viewModel.adjustScore(p, -scoreboard.defaultIncrement) },
                    onLongPress = { p -> customScoreTarget = p }
                )
            }
        }
    }
}

@Composable
fun LargeScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onIncrement: (Participant) -> Unit, onDecrement: (Participant) -> Unit, onLongPress: (Participant) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        scoreboard.participants.forEach { participant ->
            ParticipantCard(
                participant = participant,
                increment = scoreboard.defaultIncrement,
                isLeader = leader?.id == participant.id && scoreboard.scoringMode != ScoringMode.NO_WINNER,
                onIncrement = { onIncrement(participant) },
                onDecrement = { onDecrement(participant) },
                onLongPress = { onLongPress(participant) },
                modifier = Modifier.weight(1f).fillMaxHeight(0.85f)
            )
        }
    }
}

@Composable
fun GridScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onIncrement: (Participant) -> Unit, onDecrement: (Participant) -> Unit, onLongPress: (Participant) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 8.dp,
            start = 8.dp, end = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(scoreboard.participants) { participant ->
            ParticipantCard(
                participant = participant,
                increment = scoreboard.defaultIncrement,
                isLeader = leader?.id == participant.id && scoreboard.scoringMode != ScoringMode.NO_WINNER,
                onIncrement = { onIncrement(participant) },
                onDecrement = { onDecrement(participant) },
                onLongPress = { onLongPress(participant) }
            )
        }
    }
}

@Composable
fun ListScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onIncrement: (Participant) -> Unit, onDecrement: (Participant) -> Unit, onLongPress: (Participant) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 8.dp,
            start = 8.dp, end = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(scoreboard.participants) { participant ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (leader?.id == participant.id && scoreboard.scoringMode != ScoringMode.NO_WINNER) {
                        Text("👑", modifier = Modifier.padding(end = 4.dp))
                    }
                    Text(participant.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        formatScore(participant.score),
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    FilledTonalIconButton(onClick = { onDecrement(participant) }) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = { onIncrement(participant) }) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
