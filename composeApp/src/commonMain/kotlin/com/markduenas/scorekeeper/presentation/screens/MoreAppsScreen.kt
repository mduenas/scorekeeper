package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class AppPromo(
    val emoji: String,
    val name: String,
    val description: String,
    val iosUrl: String,
    val androidUrl: String
)

val markApps = listOf(
    AppPromo("🍳", "Recipeez", "Import recipes from any URL using on-device AI. Meal planning and shopping lists.",
        "https://apps.apple.com/us/app/recipeez/id6748916547",
        "https://play.google.com/store/apps/details?id=com.markduenas.recipes"),
    AppPromo("🏡", "Homesteader", "Track cattle, chickens, pigs, and livestock with health records and breeding.", "", ""),
    AppPromo("🎵", "PracticeFlow", "Track practice sessions for music, sports, or any skill.", "", ""),
    AppPromo("🏠", "EasyCapRate", "Real estate cap rate calculator for investors.", "", ""),
    AppPromo("π", "Pi Generator", "Generate and explore digits of Pi.", "", ""),
    AppPromo("📖", "Markdown Viewer", "View any Markdown file beautifully. Free.", "", ""),
)

@OptIn(ExperimentalMaterial3Api::class)
class MoreAppsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("More Apps") },
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
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 32.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("More apps by the same developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("All built with the same focus on simplicity and privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                items(markApps) { app ->
                    AppPromoCard(
                        app = app,
                        onIosClick = { if (app.iosUrl.isNotEmpty()) uriHandler.openUri(app.iosUrl) },
                        onAndroidClick = { if (app.androidUrl.isNotEmpty()) uriHandler.openUri(app.androidUrl) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppPromoCard(app: AppPromo, onIosClick: () -> Unit, onAndroidClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(app.emoji, fontSize = 32.sp)
                Column {
                    Text(app.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(app.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (app.iosUrl.isNotEmpty()) {
                    OutlinedButton(onClick = onIosClick, modifier = Modifier.weight(1f)) {
                        Text("App Store", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (app.androidUrl.isNotEmpty()) {
                    OutlinedButton(onClick = onAndroidClick, modifier = Modifier.weight(1f)) {
                        Text("Play Store", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
