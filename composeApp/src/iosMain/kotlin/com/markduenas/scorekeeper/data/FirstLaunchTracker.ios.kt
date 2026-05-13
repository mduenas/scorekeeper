package com.markduenas.scorekeeper.data

import platform.Foundation.NSUserDefaults

actual object FirstLaunchTracker {
    actual fun isFirstLaunch(): Boolean {
        return !NSUserDefaults.standardUserDefaults.boolForKey("launched")
    }

    actual fun markLaunched() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "launched")
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
