package com.holderzone.foundation.core.network

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal class UnsafeTls private constructor(
    val socketFactory: SSLSocketFactory,
    val trustManager: X509TrustManager,
    val hostnameVerifier: HostnameVerifier,
) {
    companion object {
        fun create(): UnsafeTls {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), SecureRandom())
            }
            return UnsafeTls(
                socketFactory = sslContext.socketFactory,
                trustManager = trustManager,
                hostnameVerifier = HostnameVerifier { _, _ -> true },
            )
        }
    }
}
