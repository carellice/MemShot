package com.memshot.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.memshot.app.work.WorkScheduler
import kotlinx.coroutines.launch

/** Riceve "Condividi → Salva in MemShot", importa e si chiude subito: zero attrito. */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = extractUris(intent)
        if (uris.isEmpty()) { finish(); return }
        val app = application as MemShotApp
        lifecycleScope.launch {
            val ids = app.importer.import(uris)
            if (ids.isNotEmpty()) WorkScheduler.analyze(app)
            val msg = when {
                ids.isEmpty() -> "Già presente in MemShot"
                ids.size == 1 -> "Salvato in MemShot"
                else -> "${ids.size} screenshot salvati in MemShot"
            }
            Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
            // Se serve, Android chiede conferma per togliere l'originale dalla galleria
            val request = app.cleaner.pendingRequest()
            if (request != null) trashRequest.launch(IntentSenderRequest.Builder(request).build()) else finish()
        }
    }

    private val trashRequest = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        (application as MemShotApp).cleaner.clearPending()
        finish()
    }

    @Suppress("DEPRECATION")
    private fun extractUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableExtra(Intent.EXTRA_STREAM)
        )
        Intent.ACTION_SEND_MULTIPLE ->
            (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)).orEmpty()
        else -> emptyList()
    }
}
