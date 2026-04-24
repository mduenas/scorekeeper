# Scorekeeper App Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Build a free, universal scorekeeper app for iOS and Android using Kotlin Multiplatform that promotes Mark's other apps.

**Architecture:** MVVM with Repository pattern, Compose Multiplatform UI, local SQLite storage via Room (or SQLDelight for KMP), Koin for DI, Voyager for navigation. Follows the same patterns as the Recipeez project at ~/Development/code/Recipes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material3, Voyager navigation, Koin DI, SQLDelight (local persistence), Kotlinx Serialization, Kotlinx DateTime

---

## Phase 1: Project Setup & Data Layer

### Task 1: Add dependencies to build files

**Objective:** Add SQLDelight, Koin, Voyager, Kotlinx DateTime, and Serialization to the project.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

**libs.versions.toml additions:**
```toml
[versions]
sqldelight = "2.0.2"
koin = "3.5.3"
voyager = "1.0.0"
kotlinx-datetime = "0.6.0"
kotlinx-serialization = "1.6.3"

[libraries]
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
voyager-navigator = { module = "cafe.adriel.voyager:voyager-navigator", version.ref = "voyager" }
voyager-koin = { module = "cafe.adriel.voyager:voyager-koin", version.ref = "voyager" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**build.gradle.kts additions:**
```kotlin
plugins {
    // existing...
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

// in commonMain.dependencies:
implementation(libs.sqldelight.runtime)
implementation(libs.sqldelight.coroutines)
implementation(libs.koin.core)
implementation(libs.koin.compose)
implementation(libs.voyager.navigator)
implementation(libs.voyager.koin)
implementation(libs.kotlinx.datetime)
implementation(libs.kotlinx.serialization.json)

// in androidMain.dependencies:
implementation(libs.sqldelight.android)

// in iosMain.dependencies (add sourceset):
implementation(libs.sqldelight.native)

// SQLDelight config (top level):
sqldelight {
    databases {
        create("ScorekeeperDatabase") {
            packageName.set("com.markduenas.scorekeeper.db")
        }
    }
}
```

**Verify:** `./gradlew build` compiles without errors.

**Commit:** `git commit -m "build: add SQLDelight, Koin, Voyager, datetime dependencies"`

---

### Task 2: Create SQLDelight schema

**Objective:** Define the database tables for scoreboards, participants, and score events.

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/markduenas/scorekeeper/db/Scoreboard.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/markduenas/scorekeeper/db/Participant.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/markduenas/scorekeeper/db/ScoreEvent.sq`

**Scoreboard.sq:**
```sql
CREATE TABLE Scoreboard (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    templateId TEXT,
    scoringMode TEXT NOT NULL DEFAULT 'highest_wins',
    negativeScoresAllowed INTEGER NOT NULL DEFAULT 1,
    decimalScoresAllowed INTEGER NOT NULL DEFAULT 0,
    defaultIncrement REAL NOT NULL DEFAULT 1.0,
    customIncrements TEXT NOT NULL DEFAULT '[]',
    structureType TEXT NOT NULL DEFAULT 'none',
    structureLabel TEXT NOT NULL DEFAULT 'Round',
    currentStructureIndex INTEGER NOT NULL DEFAULT 0,
    winCondition TEXT NOT NULL DEFAULT 'none',
    notes TEXT NOT NULL DEFAULT ''
);

selectAll:
SELECT * FROM Scoreboard ORDER BY updatedAt DESC;

selectById:
SELECT * FROM Scoreboard WHERE id = ?;

selectByStatus:
SELECT * FROM Scoreboard WHERE status = ? ORDER BY updatedAt DESC;

insert:
INSERT INTO Scoreboard VALUES ?;

update:
UPDATE Scoreboard SET name=?, updatedAt=?, status=?, scoringMode=?,
    negativeScoresAllowed=?, decimalScoresAllowed=?, defaultIncrement=?,
    customIncrements=?, structureType=?, structureLabel=?,
    currentStructureIndex=?, winCondition=?, notes=?
WHERE id=?;

delete:
DELETE FROM Scoreboard WHERE id=?;
```

**Participant.sq:**
```sql
CREATE TABLE Participant (
    id TEXT NOT NULL PRIMARY KEY,
    scoreboardId TEXT NOT NULL,
    name TEXT NOT NULL,
    score REAL NOT NULL DEFAULT 0,
    color TEXT NOT NULL DEFAULT '#2196F3',
    icon TEXT,
    sortOrder INTEGER NOT NULL DEFAULT 0,
    isActive INTEGER NOT NULL DEFAULT 1
);

selectByScoreboard:
SELECT * FROM Participant WHERE scoreboardId = ? ORDER BY sortOrder ASC;

insert:
INSERT INTO Participant VALUES ?;

updateScore:
UPDATE Participant SET score=? WHERE id=?;

updateName:
UPDATE Participant SET name=? WHERE id=?;

updateOrder:
UPDATE Participant SET sortOrder=? WHERE id=?;

delete:
DELETE FROM Participant WHERE id=?;

deleteByScoreboard:
DELETE FROM Participant WHERE scoreboardId=?;
```

**ScoreEvent.sq:**
```sql
CREATE TABLE ScoreEvent (
    id TEXT NOT NULL PRIMARY KEY,
    scoreboardId TEXT NOT NULL,
    participantId TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    previousScore REAL NOT NULL,
    newScore REAL NOT NULL,
    delta REAL NOT NULL,
    actionType TEXT NOT NULL,
    structureIndex INTEGER NOT NULL DEFAULT 0,
    note TEXT,
    undoneAt INTEGER
);

selectByScoreboard:
SELECT * FROM ScoreEvent WHERE scoreboardId = ? ORDER BY timestamp DESC;

selectActiveByScoreboard:
SELECT * FROM ScoreEvent WHERE scoreboardId = ? AND undoneAt IS NULL ORDER BY timestamp DESC;

insert:
INSERT INTO ScoreEvent VALUES ?;

markUndone:
UPDATE ScoreEvent SET undoneAt=? WHERE id=?;

deleteByScoreboard:
DELETE FROM ScoreEvent WHERE scoreboardId=?;
```

**Verify:** `./gradlew generateCommonMainScorekeeperDatabaseInterface` succeeds.

**Commit:** `git commit -m "db: add SQLDelight schema for scoreboards, participants, events"`

---

### Task 3: Create data models

**Objective:** Define Kotlin data classes that mirror the DB schema and are used throughout the app.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/models/Scoreboard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/models/Participant.kt`
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/models/ScoreEvent.kt`
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/models/Template.kt`

**Scoreboard.kt:**
```kotlin
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
```

**Participant.kt:**
```kotlin
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
```

**ScoreEvent.kt:**
```kotlin
package com.markduenas.scorekeeper.data.models

import kotlinx.serialization.Serializable

enum class ActionType { INCREMENT, DECREMENT, CUSTOM, SET, RESET, PENALTY, BONUS, UNDO }

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
```

**Template.kt:**
```kotlin
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
```

**Commit:** `git commit -m "feat: add data models for scoreboard, participant, score event, template"`

---

### Task 4: Create database driver (platform-specific)

**Objective:** Provide SQLDelight database drivers for Android and iOS.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/DatabaseDriverFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/markduenas/scorekeeper/data/DatabaseDriverFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/markduenas/scorekeeper/data/DatabaseDriverFactory.ios.kt`

**DatabaseDriverFactory.kt (common):**
```kotlin
package com.markduenas.scorekeeper.data

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

**DatabaseDriverFactory.android.kt:**
```kotlin
package com.markduenas.scorekeeper.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.markduenas.scorekeeper.db.ScorekeeperDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ScorekeeperDatabase.Schema, context, "scorekeeper.db")
}
```

**DatabaseDriverFactory.ios.kt:**
```kotlin
package com.markduenas.scorekeeper.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.markduenas.scorekeeper.db.ScorekeeperDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(ScorekeeperDatabase.Schema, "scorekeeper.db")
}
```

**Commit:** `git commit -m "feat: add platform database driver factories"`

---

### Task 5: Create repository

**Objective:** Implement data access layer for scoreboards, participants, and events.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/repository/ScorekeeperRepository.kt`

