package com.markduenas.scorekeeper.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.markduenas.scorekeeper.data.BuiltInTemplates
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.models.Template
import com.markduenas.scorekeeper.data.repository.FirestoreRepository
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentScoreboards: List<Scoreboard> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: ScorekeeperRepository,
    private val firestoreRepository: FirestoreRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _userTemplates = MutableStateFlow<List<Template>>(emptyList())
    val userTemplates: StateFlow<List<Template>> = _userTemplates

    private val _communityTemplates = MutableStateFlow<List<Template>>(emptyList())
    val communityTemplates: StateFlow<List<Template>> = _communityTemplates

    val builtInCategories = BuiltInTemplates.categories

    init {
        loadScoreboards()
        loadUserTemplates()
        loadCommunityTemplates()
    }

    fun loadScoreboards() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val scoreboards = repository.getAllScoreboards()
                _uiState.value = HomeUiState(recentScoreboards = scoreboards, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false)
            }
        }
    }

    fun loadUserTemplates() {
        screenModelScope.launch {
            try {
                _userTemplates.value = repository.getUserTemplates()
            } catch (e: Exception) {
                // Leave empty on error
            }
        }
    }

    fun loadCommunityTemplates() {
        screenModelScope.launch {
            try {
                _communityTemplates.value = firestoreRepository.getCommunityTemplates()
            } catch (e: Exception) {
                // Offline or auth failure — leave empty
            }
        }
    }

    fun deleteScoreboard(id: String) {
        screenModelScope.launch {
            try {
                repository.deleteScoreboard(id)
                loadScoreboards()
            } catch (e: Exception) {
                loadScoreboards()
            }
        }
    }

    fun deleteUserTemplate(id: String) {
        screenModelScope.launch {
            try {
                repository.deleteUserTemplate(id)
                loadUserTemplates()
            } catch (e: Exception) {
                loadUserTemplates()
            }
        }
    }
}
