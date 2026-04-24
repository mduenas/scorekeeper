package com.markduenas.scorekeeper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform