package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.DatabaseDriverFactory
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
}
