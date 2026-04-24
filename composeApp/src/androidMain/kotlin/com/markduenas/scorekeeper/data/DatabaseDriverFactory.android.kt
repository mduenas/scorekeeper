package com.markduenas.scorekeeper.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.markduenas.scorekeeper.db.ScorekeeperDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ScorekeeperDatabase.Schema, context, "scorekeeper.db")
}
