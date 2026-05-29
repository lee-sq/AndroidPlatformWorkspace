package com.holderzone.foundation.core.network

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class NetworkManager(
    private val options: NetworkOptions = NetworkOptions(),
) : ServiceFactory {
    private val baseUrlManager = BaseUrlManager(options.defaultBaseUrl)
    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()
    private val client: OkHttpClient = createClient(options)

    val currentBaseUrl: StateFlow<String?> = baseUrlManager.currentBaseUrl

    fun setBaseUrl(baseUrl: String) {
        baseUrlManager.setBaseUrl(baseUrl)
    }

    fun clearBaseUrl() {
        baseUrlManager.clearBaseUrl()
    }

    fun getCurrentBaseUrl(): String? = baseUrlManager.getCurrentBaseUrl()

    fun hasBaseUrl(): Boolean = baseUrlManager.hasBaseUrl()

    fun okHttpClient(): OkHttpClient = client

    fun retrofit(baseUrl: String): Retrofit {
        val normalizedBaseUrl = baseUrl.normalizeBaseUrl()
        return retrofitCache.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(client)
                .apply(options.retrofitConfig)
                .build()
        }
    }

    override fun <T> create(serviceClass: Class<T>): T {
        val currentUrl = getCurrentBaseUrl()
            ?: throw IllegalStateException("BaseUrl is not set.")
        return create(serviceClass, currentUrl)
    }

    override fun <T> create(serviceClass: Class<T>, baseUrl: String): T {
        return retrofit(baseUrl).create(serviceClass)
    }

    fun clearCache(baseUrl: String) {
        retrofitCache.remove(baseUrl.normalizeBaseUrl())
    }

    fun clearAllCache() {
        retrofitCache.clear()
    }

    fun switchBaseUrl(baseUrl: String, clearOldCache: Boolean = true) {
        val oldBaseUrl = getCurrentBaseUrl()
        setBaseUrl(baseUrl)
        if (clearOldCache && oldBaseUrl != null && oldBaseUrl != getCurrentBaseUrl()) {
            clearCache(oldBaseUrl)
        }
    }

    private fun createClient(options: NetworkOptions): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(options.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(options.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(options.writeTimeoutSeconds, TimeUnit.SECONDS)
            .apply {
                if (options.logLevel != HttpLogLevel.NONE) {
                    addInterceptor(
                        HttpLogInterceptor().apply {
                            level = options.logLevel.toHttpLogLevel()
                        },
                    )
                }
                if (options.unsafeTrustAllCerts) {
                    val unsafeTls = UnsafeTls.create()
                    sslSocketFactory(unsafeTls.socketFactory, unsafeTls.trustManager)
                    hostnameVerifier(unsafeTls.hostnameVerifier)
                }
                options.okHttpConfig(this)
            }
            .build()
    }

    private fun HttpLogLevel.toHttpLogLevel(): HttpLogInterceptor.Level {
        return when (this) {
            HttpLogLevel.NONE -> HttpLogInterceptor.Level.NONE
            HttpLogLevel.BASIC -> HttpLogInterceptor.Level.BASIC
            HttpLogLevel.HEADERS -> HttpLogInterceptor.Level.HEADERS
            HttpLogLevel.BODY -> HttpLogInterceptor.Level.BODY
        }
    }
}
