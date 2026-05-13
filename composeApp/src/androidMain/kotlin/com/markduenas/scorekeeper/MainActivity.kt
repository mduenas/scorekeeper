package com.markduenas.scorekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.markduenas.scorekeeper.data.AnalyticsService
import com.markduenas.scorekeeper.data.FirstLaunchTracker
import com.markduenas.scorekeeper.data.appContext
import com.markduenas.scorekeeper.data.initShareHelper
import com.markduenas.scorekeeper.di.androidModule
import com.markduenas.scorekeeper.di.appModule
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initShareHelper(this)
        appContext = this
        Firebase.initialize(this)
        if (FirstLaunchTracker.isFirstLaunch()) {
            AnalyticsService().logFirstOpen()
            FirstLaunchTracker.markLaunched()
        }
        startKoin {
            androidContext(this@MainActivity)
            modules(androidModule, appModule)
        }
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
