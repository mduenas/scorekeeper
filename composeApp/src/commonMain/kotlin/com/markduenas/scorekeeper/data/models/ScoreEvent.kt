package com.markduenas.scorekeeper.data.models

import kotlinx.serialization.Serializable

enum class ActionType { INCREMENT, DECREMENT, CUSTOM, SET, RESET, PENALTY, BONUS }

@Serializable
data class ScoreEvent(
    val id: String,
    val scoreboardId: String,
    val participantId: String,
    val timestamp: Long,
    val previousScore: Double,
    val newScore: Double,
    val delta: Double,
    val actionType: ActionType,
    val structureIndex: Int = 0,
    val note: String? = null,
    val undoneAt: Long? = null
)
