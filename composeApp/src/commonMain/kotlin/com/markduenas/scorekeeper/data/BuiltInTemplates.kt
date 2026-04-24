package com.markduenas.scorekeeper.data

import com.markduenas.scorekeeper.data.models.*

object BuiltInTemplates {
    val all = listOf(
        // Generic
        Template(
            "blank", "Blank Scoreboard", "Generic",
            defaultParticipantCount = 2,
            customIncrements = listOf(1.0, 5.0, 10.0)
        ),
        Template(
            "counter", "Simple Counter", "Generic",
            defaultParticipantCount = 1,
            customIncrements = listOf(1.0, 5.0, 10.0, 25.0)
        ),

        // Games
        Template(
            "board_game", "Board Game", "Games",
            defaultParticipantCount = 4,
            structureType = StructureType.ROUNDS, structureLabel = "Round",
            customIncrements = listOf(1.0, 5.0, 10.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "card_game", "Card Game", "Games",
            defaultParticipantCount = 4,
            structureType = StructureType.HANDS, structureLabel = "Hand",
            customIncrements = listOf(1.0, 5.0, 10.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "cribbage", "Cribbage", "Games",
            defaultParticipantCount = 2,
            customIncrements = listOf(1.0, 2.0, 3.0, 5.0),
            winCondition = WinCondition.FIRST_TO_TARGET,
            structureType = StructureType.NONE
        ),
        Template(
            "yahtzee", "Yahtzee", "Games",
            defaultParticipantCount = 4,
            customIncrements = listOf(1.0, 5.0, 10.0, 25.0, 50.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "trivia", "Trivia", "Games",
            defaultParticipantCount = 4,
            structureType = StructureType.ROUNDS, structureLabel = "Question",
            customIncrements = listOf(1.0, 2.0, 3.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "poker", "Poker", "Games",
            defaultParticipantCount = 6,
            structureType = StructureType.HANDS, structureLabel = "Hand",
            customIncrements = listOf(1.0, 5.0, 10.0, 25.0),
            scoringMode = ScoringMode.HIGHEST_WINS
        ),
        Template(
            "uno", "UNO", "Games",
            defaultParticipantCount = 4,
            structureType = StructureType.ROUNDS, structureLabel = "Round",
            customIncrements = listOf(1.0, 10.0, 20.0, 50.0),
            winCondition = WinCondition.LOWEST_SCORE
        ),

        // Sports
        Template(
            "basketball", "Basketball", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.PERIODS, structureLabel = "Quarter",
            customIncrements = listOf(1.0, 2.0, 3.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "football", "Football", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.PERIODS, structureLabel = "Quarter",
            customIncrements = listOf(1.0, 2.0, 3.0, 6.0, 7.0, 8.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "soccer", "Soccer", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.PERIODS, structureLabel = "Half",
            customIncrements = listOf(1.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "volleyball", "Volleyball", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.SETS, structureLabel = "Set",
            customIncrements = listOf(1.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "pickleball", "Pickleball", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.SETS, structureLabel = "Set",
            customIncrements = listOf(1.0),
            winCondition = WinCondition.FIRST_TO_TARGET
        ),
        Template(
            "tennis", "Tennis", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.SETS, structureLabel = "Set",
            customIncrements = listOf(1.0)
        ),
        Template(
            "baseball", "Baseball", "Sports",
            defaultParticipantCount = 2,
            structureType = StructureType.INNINGS, structureLabel = "Inning",
            customIncrements = listOf(1.0, 2.0, 3.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "golf", "Golf", "Sports",
            defaultParticipantCount = 4,
            scoringMode = ScoringMode.LOWEST_WINS,
            structureType = StructureType.HOLES, structureLabel = "Hole",
            customIncrements = listOf(1.0)
        ),
        Template(
            "bowling", "Bowling", "Sports",
            defaultParticipantCount = 6,
            customIncrements = listOf(1.0, 5.0, 10.0, 30.0, 60.0, 100.0, 150.0, 200.0, 300.0),
            winCondition = WinCondition.HIGHEST_SCORE
        ),
        Template(
            "darts", "Darts", "Sports",
            defaultParticipantCount = 2,
            scoringMode = ScoringMode.LOWEST_WINS,
            customIncrements = listOf(1.0, 5.0, 10.0, 20.0, 25.0, 50.0),
            winCondition = WinCondition.LOWEST_SCORE
        ),
        Template(
            "cornhole", "Cornhole", "Sports",
            defaultParticipantCount = 2,
            customIncrements = listOf(1.0, 3.0),
            winCondition = WinCondition.FIRST_TO_TARGET
        )
    )

    val categories get() = all.groupBy { it.category }
}
