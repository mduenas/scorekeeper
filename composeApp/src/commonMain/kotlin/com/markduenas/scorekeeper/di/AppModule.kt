package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import com.markduenas.scorekeeper.presentation.viewmodel.HomeViewModel
import com.markduenas.scorekeeper.presentation.viewmodel.ScoreboardViewModel
import org.koin.dsl.module

val appModule = module {
    single { ScorekeeperRepository(get()) }
    factory { HomeViewModel(get()) }
    factory { (scoreboardId: String) -> ScoreboardViewModel(get(), scoreboardId) }
}
