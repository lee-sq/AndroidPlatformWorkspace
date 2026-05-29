package com.holderzone.foundation.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class NetworkOptions(
    val defaultBaseUrl: String? = null,
    val connectTimeoutSeconds: Long = 15,
    val readTimeoutSeconds: Long = 15,
    val writeTimeoutSeconds: Long = 15,
    val logLevel: HttpLogLevel = HttpLogLevel.NONE,
    val unsafeTrustAllCerts: Boolean = false,
    val okHttpConfig: OkHttpClient.Builder.() -> Unit = {},
    val retrofitConfig: Retrofit.Builder.() -> Unit = {},
)
