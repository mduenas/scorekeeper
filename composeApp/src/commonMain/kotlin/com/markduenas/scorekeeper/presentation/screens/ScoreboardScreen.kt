package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
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
import com.markduenas.scorekeeper.data.shareText
import com.markduenas.scorekeeper.presentation.components.CustomScoreDialog
import com.markduenas.scorekeeper.presentation.components.IncrementButtonsRow
import com.markduenas.scorekeeper.presentation.components.ParticipantCard
import com.markduenas.scorekeeper.presentation.components.formatScore
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import com.markduenas.scorekeeper.currentTimeMs
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
        var showSaveTemplateDialog by remember { mutableStateOf(false) }
        var showRenameDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        // Show feedback when template is saved or Firebase share fails
        LaunchedEffect(state.templateSaved, state.templateShareError) {
            when {
                state.templateSaved && state.templateShareError == null -> {
                    snackbarHostState.showSnackbar("Template saved!")
                    viewModel.clearTemplateSaved()
                }
                state.templateSaved && state.templateShareError != null -> {
                    snackbarHostState.showSnackbar("Template saved! Community sharing unavailable offline.")
                    viewModel.clearTemplateSaved()
                }
            }
        }

        val scoreboard = state.scoreboard
        if (scoreboard == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // Winner dialog
        if (state.showWinnerDialog && state.winner != null) {
            WinnerDialog(
                winner = state.winner!!,
                onContinue = { viewModel.dismissWinnerDialog() },
                onNewGame = {
                    viewModel.dismissWinnerDialog()
                    navigator.popUntilRoot()
                }
            )
        }

        customScoreTarget?.let { participant ->
            CustomScoreDialog(
                participant = participant,
                onApplyDelta = { delta -> viewModel.adjustScore(participant, delta) },
                onSetScore = { score -> viewModel.setScore(participant, score) },
                onDismiss = { customScoreTarget = null }
            )
        }

        if (showSaveTemplateDialog) {
            SaveTemplateDialog(
                defaultName = scoreboard.name.ifEmpty { "My Template" },
                onSave = { templateName, share ->
                    viewModel.saveAsTemplate(templateName, share)
                    showSaveTemplateDialog = false
                },
                onDismiss = { showSaveTemplateDialog = false }
            )
        }

        if (showRenameDialog) {
            RenameScoreboardDialog(
                currentName = scoreboard.name,
                onRename = { newName ->
                    viewModel.renameScoreboard(newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                DropdownMenuItem(
                                    text = { Text("Share Scores") },
                                    onClick = {
                                        showMenu = false
                                        val now = Instant.fromEpochMilliseconds(currentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault())
                                        val dateStr = "${now.year}-${now.monthNumber.toString().padStart(2,'0')}-${now.dayOfMonth.toString().padStart(2,'0')}"
                                        val sorted = when (scoreboard.scoringMode) {
                                            ScoringMode.HIGHEST_WINS -> scoreboard.participants.sortedByDescending { it.score }
                                            ScoringMode.LOWEST_WINS -> scoreboard.participants.sortedBy { it.score }
                                            ScoringMode.NO_WINNER -> scoreboard.participants
                                        }
                                        val lines = sorted.mapIndexed { i, p ->
                                            "${i + 1}. ${p.name} — ${formatScore(p.score)} pts"
                                        }.joinToString("\n")
                                        val shareContent = buildString {
                                            appendLine("🏆 ${scoreboard.name.ifEmpty { "Scoreboard" }}")
                                            appendLine(dateStr)
                                            appendLine()
                                            appendLine(lines)
                                            appendLine()
                                            append("Shared from Scorr")
                                        }
                                        shareText(shareContent)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as Template") },
                                    onClick = { showMenu = false; showSaveTemplateDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Scoreboard") },
                                    onClick = { showMenu = false; showRenameDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
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

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth >= maxHeight
                val onAdjust: (Participant, Double) -> Unit = { p, amt -> viewModel.adjustScore(p, amt) }
                if (isLandscape) {
                    LandscapeScoreLayout(
                        scoreboard = scoreboard,
                        leader = leader,
                        padding = padding,
                        onAdjust = onAdjust,
                        onLongPress = { p -> customScoreTarget = p }
                    )
                } else {
                    when {
                        scoreboard.participants.size <= 2 -> LargeScoreLayout(
                            scoreboard = scoreboard, leader = leader, padding = padding,
                            onAdjust = onAdjust,
                            onLongPress = { p -> customScoreTarget = p }
                        )
                        scoreboard.participants.size <= 6 -> GridScoreLayout(
                            scoreboard = scoreboard, leader = leader, padding = padding,
                            onAdjust = onAdjust,
                            onLongPress = { p -> customScoreTarget = p }
                        )
                        else -> ListScoreLayout(
                            scoreboard = scoreboard, leader = leader, padding = padding,
                            onAdjust = onAdjust,
                            onLongPress = { p -> customScoreTarget = p }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WinnerDialog(
    winner: Participant,
    onContinue: () -> Unit,
    onNewGame: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        icon = {
            Text("🏆", fontSize = 48.sp)
        },
        title = {
            Text(
                text = "${winner.name} Wins!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Final score: ${formatScore(winner.score)}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onNewGame) {
                Text("New Game")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onContinue) {
                Text("Continue")
            }
        }
    )
}

@Composable
fun LandscapeScoreLayout(
    scoreboard: Scoreboard,
    leader: Participant?,
    padding: PaddingValues,
    onAdjust: (Participant, Double) -> Unit,
    onLongPress: (Participant) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left column: game info
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scoreboard.name.ifEmpty { "Scoreboard" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (scoreboard.structureType != StructureType.NONE) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${scoreboard.structureLabel} ${scoreboard.currentStructureIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (leader != null && scoreboard.scoringMode != ScoringMode.NO_WINNER) {
                Spacer(Modifier.height(12.dp))
                Text("👑 Leading", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = leader.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Right column: participant cards in horizontal row
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(scoreboard.participants) { participant ->
                ParticipantCard(
                    participant = participant,
                    increment = scoreboard.defaultIncrement,
                    isLeader = leader?.id == participant.id && scoreboard.scoringMode != ScoringMode.NO_WINNER,
                    onIncrement = { onAdjust(participant, scoreboard.defaultIncrement) },
                    onDecrement = { onAdjust(participant, -scoreboard.defaultIncrement) },
                    onLongPress = { onLongPress(participant) },
                    customIncrements = scoreboard.customIncrements,
                    onAdjust = if (scoreboard.customIncrements.isNotEmpty()) { amt -> onAdjust(participant, amt) } else null,
                    modifier = Modifier.width(180.dp).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun LargeScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onAdjust: (Participant, Double) -> Unit, onLongPress: (Participant) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        scoreboard.participants.forEach { participant ->
            ParticipantCard(
                participant = participant,
                increment = scoreboard.defaultIncrement,
                isLeader = leader?.id == participant.id && scoreboard.scoringMode != ScoringMode.NO_WINNER,
                onIncrement = { onAdjust(participant, scoreboard.defaultIncrement) },
                onDecrement = { onAdjust(participant, -scoreboard.defaultIncrement) },
                onLongPress = { onLongPress(participant) },
                customIncrements = scoreboard.customIncrements,
                onAdjust = if (scoreboard.customIncrements.isNotEmpty()) { amt -> onAdjust(participant, amt) } else null,
                modifier = Modifier.weight(1f).fillMaxHeight(0.85f)
            )
        }
    }
}

@Composable
fun GridScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onAdjust: (Participant, Double) -> Unit, onLongPress: (Participant) -> Unit
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
                onIncrement = { onAdjust(participant, scoreboard.defaultIncrement) },
                onDecrement = { onAdjust(participant, -scoreboard.defaultIncrement) },
                onLongPress = { onLongPress(participant) },
                customIncrements = scoreboard.customIncrements,
                onAdjust = if (scoreboard.customIncrements.isNotEmpty()) { amt -> onAdjust(participant, amt) } else null
            )
        }
    }
}

@Composable
fun ListScoreLayout(
    scoreboard: Scoreboard, leader: Participant?, padding: PaddingValues,
    onAdjust: (Participant, Double) -> Unit, onLongPress: (Participant) -> Unit
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
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
                        if (scoreboard.customIncrements.isEmpty()) {
                            FilledTonalIconButton(onClick = { onAdjust(participant, -scoreboard.defaultIncrement) }) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(4.dp))
                            FilledTonalIconButton(onClick = { onAdjust(participant, scoreboard.defaultIncrement) }) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (scoreboard.customIncrements.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        IncrementButtonsRow(
                            increments = scoreboard.customIncrements,
                            onAdjust = { amt -> onAdjust(participant, amt) },
                            onLongPress = { onLongPress(participant) },
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveTemplateDialog(
    defaultName: String,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var shareWithCommunity by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Give this setup a name to reuse it later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = shareWithCommunity,
                        onCheckedChange = { shareWithCommunity = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Share with community", style = MaterialTheme.typography.bodyMedium)
                        Text("Others can use this template (pending review)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), shareWithCommunity) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameScoreboardDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Scoreboard") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Scoreboard name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onRename(name.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