```kotlin
package com.markduenas.scorekeeper.data.repository

import com.markduenas.scorekeeper.data.DatabaseDriverFactory
import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.db.ScorekeeperDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ScorekeeperRepository(driverFactory: DatabaseDriverFactory) {

    private val db = ScorekeeperDatabase(driverFactory.createDriver())
    private val _scoreboards = MutableStateFlow<List<Scoreboard>>(emptyList())

    suspend fun getAllScoreboards(): List<Scoreboard> {
        return db.scoreboardQueries.selectAll().executeAsList().map { it.toDomain() }
    }

    suspend fun getScoreboard(id: String): Scoreboard? {
        val sb = db.scoreboardQueries.selectById(id).executeAsOneOrNull() ?: return null
        val participants = db.participantQueries.selectByScoreboard(id).executeAsList().map { it.toDomain() }
        return sb.toDomain().copy(participants = participants)
    }

    suspend fun saveScoreboard(scoreboard: Scoreboard) {
        // upsert logic
        db.scoreboardQueries.insert(scoreboard.toDb())
        scoreboard.participants.forEach { p ->
            db.participantQueries.insert(p.toDb())
        }
    }

    suspend fun updateParticipantScore(participantId: String, newScore: Double, event: ScoreEvent) {
        db.participantQueries.updateScore(newScore, participantId)
        db.scoreEventQueries.insert(event.toDb())
    }

    suspend fun undoLastEvent(scoreboardId: String) {
        val last = db.scoreEventQueries.selectActiveByScoreboard(scoreboardId)
            .executeAsList().firstOrNull() ?: return
        db.scoreEventQueries.markUndone(Clock.System.now().toEpochMilliseconds(), last.id)
        db.participantQueries.updateScore(last.previousScore, last.participantId)
    }

    suspend fun getScoreEvents(scoreboardId: String): List<ScoreEvent> {
        return db.scoreEventQueries.selectByScoreboard(scoreboardId)
            .executeAsList().map { it.toDomain() }
    }

    suspend fun deleteScoreboard(id: String) {
        db.scoreEventQueries.deleteByScoreboard(id)
        db.participantQueries.deleteByScoreboard(id)
        db.scoreboardQueries.delete(id)
    }
}
```

