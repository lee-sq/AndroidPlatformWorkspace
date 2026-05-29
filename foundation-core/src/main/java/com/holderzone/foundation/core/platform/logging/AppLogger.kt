package com.holderzone.foundation.core.platform.logging

import android.app.Application
import android.util.Log
import com.yuu.android.component.logbook.Logbook
import com.yuu.android.component.logbook.config.LogbookConfig
import com.yuu.android.component.logbook.config.LoggerConfig
import com.yuu.android.component.logbook.strategy.LogbookStrategyFile
import com.yuu.android.component.logbook.strategy.LogbookStrategyServer

object AppLogger {
    private const val DEFAULT_TAG = "AppLogger"

    var enabled: Boolean = true
    var hasInit: Boolean = false
        private set

    private var fallbackTag: String = DEFAULT_TAG

    fun init(application: Application, config: LoggerInitConfig) {
        val builder = LogbookConfig.Builder()
            .setLoggerConfig(
                LoggerConfig(
                    isShowThreadInfo = config.showThreadInfo,
                    methodCount = config.methodCount,
                ),
            )

        if (config.enableFileStrategy && !config.fileDir.isNullOrBlank()) {
            builder.addLogbookStrategy(LogbookStrategyFile(config.fileDir))
        }

        if (config.enableServerStrategy && !config.serverUrl.isNullOrBlank()) {
            builder.addLogbookStrategy(LogbookStrategyServer(config.serverUrl))
        }

        Logbook.init(application, builder.build())
        hasInit = true
    }

    fun t(tag: String?): AppLogger {
        fallbackTag = tag ?: DEFAULT_TAG
        if (hasInit) {
            Logbook.t(tag)
        }
        return this
    }

    fun label(label: String?): AppLogger {
        fallbackTag = label ?: DEFAULT_TAG
        if (hasInit) {
            Logbook.label(label)
        }
        return this
    }

    fun d(message: String, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.d(message, *args)
        } else {
            Log.d(fallbackTag, formatMessage(message, args))
        }
    }

    fun i(message: String, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.i(message, *args)
        } else {
            Log.i(fallbackTag, formatMessage(message, args))
        }
    }

    fun w(message: String, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.w(message, *args)
        } else {
            Log.w(fallbackTag, formatMessage(message, args))
        }
    }

    fun e(message: String, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.e(message, *args)
        } else {
            Log.e(fallbackTag, formatMessage(message, args))
        }
    }

    fun e(message: String, throwable: Throwable, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.e(message, throwable, *args)
        } else {
            Log.e(fallbackTag, formatMessage(message, args), throwable)
        }
    }

    fun v(message: String, vararg args: Any?) {
        if (!enabled) return
        if (hasInit) {
            Logbook.e(message, *args)
        } else {
            Log.v(fallbackTag, formatMessage(message, args))
        }
    }

    fun json(json: String) {
        if (!enabled) return
        if (hasInit) {
            Logbook.json(json)
        } else {
            Log.d(fallbackTag, json)
        }
    }

    fun xml(xml: String) {
        if (!enabled) return
        if (hasInit) {
            Logbook.xml(xml)
        } else {
            Log.d(fallbackTag, xml)
        }
    }

    fun d(tag: String, message: String) {
        t(tag).d(message)
    }

    fun i(tag: String, message: String) {
        t(tag).i(message)
    }

    fun w(tag: String, message: String) {
        t(tag).w(message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) {
            t(tag).e(message, throwable)
        } else {
            t(tag).e(message)
        }
    }

    fun debug(tag: String, message: String) {
        d(tag, message)
    }

    fun info(tag: String, message: String) {
        i(tag, message)
    }

    fun warn(tag: String, message: String) {
        w(tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        e(tag, message, throwable)
    }

    private fun formatMessage(message: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return message
        return runCatching { message.format(*args) }
            .getOrElse { "$message ${args.joinToString(prefix = "[", postfix = "]")}" }
    }
}
