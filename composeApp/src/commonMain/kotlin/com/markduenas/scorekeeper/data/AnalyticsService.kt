package com.markduenas.scorekeeper.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent

class AnalyticsService {

    private val analytics = Firebase.analytics

    fun logFirstOpen() {
        analytics.logEvent("first_open") {}
    }

    fun logAppLinkTap(appName: String, platform: String) {
        analytics.logEvent("app_link_tap") {
            param("app_name", appName)
            param("platform", platform)
        }
    }
}
