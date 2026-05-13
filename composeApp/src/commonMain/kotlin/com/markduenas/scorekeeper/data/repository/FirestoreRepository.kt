package com.markduenas.scorekeeper.data.repository

import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import com.markduenas.scorekeeper.currentTimeMs

class FirestoreRepository(private val localRepo: ScorekeeperRepository) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val templatesCollection = firestore.collection("community_templates")
    private var signedIn = false

    suspend fun ensureSignedIn() {
        if (signedIn) return
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously()
            }
            signedIn = true
            // Flush any templates that were saved while offline
            flushPendingShares()
        } catch (e: Exception) {
            // No network or auth failure — will retry next call
        }
    }

    private suspend fun flushPendingShares() {
        val pending = localRepo.getPendingCommunityShares()
        for (template in pending) {
            try {
                val result = shareCommunityTemplate(template)
                if (result.isSuccess) {
                    localRepo.clearPendingCommunityShare(template.id)
                }
            } catch (e: Exception) {
                // Leave it pending for next time
            }
        }
    }

    suspend fun getCommunityTemplates(): List<Template> {
        return try {
            ensureSignedIn()
            val snapshot = templatesCollection
                .where { "isApproved" equalTo true }
                .get()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data<Map<String, Any?>>()
                    Template(
                        id = doc.id,
                        name = data["name"] as? String ?: return@mapNotNull null,
                        category = data["category"] as? String ?: "Community",
                        defaultParticipantCount = (data["defaultParticipantCount"] as? Number)?.toInt() ?: 2,
                        scoringMode = parseScoringMode(data["scoringMode"] as? String),
                        defaultIncrement = (data["defaultIncrement"] as? Number)?.toDouble() ?: 1.0,
                        customIncrements = parseDoubleList(data["customIncrements"]),
                        structureType = parseStructureType(data["structureType"] as? String),
                        structureLabel = data["structureLabel"] as? String ?: "Round",
                        winCondition = parseWinCondition(data["winCondition"] as? String),
                        isUserCreated = false
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun shareCommunityTemplate(template: Template): Result<Unit> {
        return try {
            ensureSignedIn()
            if (!signedIn) return Result.success(Unit) // offline, skip silently
            val data = mapOf(
                "name" to template.name,
                "category" to template.category,
                "defaultParticipantCount" to template.defaultParticipantCount,
                "scoringMode" to template.scoringMode.name.lowercase(),
                "defaultIncrement" to template.defaultIncrement,
                "customIncrements" to template.customIncrements,
                "structureType" to template.structureType.name.lowercase(),
                "structureLabel" to template.structureLabel,
                "winCondition" to template.winCondition.name.lowercase(),
                "isApproved" to false,
                "createdAt" to currentTimeMs()
            )
            templatesCollection.add(data)
            Result.success(Unit)
        } catch (e: Exception) {
            // Silently ignore network errors — template is already saved locally
            Result.success(Unit)
        }
    }

    private fun parseDoubleList(value: Any?): List<Double> {
        if (value == null) return emptyList()
        @Suppress("UNCHECKED_CAST")
        return try {
            (value as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseScoringMode(value: String?): ScoringMode = when (value?.uppercase()) {
        "LOWEST_WINS" -> ScoringMode.LOWEST_WINS
        "NO_WINNER" -> ScoringMode.NO_WINNER
        else -> ScoringMode.HIGHEST_WINS
    }

    private fun parseStructureType(value: String?): StructureType = when (value?.uppercase()) {
        "ROUNDS" -> StructureType.ROUNDS
        "TURNS" -> StructureType.TURNS
        "PERIODS" -> StructureType.PERIODS
        "SETS" -> StructureType.SETS
        "INNINGS" -> StructureType.INNINGS
        "HOLES" -> StructureType.HOLES
        "HANDS" -> StructureType.HANDS
        "CUSTOM" -> StructureType.CUSTOM
        else -> StructureType.NONE
    }

    private fun parseWinCondition(value: String?): WinCondition = when (value?.uppercase()) {
        "HIGHEST_SCORE" -> WinCondition.HIGHEST_SCORE
        "LOWEST_SCORE" -> WinCondition.LOWEST_SCORE
        "FIRST_TO_TARGET" -> WinCondition.FIRST_TO_TARGET
        "BEST_OF" -> WinCondition.BEST_OF
        "MANUAL" -> WinCondition.MANUAL
        else -> WinCondition.NONE
    }
}
