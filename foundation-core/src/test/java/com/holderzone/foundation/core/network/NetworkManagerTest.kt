package com.holderzone.foundation.core.network

import okhttp3.Interceptor
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Converter
import retrofit2.http.GET

class NetworkManagerTest {
    @Test
    fun `default base url is normalized`() {
        val manager = NetworkManager(
            NetworkOptions(defaultBaseUrl = "https://api.example.com"),
        )

        assertEquals("https://api.example.com/", manager.getCurrentBaseUrl())
    }

    @Test
    fun `create without base url throws clear error`() {
        val manager = NetworkManager()

        val error = assertThrows(IllegalStateException::class.java) {
            manager.create(TestApi::class.java)
        }

        assertEquals("BaseUrl is not set.", error.message)
    }

    @Test
    fun `retrofit instances are cached by normalized base url`() {
        val manager = NetworkManager()

        val first = manager.retrofit("https://api.example.com")
        val second = manager.retrofit("https://api.example.com/")

        assertSame(first, second)
    }

    @Test
    fun `clearCache removes only selected base url`() {
        val manager = NetworkManager()
        val first = manager.retrofit("https://api.example.com")
        val untouched = manager.retrofit("https://other.example.com")

        manager.clearCache("https://api.example.com/")

        assertNotSame(first, manager.retrofit("https://api.example.com"))
        assertSame(untouched, manager.retrofit("https://other.example.com"))
    }

    @Test
    fun `clearAllCache removes every cached retrofit`() {
        val manager = NetworkManager()
        val first = manager.retrofit("https://api.example.com")
        val second = manager.retrofit("https://other.example.com")

        manager.clearAllCache()

        assertNotSame(first, manager.retrofit("https://api.example.com"))
        assertNotSame(second, manager.retrofit("https://other.example.com"))
    }

    @Test
    fun `custom okHttp and retrofit configuration is applied`() {
        val interceptor = Interceptor { chain -> chain.proceed(chain.request()) }
        val converterFactory = object : Converter.Factory() {}
        val manager = NetworkManager(
            NetworkOptions(
                defaultBaseUrl = "https://api.example.com",
                okHttpConfig = { addInterceptor(interceptor) },
                retrofitConfig = { addConverterFactory(converterFactory) },
            ),
        )

        assertTrue(manager.okHttpClient().interceptors.contains(interceptor))
        assertTrue(manager.retrofit("https://api.example.com").converterFactories().contains(converterFactory))
    }

    @Test
    fun `default configuration does not add body logging`() {
        val manager = NetworkManager()

        val loggingInterceptors = manager.okHttpClient().interceptors.filterIsInstance<HttpLogInterceptor>()

        assertTrue(loggingInterceptors.isEmpty())
    }

    @Test
    fun `logging interceptor is only added when requested`() {
        val manager = NetworkManager(NetworkOptions(logLevel = HttpLogLevel.HEADERS))

        val loggingInterceptor = manager.okHttpClient().interceptors
            .filterIsInstance<HttpLogInterceptor>()
            .single()

        assertEquals(HttpLogInterceptor.Level.HEADERS, loggingInterceptor.level)
        assertFalse(loggingInterceptor.level == HttpLogInterceptor.Level.BODY)
    }

    private interface TestApi {
        @GET("ping")
        fun ping(): Call<ResponseBody>
    }
}
