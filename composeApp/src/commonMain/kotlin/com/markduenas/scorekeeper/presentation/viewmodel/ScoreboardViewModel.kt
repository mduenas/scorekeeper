package com.markduenas.scorekeeper.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

data class ScoreboardUiState(
    val scoreboard: Scoreboard? = null,
    val scoreEvents: List<ScoreEvent> = emptyList(),
    val isLoading: Boolean = false,
    val winner: Participant? = null,
    val showWinnerDialog: Boolean = false
)

class ScoreboardViewModel(
    private val repository: ScorekeeperRepository,
    val scoreboardId: String
) : ScreenModel {

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState

    // Track winner IDs that the user has already dismissed so we don't re-show
    private val dismissedWinnerIds = mutableSetOf<String>()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scoreboard = repository.getScoreboard(scoreboardId)
            val events = repository.getScoreEvents(scoreboardId)
            val winner = detectWinner(scoreboard)
            val showDialog = winner != null && winner.id !in dismissedWinnerIds
            _uiState.value = ScoreboardUiState(
                scoreboard = scoreboard,
                scoreEvents = events,
                winner = winner,
                showWinnerDialog = showDialog
            )
        }
    }

    fun adjustScore(participant: Participant, delta: Double, actionType: ActionType = if (delta >= 0) ActionType.INCREMENT else ActionType.DECREMENT) {
        val sb = _uiState.value.scoreboard ?: return
        val rawScore = participant.score + delta
        val newScore = if (!sb.negativeScoresAllowed) maxOf(0.0, rawScore) else rawScore
        val event = ScoreEvent(
            id = generateId(),
            scoreboardId = scoreboardId,
            participantId = participant.id,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            previousScore = participant.score,
            newScore = newScore,
            delta = newScore - participant.score,
            actionType = actionType,
            structureIndex = sb.currentStructureIndex
        )
        screenModelScope.launch {
            repository.updateParticipantScore(participant.id, newScore, event)
            load()
        }
    }

    fun setScore(participant: Participant, newScore: Double) {
        adjustScore(participant, newScore - participant.score, ActionType.SET)
    }

    fun resetScore(participant: Participant) {
        adjustScore(participant, -participant.score, ActionType.RESET)
    }

    fun undo() {
        screenModelScope.launch {
            repository.undoLastEvent(scoreboardId)
            load()
        }
    }

    fun advanceStructure() {
        val sb = _uiState.value.scoreboard ?: return
        screenModelScope.launch {
            repository.advanceStructure(scoreboardId, sb.currentStructureIndex + 1)
            load()
        }
    }

    fun completeGame() {
        screenModelScope.launch {
            repository.completeScoreboard(scoreboardId)
            load()
        }
    }

    fun dismissWinnerDialog() {
        _uiState.value.winner?.id?.let { dismissedWinnerIds.add(it) }
        _uiState.value = _uiState.value.copy(showWinnerDialog = false)
    }

    private fun detectWinner(scoreboard: Scoreboard?): Participant? {
        if (scoreboard == null) return null
        if (scoreboard.winCondition == WinCondition.NONE) return null
        if (scoreboard.participants.isEmpty()) return null
        return when (scoreboard.scoringMode) {
            ScoringMode.HIGHEST_WINS -> scoreboard.participants.maxByOrNull { it.score }
            ScoringMode.LOWEST_WINS -> scoreboard.participants.minByOrNull { it.score }
            ScoringMode.NO_WINNER -> null
        }
    }

    private fun generateId(): String =
        (1..16).map { "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)] }.joinToString("")
}
