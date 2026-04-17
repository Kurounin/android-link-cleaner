package com.kurounin.linkcleaner

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kurounin.linkcleaner.ui.CleanerScreen
import com.kurounin.linkcleaner.ui.theme.LinkCleanerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent.extractSharedText()
        val clipboardText = if (sharedText.isNullOrBlank()) readClipboard() else null
        val initial = sharedText ?: clipboardText ?: ""
        val autoClean = !sharedText.isNullOrBlank()

        setContent {
            LinkCleanerTheme {
                CleanerScreen(initialInput = initial, autoClean = autoClean)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val shared = intent.extractSharedText() ?: return
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(Intent.EXTRA_TEXT, shared)
                action = Intent.ACTION_SEND
                type = "text/plain"
            }
        )
        finish()
    }

    private fun Intent.extractSharedText(): String? {
        if (action != Intent.ACTION_SEND) return null
        if (type != "text/plain") return null
        return getStringExtra(Intent.EXTRA_TEXT)
    }

    private fun readClipboard(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0)?.text?.toString()
    }
}
