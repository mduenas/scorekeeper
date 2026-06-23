package com.markduenas.scorekeeper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import com.markduenas.scorekeeper.data.repository.FirestoreRepository
import com.markduenas.scorekeeper.presentation.screens.HomeScreen
import com.markduenas.scorekeeper.presentation.theme.ScorekeeperTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App(screenshotName: String? = null) {
    ScorekeeperTheme {
        if (screenshotName != null) {
            StoreScreenshotScreen(screenshotName)
        } else {
            KoinContext {
                val firestoreRepo: FirestoreRepository = koinInject()

                LaunchedEffect(Unit) {
                    try {
                        firestoreRepo.ensureSignedIn()
                    } catch (e: Exception) {
                        // No network - proceed anyway
                    }
                }

                Navigator(HomeScreen())
            }
        }
    }
}
