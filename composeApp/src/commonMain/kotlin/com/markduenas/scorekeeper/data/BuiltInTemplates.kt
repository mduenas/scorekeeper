package com.markduenas.scorekeeper.data

import com.markduenas.scorekeeper.data.models.*

object BuiltInTemplates {
    val all = listOf(
        Template("blank", "Blank Scoreboard", "Generic", defaultParticipantCount = 2),
        Template("counter", "Simple Counter", "Generic", defaultParticipantCount = 1,
            customIncrements = listOf(1.0, 5.0, 10.0)),
        Template("team", "Team Scoreboard", "Generic", defaultParticipantCount = 2),
        Template("board_game", "Board Game", "Games", defaultParticipantCount = 4,
            structureType = StructureType.ROUNDS, structureLabel = "Round"),
        Template("card_game", "Card Game", "Games", defaultParticipantCount = 4,
            structureType = StructureType.HANDS, structureLabel = "Hand"),
        Template("trivia", "Trivia", "Games", defaultParticipantCount = 4,
            structureType = StructureType.ROUNDS, structureLabel = "Question"),
        Template("basketball", "Basketball", "Sports", defaultParticipantCount = 2,
            customIncrements = listOf(1.0, 2.0, 3.0),
            structureType = StructureType.PERIODS, structureLabel = "Period"),
        Template("soccer", "Soccer", "Sports", defaultParticipantCount = 2,
            structureType = StructureType.PERIODS, structureLabel = "Half"),
        Template("volleyball", "Volleyball", "Sports", defaultParticipantCount = 2,
            structureType = StructureType.SETS, structureLabel = "Set"),
        Template("pickleball", "Pickleball", "Sports", defaultParticipantCount = 2,
            structureType = StructureType.SETS, structureLabel = "Set"),
        Template("cornhole", "Cornhole", "Sports", defaultParticipantCount = 2,
            customIncrements = listOf(1.0, 3.0)),
        Template("golf", "Golf", "Sports", defaultParticipantCount = 4,
            scoringMode = ScoringMode.LOWEST_WINS,
            structureType = StructureType.HOLES, structureLabel = "Hole")
    )

    val categories get() = all.groupBy { it.category }
}
