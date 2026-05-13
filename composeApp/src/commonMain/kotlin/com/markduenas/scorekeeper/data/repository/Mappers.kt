package com.markduenas.scorekeeper.data.repository

import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.db.Participant as DbParticipant
import com.markduenas.scorekeeper.db.Scoreboard as DbScoreboard
import com.markduenas.scorekeeper.db.ScoreEvent as DbScoreEvent
import com.markduenas.scorekeeper.db.UserTemplate as DbUserTemplate
import kotlinx.serialization.json.Json

fun DbScoreboard.toDomain(): Scoreboard = Scoreboard(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = ScoreboardStatus.valueOf(status.uppercase()),
    templateId = templateId,
    scoringMode = ScoringMode.valueOf(scoringMode.uppercase()),
    negativeScoresAllowed = negativeScoresAllowed != 0L,
    decimalScoresAllowed = decimalScoresAllowed != 0L,
    defaultIncrement = defaultIncrement,
    customIncrements = Json.decodeFromString(customIncrements),
    structureType = StructureType.valueOf(structureType.uppercase()),
    structureLabel = structureLabel,
    currentStructureIndex = currentStructureIndex.toInt(),
    winCondition = WinCondition.valueOf(winCondition.uppercase()),
    notes = notes
)

fun Scoreboard.toDb(): DbScoreboard = DbScoreboard(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status.name.lowercase(),
    templateId = templateId,
    scoringMode = scoringMode.name.lowercase(),
    negativeScoresAllowed = if (negativeScoresAllowed) 1L else 0L,
    decimalScoresAllowed = if (decimalScoresAllowed) 1L else 0L,
    defaultIncrement = defaultIncrement,
    customIncrements = Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<Double>()), customIncrements),
    structureType = structureType.name.lowercase(),
    structureLabel = structureLabel,
    currentStructureIndex = currentStructureIndex.toLong(),
    winCondition = winCondition.name.lowercase(),
    notes = notes
)

fun DbParticipant.toDomain(): Participant = Participant(
    id = id,
    scoreboardId = scoreboardId,
    name = name,
    score = score,
    color = color,
    icon = icon,
    sortOrder = sortOrder.toInt(),
    isActive = isActive != 0L
)

fun Participant.toDb(): DbParticipant = DbParticipant(
    id = id,
    scoreboardId = scoreboardId,
    name = name,
    score = score,
    color = color,
    icon = icon,
    sortOrder = sortOrder.toLong(),
    isActive = if (isActive) 1L else 0L
)

fun DbScoreEvent.toDomain(): ScoreEvent = ScoreEvent(
    id = id,
    scoreboardId = scoreboardId,
    participantId = participantId,
    timestamp = timestamp,
    previousScore = previousScore,
    newScore = newScore,
    delta = delta,
    actionType = ActionType.valueOf(actionType.uppercase()),
    structureIndex = structureIndex.toInt(),
    note = note,
    undoneAt = undoneAt
)

fun DbUserTemplate.toDomain(): Template = Template(
    id = id,
    name = name,
    category = category,
    defaultParticipantCount = defaultParticipantCount.toInt(),
    scoringMode = ScoringMode.valueOf(scoringMode.uppercase()),
    defaultIncrement = defaultIncrement,
    customIncrements = Json.decodeFromString(customIncrements),
    structureType = StructureType.valueOf(structureType.uppercase()),
    structureLabel = structureLabel,
    winCondition = WinCondition.valueOf(winCondition.uppercase()),
    isUserCreated = true
)

fun Template.toDb(createdAt: Long, pendingCommunityShare: Boolean = false): DbUserTemplate = DbUserTemplate(
    id = id,
    name = name,
    category = category,
    defaultParticipantCount = defaultParticipantCount.toLong(),
    scoringMode = scoringMode.name.lowercase(),
    defaultIncrement = defaultIncrement,
    customIncrements = Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<Double>()), customIncrements),
    structureType = structureType.name.lowercase(),
    structureLabel = structureLabel,
    winCondition = winCondition.name.lowercase(),
    createdAt = createdAt,
    pendingCommunityShare = if (pendingCommunityShare) 1L else 0L
)

fun ScoreEvent.toDb(): DbScoreEvent = DbScoreEvent(
    id = id,
    scoreboardId = scoreboardId,
    participantId = participantId,
    timestamp = timestamp,
    previousScore = previousScore,
    newScore = newScore,
    delta = delta,
    actionType = actionType.name.lowercase(),
    structureIndex = structureIndex.toLong(),
    note = note,
    undoneAt = undoneAt
)
