package com.holderzone.foundation.core.platform.storage

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV
import java.util.Collections
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MMKVUtils {
    private var initialized = false
    private val instances = Collections.synchronizedMap(mutableMapOf<String, MMKV>())
    private val defaultMMKV: MMKV by lazy {
        checkInitialized()
        MMKV.defaultMMKV()
    }

    fun init(context: Context): String = initialize(context)

    fun initialize(context: Context): String {
        val rootDir = MMKV.initialize(context.applicationContext)
        initialized = true
        return rootDir
    }

    fun getInstance(
        name: String,
        mode: Int = MMKV.SINGLE_PROCESS_MODE,
        cryptKey: String? = null,
        rootDir: String? = null,
    ): MMKV {
        checkInitialized()
        return instances.getOrPut("$name-$mode-${cryptKey.orEmpty()}-${rootDir.orEmpty()}") {
            MMKV.mmkvWithID(name, mode, cryptKey, rootDir)
        }
    }

    fun getMultiProcessInstance(name: String, cryptKey: String? = null): MMKV {
        return getInstance(name, MMKV.MULTI_PROCESS_MODE, cryptKey)
    }

    fun getEncryptedInstance(name: String, cryptKey: String, multiProcess: Boolean = false): MMKV {
        val mode = if (multiProcess) MMKV.MULTI_PROCESS_MODE else MMKV.SINGLE_PROCESS_MODE
        return getInstance(name, mode, cryptKey)
    }

    fun putBoolean(key: String, value: Boolean) {
        defaultMMKV.encode(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return defaultMMKV.decodeBool(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        defaultMMKV.encode(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return defaultMMKV.decodeInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        defaultMMKV.encode(key, value)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return defaultMMKV.decodeLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        defaultMMKV.encode(key, value)
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return defaultMMKV.decodeFloat(key, defaultValue)
    }

    fun putDouble(key: String, value: Double) {
        defaultMMKV.encode(key, value)
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return defaultMMKV.decodeDouble(key, defaultValue)
    }

    fun putString(key: String, value: String?) {
        defaultMMKV.encode(key, value)
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return defaultMMKV.decodeString(key, defaultValue) ?: defaultValue
    }

    fun putBytes(key: String, value: ByteArray?) {
        defaultMMKV.encode(key, value)
    }

    fun getBytes(key: String): ByteArray? {
        return defaultMMKV.decodeBytes(key)
    }

    fun <T : Parcelable> putParcelable(key: String, value: T?) {
        defaultMMKV.encode(key, value)
    }

    fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>): T? {
        return defaultMMKV.decodeParcelable(key, clazz)
    }

    inline fun <reified T> putObject(key: String, value: T) {
        putString(key, Json.encodeToString(value))
    }

    inline fun <reified T> getObject(key: String): T? {
        val value = getString(key)
        if (value.isBlank()) return null
        return runCatching { Json.decodeFromString<T>(value) }.getOrNull()
    }

    fun putStringSet(key: String, value: Set<String>?) {
        defaultMMKV.encode(key, value)
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return defaultMMKV.decodeStringSet(key, defaultValue) ?: defaultValue
    }

    fun containsKey(key: String): Boolean {
        return defaultMMKV.containsKey(key)
    }

    fun contains(key: String): Boolean = containsKey(key)

    fun remove(key: String) {
        defaultMMKV.removeValueForKey(key)
    }

    fun removeValuesForKeys(keyPrefix: String) {
        val keys = defaultMMKV.allKeys()
            ?.filter { it.startsWith(keyPrefix) }
            ?.toTypedArray()
            .orEmpty()
        if (keys.isNotEmpty()) {
            defaultMMKV.removeValuesForKeys(keys)
        }
    }

    fun clearAll() {
        defaultMMKV.clearAll()
    }

    fun getAllKeys(): Set<String> {
        return defaultMMKV.allKeys()?.toSet().orEmpty()
    }

    fun allKeys(): Set<String> = getAllKeys()

    fun totalSize(): Long {
        return defaultMMKV.totalSize()
    }

    fun count(): Long {
        return defaultMMKV.count()
    }

    private fun checkInitialized() {
        check(initialized) {
            "MMKVUtils is not initialized. Call MMKVUtils.initialize(context) first."
        }
    }
}
