package com.markduenas.scorekeeper

import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.scorekeeper.di.appModule
import com.markduenas.scorekeeper.di.iosModule
import org.koin.core.context.startKoin

fun MainViewController() = MainViewControllerWithScreenshot(null)

fun MainViewControllerWithScreenshot(screenshotName: String?) = run {
    if (screenshotName == null) {
        startKoin {
            modules(iosModule, appModule)
        }
    }
    ComposeUIViewController { App(screenshotName = screenshotName) }
}
