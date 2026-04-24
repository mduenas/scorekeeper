package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.repository.ScorekeeperRepository
import org.koin.dsl.module

val appModule = module {
    single { ScorekeeperRepository(get()) }
}
