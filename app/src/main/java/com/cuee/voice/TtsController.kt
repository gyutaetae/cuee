package com.cuee.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.KOREAN
            tts?.setSpeechRate(1.0f)
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cuee-${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

