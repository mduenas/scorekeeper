package com.markduenas.scorekeeper.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Participant(
    val id: String,
    val scoreboardId: String,
    val name: String,
    val score: Double = 0.0,
    val color: String = "#2196F3",
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
