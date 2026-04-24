package com.markduenas.scorekeeper.di

import com.markduenas.scorekeeper.data.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
}
