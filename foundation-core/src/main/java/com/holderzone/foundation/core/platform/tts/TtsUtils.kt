package com.holderzone.foundation.core.platform.tts

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yuu.android.component.voice.tts.EngineService
import java.util.ArrayDeque

object TtsUtils {
    private const val TAG = "TtsUtils"

    private val pendingTexts = ArrayDeque<String>()
    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var options: TtsOptions = TtsOptions()

    @Volatile
    private var isInitializing = false

    @Volatile
    private var isReady = false

    fun init(application: Application, options: TtsOptions = TtsOptions()) {
        synchronized(lock) {
            this.options = options
            appContext = application.applicationContext
            if (isInitializing || isReady) {
                flushPendingLocked()
                return
            }
            isInitializing = true
        }

        EngineService.checkVoiceService(application.applicationContext) {
            synchronized(lock) {
                isReady = true
                isInitializing = false
                flushPendingLocked()
            }
        }
    }

    fun speak(text: String): Boolean {
        val content = text.trim()
        if (content.isEmpty()) {
            return false
        }

        synchronized(lock) {
            if (appContext == null || !isReady) {
                pendingTexts.add(content)
                return true
            }
            return startSpeakServiceLocked(content)
        }
    }

    fun stop() {
        synchronized(lock) {
            pendingTexts.clear()
            appContext?.startService(
                Intent(appContext, FoundationTtsService::class.java)
                    .setAction(FoundationTtsService.ACTION_STOP),
            )
        }
    }

    fun shutdown() {
        synchronized(lock) {
            pendingTexts.clear()
            isReady = false
            isInitializing = false
            appContext?.startService(
                Intent(appContext, FoundationTtsService::class.java)
                    .setAction(FoundationTtsService.ACTION_SHUTDOWN),
            )
            appContext = null
        }
    }

    private fun flushPendingLocked() {
        while (pendingTexts.isNotEmpty()) {
            if (!startSpeakServiceLocked(pendingTexts.removeFirst())) {
                return
            }
        }
    }

    private fun startSpeakServiceLocked(text: String): Boolean {
        val context = appContext ?: return false
        return runCatching {
            context.startService(
                Intent(context, FoundationTtsService::class.java)
                    .setAction(FoundationTtsService.ACTION_SPEAK)
                    .putExtra(FoundationTtsService.EXTRA_TEXT, text)
                    .putExtra(FoundationTtsService.EXTRA_SPEECH_RATE, options.speechRate)
                    .putExtra(FoundationTtsService.EXTRA_PITCH, options.pitch)
                    .putExtra(FoundationTtsService.EXTRA_LANGUAGE_TAG, options.locale.toLanguageTag()),
            )
            true
        }.getOrElse { throwable ->
            Log.e(TAG, "Failed to start TTS service", throwable)
            false
        }
    }
}
