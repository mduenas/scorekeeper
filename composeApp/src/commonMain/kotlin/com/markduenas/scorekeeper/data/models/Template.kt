package com.markduenas.scorekeeper.data.models

data class Template(
    val id: String,
    val name: String,
    val category: String,
    val defaultParticipantCount: Int = 2,
    val scoringMode: ScoringMode = ScoringMode.HIGHEST_WINS,
    val defaultIncrement: Double = 1.0,
    val customIncrements: List<Double> = emptyList(),
    val structureType: StructureType = StructureType.NONE,
    val structureLabel: String = "Round",
    val winCondition: WinCondition = WinCondition.NONE
)
