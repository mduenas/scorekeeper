package com.markduenas.scorekeeper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.markduenas.scorekeeper.data.BuiltInTemplates
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.models.Template
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentScoreboards: List<Scoreboard> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(private val repository: ScorekeeperRepository) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val templates: List<Template> = BuiltInTemplates.all
    val templateCategories = BuiltInTemplates.categories

    init {
        loadScoreboards()
    }

    fun loadScoreboards() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scoreboards = repository.getAllScoreboards()
            _uiState.value = HomeUiState(recentScoreboards = scoreboards, isLoading = false)
        }
    }

    fun deleteScoreboard(id: String) {
        screenModelScope.launch {
            repository.deleteScoreboard(id)
            loadScoreboards()
        }
    }
}
