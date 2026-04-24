package com.markduenas.scorekeeper.data.repository

import com.markduenas.scorekeeper.data.DatabaseDriverFactory
import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.db.ScorekeeperDatabase
import kotlinx.datetime.Clock

class ScorekeeperRepository(driverFactory: DatabaseDriverFactory) {

    private val db = ScorekeeperDatabase(driverFactory.createDriver())

    fun getAllScoreboards(): List<Scoreboard> =
        db.scoreboardQueries.selectAll().executeAsList().map { it.toDomain() }

    fun getScoreboard(id: String): Scoreboard? {
        val sb = db.scoreboardQueries.selectById(id).executeAsOneOrNull() ?: return null
        val participants = db.participantQueries.selectByScoreboard(id)
            .executeAsList().map { it.toDomain() }
        return sb.toDomain().copy(participants = participants)
    }

    fun saveScoreboard(scoreboard: Scoreboard) {
        val s = scoreboard.toDb()
        db.scoreboardQueries.insert(s.id, s.name, s.createdAt, s.updatedAt, s.status,
            s.templateId, s.scoringMode, s.negativeScoresAllowed, s.decimalScoresAllowed,
            s.defaultIncrement, s.customIncrements, s.structureType, s.structureLabel,
            s.currentStructureIndex, s.winCondition, s.notes)
        scoreboard.participants.forEach { p ->
            val pd = p.toDb()
            db.participantQueries.insert(pd.id, pd.scoreboardId, pd.name, pd.score, pd.color, pd.icon, pd.sortOrder, pd.isActive)
        }
    }

    fun updateScoreboardMeta(scoreboard: Scoreboard) {
        db.scoreboardQueries.updateMeta(
            name = scoreboard.name,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            status = scoreboard.status.name.lowercase(),
            scoringMode = scoreboard.scoringMode.name.lowercase(),
            negativeScoresAllowed = if (scoreboard.negativeScoresAllowed) 1L else 0L,
            decimalScoresAllowed = if (scoreboard.decimalScoresAllowed) 1L else 0L,
            defaultIncrement = scoreboard.defaultIncrement,
            customIncrements = scoreboard.customIncrements.toString(),
            structureType = scoreboard.structureType.name.lowercase(),
            structureLabel = scoreboard.structureLabel,
            currentStructureIndex = scoreboard.currentStructureIndex.toLong(),
            winCondition = scoreboard.winCondition.name.lowercase(),
            notes = scoreboard.notes,
            id = scoreboard.id
        )
    }

    fun updateParticipantScore(participantId: String, newScore: Double, event: ScoreEvent) {
        db.participantQueries.updateScore(newScore, participantId)
        val e = event.toDb()
        db.scoreEventQueries.insert(e.id, e.scoreboardId, e.participantId, e.timestamp,
            e.previousScore, e.newScore, e.delta, e.actionType, e.structureIndex, e.note, e.undoneAt)
    }

    fun undoLastEvent(scoreboardId: String) {
        val last = db.scoreEventQueries.selectActiveByScoreboard(scoreboardId)
            .executeAsList().firstOrNull() ?: return
        db.scoreEventQueries.markUndone(Clock.System.now().toEpochMilliseconds(), last.id)
        db.participantQueries.updateScore(last.previousScore, last.participantId)
    }

    fun getScoreEvents(scoreboardId: String): List<ScoreEvent> =
        db.scoreEventQueries.selectByScoreboard(scoreboardId)
            .executeAsList().map { it.toDomain() }

    fun addParticipant(participant: Participant) {
        val p = participant.toDb()
        db.participantQueries.insert(p.id, p.scoreboardId, p.name, p.score, p.color, p.icon, p.sortOrder, p.isActive)
    }

    fun deleteParticipant(id: String) =
        db.participantQueries.delete(id)

    fun renameParticipant(id: String, name: String) =
        db.participantQueries.updateName(name, id)

    fun advanceStructure(scoreboardId: String, newIndex: Int) =
        db.scoreboardQueries.updateStructureIndex(
            newIndex.toLong(),
            Clock.System.now().toEpochMilliseconds(),
            scoreboardId
        )

    fun completeScoreboard(scoreboardId: String) =
        db.scoreboardQueries.updateStatus(
            "completed",
            Clock.System.now().toEpochMilliseconds(),
            scoreboardId
        )

    fun deleteScoreboard(id: String) {
        db.scoreEventQueries.deleteByScoreboard(id)
        db.participantQueries.deleteByScoreboard(id)
        db.scoreboardQueries.delete(id)
    }
}
