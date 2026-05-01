package com.cuee.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechController(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onFailure: () -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        stop()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition is not available")
            onFailure()
            return
        }
        recognizer = createRecognizer()
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                Log.w(TAG, "Speech recognition error: $error")
                onFailure()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                Log.d(TAG, "Speech recognition results: $matches")
                if (text.isBlank()) onFailure() else onResult(text)
            }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    fun stop() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer(): SpeechRecognizer {
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    private companion object {
        const val TAG = "CueSpeechController"
    }
}