Note: Add `.toDomain()` and `.toDb()` extension functions as a separate file:
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/repository/Mappers.kt`

These map between DB-generated types and domain models (straightforward field-by-field mapping).

**Commit:** `git commit -m "feat: add ScorekeeperRepository with CRUD and undo support"`

---

### Task 6: Set up Koin dependency injection

**Objective:** Wire up DI so ViewModels and Repository are injected throughout the app.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/di/AppModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/markduenas/scorekeeper/MainActivity.kt`
- Modify: `composeApp/src/iosMain/kotlin/com/markduenas/scorekeeper/MainViewController.kt`

**AppModule.kt:**
```kotlin
package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import com.markduenas.scorekeeper.presentation.viewmodel.HomeViewModel
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import org.koin.dsl.module

val appModule = module {
    single { ScorekeeperRepository(get()) }
    factory { HomeViewModel(get()) }
    factory { params -> ScoreboardViewModel(get(), params.get()) }
}
```

**Commit:** `git commit -m "feat: configure Koin DI module"`

---

## Phase 2: Templates

### Task 7: Create built-in templates

**Objective:** Define the 12 MVP templates as static data.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/BuiltInTemplates.kt`

```kotlin
package com.markduenas.scorekeeper.data

import com.markduenas.scorekeeper.data.models.*

object BuiltInTemplates {
    val all = listOf(
        Template("blank", "Blank Scoreboard", "Generic", defaultParticipantCount = 2),
        Template("counter", "Simple Counter", "Generic", defaultParticipantCount = 1, customIncrements = listOf(1.0, 5.0, 10.0)),
        Template("team", "Team Scoreboard", "Generic", defaultParticipantCount = 2),
        Template("board_game", "Board Game", "Games", defaultParticipantCount = 4, structureType = StructureType.ROUNDS, structureLabel = "Round"),
        Template("card_game", "Card Game", "Games", defaultParticipantCount = 4, structureType = StructureType.HANDS, structureLabel = "Hand"),
        Template("trivia", "Trivia", "Games", defaultParticipantCount = 4, structureType = StructureType.ROUNDS, structureLabel = "Question"),
        Template("basketball", "Basketball", "Sports", defaultParticipantCount = 2, customIncrements = listOf(1.0, 2.0, 3.0), structureType = StructureType.PERIODS, structureLabel = "Period"),
        Template("soccer", "Soccer", "Sports", defaultParticipantCount = 2, structureType = StructureType.PERIODS, structureLabel = "Half"),
        Template("volleyball", "Volleyball", "Sports", defaultParticipantCount = 2, structureType = StructureType.SETS, structureLabel = "Set"),
        Template("pickleball", "Pickleball", "Sports", defaultParticipantCount = 2, structureType = StructureType.SETS, structureLabel = "Set"),
        Template("cornhole", "Cornhole", "Sports", defaultParticipantCount = 2, customIncrements = listOf(1.0, 3.0)),
        Template("golf", "Golf", "Sports", defaultParticipantCount = 4, scoringMode = ScoringMode.LOWEST_WINS, structureType = StructureType.HOLES, structureLabel = "Hole")
    )

