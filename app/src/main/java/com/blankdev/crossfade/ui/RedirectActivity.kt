package com.blankdev.crossfade.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankdev.crossfade.utils.LinkProcessor

class RedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout, using transparent theme from AndroidManifest
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }

        val url = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedText?.let { extractUrlFromText(it) }
                } else null
            }
            else -> null
        }

        if (url != null) {
            LinkProcessor.processUrl(this, lifecycleScope, url) { _ ->
                finish()
            }
        } else {
            finish()
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val urlRegex = "(https?://\\S+)".toRegex()
        val matchResult = urlRegex.find(text)
        return matchResult?.value
    }
}
