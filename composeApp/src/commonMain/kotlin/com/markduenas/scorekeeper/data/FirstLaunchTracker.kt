package com.markduenas.scorekeeper.data

expect object FirstLaunchTracker {
    fun isFirstLaunch(): Boolean
    fun markLaunched()
}
