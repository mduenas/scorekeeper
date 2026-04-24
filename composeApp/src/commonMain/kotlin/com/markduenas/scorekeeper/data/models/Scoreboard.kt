package com.markduenas.scorekeeper.data.models

import kotlinx.serialization.Serializable

enum class ScoringMode { HIGHEST_WINS, LOWEST_WINS, NO_WINNER }
enum class StructureType { NONE, ROUNDS, TURNS, PERIODS, SETS, INNINGS, HOLES, HANDS, CUSTOM }
enum class WinCondition { NONE, HIGHEST_SCORE, LOWEST_SCORE, FIRST_TO_TARGET, BEST_OF, MANUAL }
enum class ScoreboardStatus { ACTIVE, COMPLETED, ARCHIVED }

@Serializable
data class Scoreboard(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: ScoreboardStatus = ScoreboardStatus.ACTIVE,
    val templateId: String? = null,
    val scoringMode: ScoringMode = ScoringMode.HIGHEST_WINS,
    val negativeScoresAllowed: Boolean = true,
    val decimalScoresAllowed: Boolean = false,
    val defaultIncrement: Double = 1.0,
    val customIncrements: List<Double> = emptyList(),
    val structureType: StructureType = StructureType.NONE,
    val structureLabel: String = "Round",
    val currentStructureIndex: Int = 0,
    val winCondition: WinCondition = WinCondition.NONE,
    val notes: String = "",
    val participants: List<Participant> = emptyList()
)
