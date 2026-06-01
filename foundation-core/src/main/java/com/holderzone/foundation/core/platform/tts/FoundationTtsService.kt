package com.holderzone.foundation.core.platform.tts

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import com.yuu.android.component.voice.tts.EngineService
import java.util.ArrayDeque
import java.util.Locale

internal class FoundationTtsService : Service() {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var options = TtsOptions()
    private val pendingTexts = ArrayDeque<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(
            this,
            { status -> onTtsInitialized(status) },
            EngineService.VOICE_SERVICE_PACKAGE_NAME,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSpeaking()
            ACTION_SHUTDOWN -> {
                shutdownTts()
                stopSelf()
            }
            else -> {
                updateOptions(intent)
                intent?.getStringExtra(EXTRA_TEXT)?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::enqueueOrSpeak)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        shutdownTts()
        super.onDestroy()
    }

    private fun onTtsInitialized(status: Int) {
        val currentTts = tts
        if (status == TextToSpeech.SUCCESS && currentTts != null) {
            val languageStatus = currentTts.setLanguage(options.locale)
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA ||
                languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e(TAG, "Language is not supported: ${options.locale}")
                return
            }

            currentTts.setSpeechRate(options.speechRate)
            currentTts.setPitch(options.pitch)
            isTtsReady = true
            flushPending()
        } else {
            Log.e(TAG, "TTS init failed: $status")
        }
    }

    private fun updateOptions(intent: Intent?) {
        options = TtsOptions(
            speechRate = intent?.getFloatExtra(EXTRA_SPEECH_RATE, options.speechRate) ?: options.speechRate,
            pitch = intent?.getFloatExtra(EXTRA_PITCH, options.pitch) ?: options.pitch,
            locale = intent?.getStringExtra(EXTRA_LANGUAGE_TAG)
                ?.takeIf { it.isNotBlank() }
                ?.let(Locale::forLanguageTag)
                ?: options.locale,
        )

        tts?.let { currentTts ->
            currentTts.setSpeechRate(options.speechRate)
            currentTts.setPitch(options.pitch)
            currentTts.setLanguage(options.locale)
        }
    }

    private fun enqueueOrSpeak(text: String) {
        if (!isTtsReady) {
            pendingTexts.add(text)
            return
        }
        speak(text)
    }

    private fun flushPending() {
        while (pendingTexts.isNotEmpty()) {
            speak(pendingTexts.removeFirst())
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, System.currentTimeMillis().toString())
    }

    private fun stopSpeaking() {
        pendingTexts.clear()
        tts?.stop()
    }

    private fun shutdownTts() {
        pendingTexts.clear()
        isTtsReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        const val ACTION_SPEAK = "com.holderzone.foundation.core.platform.tts.action.SPEAK"
        const val ACTION_STOP = "com.holderzone.foundation.core.platform.tts.action.STOP"
        const val ACTION_SHUTDOWN = "com.holderzone.foundation.core.platform.tts.action.SHUTDOWN"
        const val EXTRA_TEXT = "com.holderzone.foundation.core.platform.tts.extra.TEXT"
        const val EXTRA_SPEECH_RATE = "com.holderzone.foundation.core.platform.tts.extra.SPEECH_RATE"
        const val EXTRA_PITCH = "com.holderzone.foundation.core.platform.tts.extra.PITCH"
        const val EXTRA_LANGUAGE_TAG = "com.holderzone.foundation.core.platform.tts.extra.LANGUAGE_TAG"

        private const val TAG = "FoundationTtsService"
    }
}