    val categories = all.groupBy { it.category }
}
```

**Commit:** `git commit -m "feat: add 12 built-in MVP templates"`

---

## Phase 3: ViewModels

### Task 8: HomeViewModel

**Objective:** Manage state for home screen — recent scoreboards, template list.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/viewmodel/HomeViewModel.kt`

```kotlin
package com.markduenas.scorekeeper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markduenas.scorekeeper.data.BuiltInTemplates
import com.markduenas.scorekeeper.data.models.Scoreboard
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentScoreboards: List<Scoreboard> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(private val repository: ScorekeeperRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val templates = BuiltInTemplates.all

    fun loadScoreboards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scoreboards = repository.getAllScoreboards()
            _uiState.value = HomeUiState(recentScoreboards = scoreboards, isLoading = false)
        }
    }

    fun deleteScoreboard(id: String) {
        viewModelScope.launch {
            repository.deleteScoreboard(id)
            loadScoreboards()
        }
    }
}
```

**Commit:** `git commit -m "feat: add HomeViewModel"`

---

### Task 9: ScoreboardViewModel

**Objective:** Manage all scoring actions, undo, participant management for an active scoreboard.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/viewmodel/ScoreboardViewModel.kt`

```kotlin
package com.markduenas.scorekeeper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markduenas.scorekeeper.data.models.*
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.Uuid

data class ScoreboardUiState(
    val scoreboard: Scoreboard? = null,
    val scoreEvents: List<ScoreEvent> = emptyList(),
    val isLoading: Boolean = false,
    val winner: Participant? = null
)

class ScoreboardViewModel(
    private val repository: ScorekeeperRepository,
    private val scoreboardId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scoreboard = repository.getScoreboard(scoreboardId)
            val events = repository.getScoreEvents(scoreboardId)
            _uiState.value = ScoreboardUiState(scoreboard = scoreboard, scoreEvents = events)
        }
    }

    fun adjustScore(participant: Participant, delta: Double, actionType: ActionType = ActionType.INCREMENT) {
        val sb = _uiState.value.scoreboard ?: return
        val newScore = if (!sb.negativeScoresAllowed) maxOf(0.0, participant.score + delta)
                       else participant.score + delta
        val event = ScoreEvent(
            id = Uuid.random().toString(),
            scoreboardId = scoreboardId,
            participantId = participant.id,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            previousScore = participant.score,
            newScore = newScore,
            delta = delta,
            actionType = actionType,
            structureIndex = sb.currentStructureIndex
        )
        viewModelScope.launch {
            repository.updateParticipantScore(participant.id, newScore, event)
            load()
        }
    }

    fun setScore(participant: Participant, newScore: Double) {
        adjustScore(participant, newScore - participant.score, ActionType.SET)
    }

    fun undo() {
        viewModelScope.launch {
            repository.undoLastEvent(scoreboardId)
            load()
        }
    }

    fun advanceStructure() {
        val sb = _uiState.value.scoreboard ?: return
        viewModelScope.launch {
            repository.saveScoreboard(sb.copy(currentStructureIndex = sb.currentStructureIndex + 1))
            load()
        }
    }
}
```

**Commit:** `git commit -m "feat: add ScoreboardViewModel with scoring, undo, structure support"`

---

## Phase 4: Screens

### Task 10: App entry point and navigation

**Objective:** Set up Voyager navigation with Home as root screen.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/App.kt`

```kotlin
package com.markduenas.scorekeeper

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.markduenas.scorekeeper.presentation.screens.HomeScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            Navigator(HomeScreen())
        }
    }
}
```

**Commit:** `git commit -m "feat: set up Voyager navigation with HomeScreen root"`

---

### Task 11: Home Screen

