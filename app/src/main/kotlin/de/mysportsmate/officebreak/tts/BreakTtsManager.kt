package de.mysportsmate.officebreak.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class BreakTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (!isReady) {
            Log.w(TAG, "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String, locale: Locale) {
        if (!isReady) return

        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS language not supported: $locale")
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        isReady = false
    }

    companion object {
        private const val TAG = "BreakTtsManager"
        private const val UTTERANCE_ID = "break_announcement"
    }
}
