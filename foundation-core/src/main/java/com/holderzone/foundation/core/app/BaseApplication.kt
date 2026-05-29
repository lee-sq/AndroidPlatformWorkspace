package com.holderzone.foundation.core.app

import android.app.Application
import com.holderzone.foundation.core.platform.storage.MMKVUtils
import com.holderzone.foundation.core.platform.storage.StringResHelper

abstract class BaseApplication : Application() {
    protected open val initializeMMKV: Boolean = true
    protected open val initializeStringResHelper: Boolean = true

    override fun onCreate() {
        super.onCreate()
        initializeFoundation()
    }

    protected open fun initializeFoundation() {
        if (initializeMMKV) {
            MMKVUtils.init(this)
        }
        if (initializeStringResHelper) {
            StringResHelper.init(this)
        }
    }
}
