package com.memshot.app.analysis

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.memshot.app.data.Prefs
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class Analysis(val text: String, val labels: List<VisualLabel>, val tags: List<String>)

/** OCR + riconoscimento immagini (ML Kit, on-device) + classificazione. */
class Analyzer(private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build())
    }
    private val classifier = Classifier()

    suspend fun analyze(file: File, sourceApp: String?): Analysis {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val text = recognizer.process(image).await().textBlocks.joinToString("\n") { it.text }
        val labels = runCatching {
            labeler.process(image).await().map { VisualLabel(it.text, it.confidence) }
        }.getOrDefault(emptyList())
        val learned = sourceApp?.let { Prefs(context).learnedApps[appKey(it)] }
        return Analysis(text, labels, classifier.classify(text, labels, sourceApp, learned))
    }
}

/** Chiave stabile per un'app di provenienza ("Netflix", "netflix com.netflix.mediaclient" → "netflix"). */
fun appKey(sourceApp: String): String = sourceApp.trim().substringBefore(' ').lowercase()

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
