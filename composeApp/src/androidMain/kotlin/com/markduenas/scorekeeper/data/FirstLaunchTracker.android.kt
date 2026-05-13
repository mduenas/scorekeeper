package com.markduenas.scorekeeper.data

import android.content.Context
import com.markduenas.scorekeeper.data.appContext

actual object FirstLaunchTracker {
    actual fun isFirstLaunch(): Boolean {
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences("scorr_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("launched", false)
    }

    actual fun markLaunched() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("scorr_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("launched", true).apply()
    }
}