**Objective:** Show recent scoreboards, new scoreboard button, and templates.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/HomeScreen.kt`

Key UI elements:
- Top app bar: "Scorekeeper" title + settings icon
- FAB: "+ New Scoreboard"
- Recent scoreboards list (cards with name, players, scores, last updated)
- Templates section (horizontal scroll chips by category)
- Empty state when no scoreboards yet

```kotlin
package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.scorekeeper.presentation.viewmodel.HomeViewModel

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: HomeViewModel = getScreenModel()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) { viewModel.loadScoreboards() }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Scorekeeper") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { navigator.push(NewScoreboardScreen()) }) {
                    Icon(Icons.Default.Add, contentDescription = "New Scoreboard")
                }
            }
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                if (state.recentScoreboards.isEmpty()) {
                    item { EmptyHomeState() }
                } else {
                    items(state.recentScoreboards) { sb ->
                        ScoreboardCard(
                            scoreboard = sb,
                            onClick = { navigator.push(ScoreboardScreen(sb.id)) },
                            onDelete = { viewModel.deleteScoreboard(sb.id) }
                        )
                    }
                }
                item {
                    TemplatesSection(
                        templates = viewModel.templates,
                        onSelect = { template -> navigator.push(NewScoreboardScreen(template.id)) }
                    )
                }
            }
        }
    }
}
```

**Commit:** `git commit -m "feat: add HomeScreen with recent scoreboards and templates"`

---

### Task 12: New Scoreboard Screen

**Objective:** Let users create a blank or template-based scoreboard, add players, and start.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/NewScoreboardScreen.kt`

Key UI elements:
- Scoreboard name field (optional)
- Player list with add/remove/rename
- Scoring mode selector (highest/lowest/none)
- Start button

```kotlin
class NewScoreboardScreen(private val templateId: String? = null) : Screen {
    @Composable
    override fun Content() {
        // Name field, player list, scoring mode, start button
        // On Start: create Scoreboard, save to repo, navigate to ScoreboardScreen
    }
}
```

**Commit:** `git commit -m "feat: add NewScoreboardScreen with player setup"`

---

### Task 13: Scoreboard Screen (main scoring UI)

**Objective:** The primary screen where scoring happens. Large scores, tap to adjust, undo.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/ScoreboardScreen.kt`

Layout rules:
- 1–2 players: large side-by-side or stacked cards
- 3–6 players: 2-column grid
- 7+ players: compact list

Each player card:
- Name (large, readable)
- Score (very large, high contrast)
- +1 / -1 buttons (big tap targets)
- Long-press or secondary button for custom amount

Top bar actions:
- Undo button
- Structure advance (if enabled)
- More menu (settings, history, share, end game)

```kotlin
class ScoreboardScreen(private val scoreboardId: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel: ScoreboardViewModel = getScreenModel { parametersOf(scoreboardId) }
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) { viewModel.load() }

        val scoreboard = state.scoreboard ?: return

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scoreboard.name.ifEmpty { "Scoreboard" }) },
                    actions = {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.Default.Undo, "Undo")
                        }
                        // More menu
                    }
                )
            }
        ) { padding ->
            when {
                scoreboard.participants.size <= 2 -> LargeScoreLayout(scoreboard, viewModel, padding)
                scoreboard.participants.size <= 6 -> GridScoreLayout(scoreboard, viewModel, padding)
                else -> ListScoreLayout(scoreboard, viewModel, padding)
            }
        }
    }
}
```

**Commit:** `git commit -m "feat: add ScoreboardScreen with adaptive layouts"`

---

### Task 14: Player score card composable

**Objective:** Reusable score card with large number, +/- buttons, and color indicator.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/components/ParticipantCard.kt`

```kotlin
@Composable
fun ParticipantCard(
    participant: Participant,
    increment: Double,
    isLeader: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditScore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        border = if (isLeader) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(participant.name, style = MaterialTheme.typography.titleMedium)
            Text(
                formatScore(participant.score),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                Button(onClick = onDecrement, modifier = Modifier.size(56.dp)) { Text("-") }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onIncrement, modifier = Modifier.size(56.dp)) { Text("+") }
            }
        }
    }
}
```

**Commit:** `git commit -m "feat: add ParticipantCard composable"`

---

### Task 15: Score history screen

**Objective:** Show a chronological log of all scoring events with undo support.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/HistoryScreen.kt`

Show each event as a row: player name, +/- delta, timestamp, action type. Allow undo of recent events. Show round/period label if applicable.

**Commit:** `git commit -m "feat: add HistoryScreen"`

---

### Task 16: Custom score dialog

**Objective:** Let users enter a custom score amount or set score directly.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/components/CustomScoreDialog.kt`

```kotlin
@Composable
fun CustomScoreDialog(
    participant: Participant,
    onApplyDelta: (Double) -> Unit,
    onSetScore: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    // Number input, +/- toggle, Apply button
}
```

**Commit:** `git commit -m "feat: add CustomScoreDialog"`

