package com.holderzone.foundation.core.platform.logging

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.yuu.android.component.logbook.LogbookRequest
import com.yuu.android.component.logbook.LogbookResponse
import com.yuu.android.component.logbook.config.LogStorageLevel
import com.yuu.android.component.logbook.model.LogbookProtocol
import com.yuu.android.component.logbook.strategy.LogbookStrategy
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class FoundationLogbookServerStrategy(
    private val application: Application,
    private val serverUrl: String,
) : LogbookStrategy {
    override fun recordable(): Boolean = true

    override fun verify(request: LogbookRequest): LogbookProtocol? {
        if (request.priority.level < LogStorageLevel.DEBUG.level) return null
        return JsonLogbookProtocol(buildPayload(request).toString())
    }

    override fun record(response: LogbookResponse) {
        val payload = response.log ?: return
        runCatching {
            postJson(payload)
        }.onFailure { throwable ->
            Log.e(TAG, "Upload app log failed: ${throwable.message}", throwable)
        }
    }

    private fun buildPayload(request: LogbookRequest): JSONObject {
        return JSONObject().apply {
            put("chain", request.chain)
            put("logcat", request.logcat)
            put("tag", request.tag)
            put("label", request.label ?: DEFAULT_LABEL)
            put("priority", request.priority.meaning)
            put("throwable", request.throwable?.stackTraceToString())
            put("appVersionName", appVersionName())
            put("deviceModel", Build.MODEL.orEmpty())
            put("deviceTimestamp", now())
            put("deviceID", deviceId())
        }
    }

    private fun postJson(payload: String) {
        val connection = (URL(serverUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Connection", "close")
            useCaches = false
            doOutput = true
            doInput = true
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }
            val responseCode = connection.responseCode
            val responseBody = readResponse(connection, responseCode)
            if (responseCode !in HTTP_SUCCESS_RANGE) {
                throw IllegalStateException("HTTP $responseCode: $responseBody")
            }
            Log.d(TAG, "Uploaded app log: HTTP $responseCode")
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in HTTP_SUCCESS_RANGE) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun appVersionName(): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, 0)
            }
            packageInfo.versionName.orEmpty()
        }.getOrDefault("")
    }

    @SuppressLint("HardwareIds")
    private fun deviceId(): String {
        return Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    private fun now(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
    }

    private class JsonLogbookProtocol(
        private val payload: String,
    ) : LogbookProtocol {
        override fun pipeline(): String = payload
    }

    private companion object {
        private const val TAG = "AppLogger"
        private const val DEFAULT_LABEL = "DEFAULT_LOG"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private val HTTP_SUCCESS_RANGE = 200..299
    }
}
