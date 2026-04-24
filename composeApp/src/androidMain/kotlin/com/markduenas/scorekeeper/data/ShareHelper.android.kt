package com.markduenas.scorekeeper.data

import android.content.Context
import android.content.Intent
import org.koin.java.KoinJavaComponent.get

actual fun shareText(text: String) {
    val context: Context = get(Context::class.java)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, "Share Scores")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