---

## Phase 5: Share & Export

### Task 17: Share final score as text

**Objective:** Let users share scoreboard results as plain text via the system share sheet.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/data/ShareHelper.kt`
- Create platform-specific implementations for Android and iOS

**ShareHelper.kt (common):**
```kotlin
expect fun shareText(text: String)
```

**Share text format:**
```
🏆 [Scoreboard Name]
[Date]

1. Player A — 42 pts
2. Player B — 38 pts
3. Player C — 27 pts

Winner: Player A
Shared from Scorekeeper App
```

**Commit:** `git commit -m "feat: add share as text functionality"`

---

## Phase 6: "More Apps" Promotion

### Task 18: Add More Apps screen

**Objective:** Promote Mark's other apps with a dedicated screen accessible from Settings.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/MoreAppsScreen.kt`

```kotlin
data class AppPromo(val name: String, val description: String, val iosUrl: String, val androidUrl: String, val emoji: String)

val markApps = listOf(
    AppPromo("Recipeez", "Import recipes from any URL with AI", "https://apps.apple.com/us/app/recipeez/id6748916547", "https://play.google.com/store/apps/details?id=com.markduenas.recipes", "🍳"),
    AppPromo("Homesteader", "Track cattle, livestock, and farm animals", "...", "...", "🐄"),
    AppPromo("PracticeFlow", "Practice tracker for musicians and athletes", "...", "...", "🎵"),
    AppPromo("EasyCapRate", "Real estate cap rate calculator", "...", "...", "🏠"),
    AppPromo("Pi Generator", "Generate digits of Pi", "...", "...", "π"),
    // Add others as published
)
```

Each app card: emoji, name, short description, App Store + Play Store buttons.

Also add a subtle "More Apps" button in the main settings/menu — not intrusive but visible.

**Commit:** `git commit -m "feat: add More Apps promo screen"`

---

## Phase 7: Polish & MVP Completion

### Task 19: Dark mode and theming

**Objective:** Support system dark/light mode with a clean Material3 color scheme.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/theme/Theme.kt`

Use a bold, high-contrast color palette. Scores should be readable at a distance. Suggest deep blue primary with white text on score cards.

**Commit:** `git commit -m "feat: add dark/light theme support"`

---

### Task 20: Portrait and landscape layouts

**Objective:** Adapt the scoreboard screen for landscape orientation — side-by-side large score cards.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/screens/ScoreboardScreen.kt`

Use `LocalConfiguration` (Android) and equivalent (iOS) to detect orientation and switch layout.

**Commit:** `git commit -m "feat: add landscape layout for scoreboard screen"`

---

### Task 21: Win condition detection

**Objective:** Detect and display a winner when a win condition is met.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/markduenas/scorekeeper/presentation/viewmodel/ScoreboardViewModel.kt`

After each score update, check win condition. If met, set `winner` in UI state. Show a winner banner/dialog with "Continue" or "End Game" options.

**Commit:** `git commit -m "feat: add win condition detection and winner display"`

---

### Task 22: App icon and store assets

**Objective:** Replace default icon with a Scorekeeper-branded icon.

**Files:**
- Replace: `composeApp/src/androidMain/res/mipmap-*/ic_launcher.png`
- Replace: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`

Suggested concept: scoreboard or tally marks on a clean background with the app's primary color. Can use image_gen tool to create initial concept.

**Commit:** `git commit -m "assets: add app icon"`

---

## MVP Checklist

- [ ] Create blank scoreboard
- [ ] Create from template (12 templates)
- [ ] Add/edit/remove participants
- [ ] Increment/decrement scores
- [ ] Custom score adjustment
- [ ] Undo last action
- [ ] Local save/resume
- [ ] Score history screen
- [ ] Highest/lowest/no winner mode
- [ ] Basic round/period support
- [ ] Portrait/landscape support
- [ ] Dark/light mode
- [ ] Share final score as text
- [ ] More Apps promotion screen

---

## Build & Run

```bash
# Android
./gradlew installDebug

# iOS
# Open iosApp/iosApp.xcodeproj in Xcode and run

# Tests
./gradlew allTests
```

---

## Notes

- Follow the same MVVM + Repository + Koin + Voyager patterns used in ~/Development/code/Recipes
- All data is local-first, no Firebase needed for MVP
- The "More Apps" screen is a key deliverable — it should be polished and easy to find
- Keep the core scoring UX as simple and fast as possible — this is the main value prop
