package com.markduenas.scorekeeper.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.markduenas.scorekeeper.db.ScorekeeperDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(ScorekeeperDatabase.Schema, "scorekeeper.db")
}
