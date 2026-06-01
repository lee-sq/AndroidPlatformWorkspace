package com.holderzone.foundation.core.utils

/**
 * Auth：李天才
 * CrateTime：2026/6/1 14:47
 * Description: 构建Utils
 */
object BuildUtils {
    fun generatorVersionName(versionCode: Int): String {
        if (versionCode !in 0..999999) {
            throw IllegalArgumentException("versionCode 必须是 0-999999 之间的六位数")
        }

        val major = versionCode / 10000
        val minor = (versionCode % 10000) / 100
        val patch = versionCode % 100

        return "$major.$minor.$patch"
    }
}