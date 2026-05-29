package com.holderzone.foundation.core.platform.storage

import android.content.Context
import androidx.annotation.StringRes

object StringResHelper {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun init(context: Context) {
        initialize(context)
    }

    fun getString(@StringRes resId: Int): String {
        return requireContext().getString(resId)
    }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return requireContext().getString(resId, *formatArgs)
    }

    private fun requireContext(): Context {
        return requireNotNull(appContext) {
            "StringResHelper is not initialized. Call StringResHelper.initialize(context) first."
        }
    }
}
