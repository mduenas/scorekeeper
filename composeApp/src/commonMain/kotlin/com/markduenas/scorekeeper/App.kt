package com.markduenas.scorekeeper

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.markduenas.scorekeeper.presentation.screens.HomeScreen
import com.markduenas.scorekeeper.presentation.theme.ScorekeeperTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        ScorekeeperTheme {
            Navigator(HomeScreen())
        }
    }
}
