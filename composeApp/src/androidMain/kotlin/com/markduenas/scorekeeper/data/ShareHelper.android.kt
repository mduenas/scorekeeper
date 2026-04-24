package com.markduenas.scorekeeper.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent

@SuppressLint("StaticFieldLeak")
private var appContext: Context? = null

fun initShareHelper(context: Context) {
    appContext = context.applicationContext
}

actual fun shareText(text: String) {
    val context = appContext ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, "Share Scores")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
