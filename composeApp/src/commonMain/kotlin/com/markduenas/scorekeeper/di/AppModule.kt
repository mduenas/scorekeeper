package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.AnalyticsService
import com.markduenas.scorekeeper.data.repository.FirestoreRepository
import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import com.markduenas.scorekeeper.presentation.viewmodel.HomeViewModel
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import org.koin.dsl.module

val appModule = module {
    single { ScorekeeperRepository(get()) }
    single { FirestoreRepository(get()) }
    single { AnalyticsService() }
    factory { HomeViewModel(get(), get()) }
    factory { (scoreboardId: String) -> ScoreboardViewModel(get(), get(), scoreboardId) }
}
