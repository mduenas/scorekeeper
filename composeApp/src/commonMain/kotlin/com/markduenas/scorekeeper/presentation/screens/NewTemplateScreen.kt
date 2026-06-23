package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.scorekeeper.data.models.ScoringMode
import com.markduenas.scorekeeper.data.models.Template
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import org.koin.compose.koinInject
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
class NewTemplateScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: ScorekeeperRepository = koinInject()

        var name by remember { mutableStateOf("") }
        var playerCountStr by remember { mutableStateOf("2") }
        var scoringMode by remember { mutableStateOf(ScoringMode.HIGHEST_WINS) }
        var buttonInput by remember { mutableStateOf("") }
        var customIncrements by remember { mutableStateOf(emptyList<Double>()) }

        val playerCount = playerCountStr.toIntOrNull() ?: 2
        val isNameValid = name.isNotBlank()
        val isPlayerCountValid = playerCount in 1..100
        val isFormValid = isNameValid && isPlayerCountValid

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("New Template") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 100.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Template name") },
                        placeholder = { Text("e.g. Scrabble") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = playerCountStr,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                playerCountStr = newValue
                            }
                        },
                        label = { Text("Default Player Count (1-100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Win Condition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Column {
                        listOf(
                            ScoringMode.HIGHEST_WINS to "Highest score wins",
                            ScoringMode.LOWEST_WINS to "Lowest score wins",
                            ScoringMode.NO_WINNER to "No winner tracking"
                        ).forEach { (mode, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = scoringMode == mode, onClick = { scoringMode = mode })
                                Text(label)
                            }
                        }
                    }
                }

                item {
                    Text("Quick Scoring Buttons (1-10 buttons)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Configure values for quick scoring (e.g. 5 will create -5 and +5 buttons). If none are added, standard +1 / -1 buttons will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = buttonInput,
                            onValueChange = { buttonInput = it },
                            label = { Text("Button value") },
                            placeholder = { Text("e.g. 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val value = buttonInput.toDoubleOrNull()
                                if (value != null && value > 0 && value !in customIncrements && customIncrements.size < 10) {
                                    customIncrements = customIncrements + value
                                    buttonInput = ""
                                }
                            },
                            enabled = buttonInput.toDoubleOrNull()?.let { it > 0 && customIncrements.size < 10 } == true
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Button")
                        }
                    }
                }

                if (customIncrements.isNotEmpty()) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            customIncrements.forEach { valDouble ->
                                val formattedValue = if (valDouble == valDouble.toLong().toDouble()) valDouble.toLong().toString() else valDouble.toString()
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("±$formattedValue") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { customIncrements = customIncrements - valDouble },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove button",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val templateId = (1..16).map { "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)] }.joinToString("")
                            val template = Template(
                                id = templateId,
                                name = name.trim(),
                                category = "My Templates",
                                defaultParticipantCount = playerCount,
                                scoringMode = scoringMode,
                                customIncrements = customIncrements,
                                isUserCreated = true
                            )
                            repository.saveUserTemplate(template)
                            navigator.pop()
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Save Template", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
