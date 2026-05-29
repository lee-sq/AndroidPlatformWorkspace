package com.holderzone.foundation.core.network

import android.util.Log
import java.io.EOFException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.nio.charset.Charset
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

class HttpLogInterceptor @JvmOverloads constructor(
    private val logger: Logger = Logger.DEFAULT,
    private val maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES,
) : Interceptor {
    @Volatile
    var level: Level = Level.NONE

    @Volatile
    private var headersToRedact: Set<String> = TreeSet(String.CASE_INSENSITIVE_ORDER).apply {
        addAll(DEFAULT_REDACTED_HEADERS)
    }

    fun redactHeader(name: String) {
        headersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER).apply {
            addAll(headersToRedact)
            add(name)
        }
    }

    fun redactHeaders(vararg names: String) {
        headersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER).apply {
            addAll(headersToRedact)
            addAll(names)
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentLevel = level
        val request = chain.request()
        if (currentLevel == Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = currentLevel == Level.BODY
        val logHeaders = logBody || currentLevel == Level.HEADERS

        logger.log(buildRequestMessage(chain, request.body, logHeaders, logBody))

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (throwable: Throwable) {
            logger.log("<-- HTTP FAILED: $throwable")
            throw throwable
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        logger.log(buildResponseMessage(response, tookMs, logHeaders, logBody))
        return response
    }

    private fun buildRequestMessage(
        chain: Interceptor.Chain,
        requestBody: RequestBody?,
        logHeaders: Boolean,
        logBody: Boolean,
    ): String {
        val request = chain.request()
        val message = StringBuilder()
        val protocol = chain.connection()?.protocol()
        message.append("--> ")
            .append(request.method)
            .append(' ')
            .append(request.url)
        if (protocol != null) {
            message.append(' ').append(protocol)
        }
        if (!logHeaders && requestBody != null) {
            message.append(" (").append(safeContentLength(requestBody)).append("-byte body)")
        }
        message.append(SEPARATOR)

        if (!logHeaders) {
            return message.toString()
        }

        appendRequestHeaders(message, request.headers, requestBody)
        message.append(SEPARATOR)
        appendRequestBody(message, request.method, request.headers, requestBody, logBody)
        return message.toString()
    }

    private fun appendRequestHeaders(
        message: StringBuilder,
        headers: Headers,
        requestBody: RequestBody?,
    ) {
        if (requestBody != null) {
            requestBody.contentType()?.let { contentType ->
                message.append("Content-Type: ").append(contentType).append(SEPARATOR)
            }
            val contentLength = safeContentLength(requestBody)
            if (contentLength != -1L) {
                message.append("Content-Length: ").append(contentLength).append(SEPARATOR)
            }
        }

        for (index in 0 until headers.size) {
            val name = headers.name(index)
            if (!name.equals("Content-Type", ignoreCase = true) &&
                !name.equals("Content-Length", ignoreCase = true)
            ) {
                message.append(headerMessage(headers, index))
            }
        }
    }

    private fun appendRequestBody(
        message: StringBuilder,
        method: String,
        headers: Headers,
        requestBody: RequestBody?,
        logBody: Boolean,
    ) {
        if (!logBody || requestBody == null) {
            message.append("--> END ").append(method).append(SEPARATOR)
            return
        }
        if (bodyHasUnknownEncoding(headers)) {
            message.append("--> END ").append(method).append(" (encoded body omitted)").append(SEPARATOR)
            return
        }
        if (requestBody.isDuplex()) {
            message.append("--> END ").append(method).append(" (duplex request body omitted)").append(SEPARATOR)
            return
        }
        if (requestBody.isOneShot()) {
            message.append("--> END ").append(method).append(" (one-shot request body omitted)").append(SEPARATOR)
            return
        }

        val contentLength = safeContentLength(requestBody)
        if (contentLength == -1L || contentLength > maxBodyBytes) {
            message.append("--> END ")
                .append(method)
                .append(" (")
                .append(contentLength)
                .append("-byte body omitted)")
                .append(SEPARATOR)
            return
        }

        val buffer = Buffer()
        requestBody.writeTo(buffer)
        if (!isPlaintext(buffer)) {
            message.append("--> END ")
                .append(method)
                .append(" (binary ")
                .append(contentLength)
                .append("-byte body omitted)")
                .append(SEPARATOR)
            return
        }

        val charset = requestBody.contentType()?.charset(UTF8) ?: UTF8
        if (contentLength != 0L) {
            message.append(buffer.readString(charset)).append(SEPARATOR).append(SEPARATOR)
        }
        message.append("--> END ")
            .append(method)
            .append(" (")
            .append(contentLength)
            .append("-byte body)")
            .append(SEPARATOR)
    }

    private fun buildResponseMessage(
        response: Response,
        tookMs: Long,
        logHeaders: Boolean,
        logBody: Boolean,
    ): String {
        val message = StringBuilder()
        val responseBody = response.body
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        message.append("<-- ")
            .append(response.code)
        if (response.message.isNotEmpty()) {
            message.append(' ').append(response.message)
        }
        message.append(' ')
            .append(response.request.url)
            .append(" (")
            .append(tookMs)
            .append("ms")
        if (!logHeaders) {
            message.append(", ").append(bodySize).append(" body")
        }
        message.append(')').append(SEPARATOR)

        if (!logHeaders) {
            return message.toString()
        }

        val headers = response.headers
        for (index in 0 until headers.size) {
            message.append(headerMessage(headers, index))
        }

        if (!logBody || !responseHasBody(response)) {
            message.append("<-- END HTTP").append(SEPARATOR)
            return message.toString()
        }
        if (bodyHasUnknownEncoding(headers)) {
            message.append("<-- END HTTP (encoded body omitted)").append(SEPARATOR)
            return message.toString()
        }

        appendResponseBody(message, response, contentLength)
        return message.toString()
    }

    private fun appendResponseBody(
        message: StringBuilder,
        response: Response,
        contentLength: Long,
    ) {
        val responseBody = response.body
        val peekedBody = response.peekBody(maxBodyBytes)
        val source = peekedBody.source()
        source.request(Long.MAX_VALUE)
        val buffer = source.buffer.clone()

        if (!isPlaintext(buffer)) {
            message.append(SEPARATOR)
                .append("<-- END HTTP (binary ")
                .append(buffer.size)
                .append("-byte body omitted)")
                .append(SEPARATOR)
            return
        }

        val charset = responseBody.contentType()?.charset(UTF8) ?: UTF8
        if (buffer.size != 0L) {
            message.append(SEPARATOR)
                .append(buffer.readString(charset))
                .append(SEPARATOR)
        }

        message.append(SEPARATOR)
            .append("<-- END HTTP (")
            .append(peekedBody.contentLength())
            .append("-byte")
        if (contentLength > maxBodyBytes || (contentLength == -1L && peekedBody.contentLength() == maxBodyBytes)) {
            message.append(", truncated")
        }
        message.append(" body)").append(SEPARATOR)
    }

    private fun responseHasBody(response: Response): Boolean {
        if (response.request.method.equals("HEAD", ignoreCase = true)) {
            return false
        }

        val responseCode = response.code
        if ((responseCode < HTTP_CONTINUE_STATUS || responseCode >= 200) &&
            responseCode != HTTP_NO_CONTENT &&
            responseCode != HTTP_NOT_MODIFIED
        ) {
            return true
        }

        return response.headersContentLength() != -1L ||
            response.header("Transfer-Encoding").equals("chunked", ignoreCase = true)
    }

    private fun Response.headersContentLength(): Long {
        return header("Content-Length")?.toLongOrNull() ?: -1L
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
            !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun headerMessage(headers: Headers, index: Int): String {
        val name = headers.name(index)
        val value = if (headersToRedact.contains(name)) REDACTED_VALUE else headers.value(index)
        return "$name: $value$SEPARATOR"
    }

    private fun safeContentLength(requestBody: RequestBody): Long {
        return runCatching { requestBody.contentLength() }.getOrDefault(-1L)
    }

    enum class Level {
        NONE,
        BASIC,
        HEADERS,
        BODY,
    }

    fun interface Logger {
        fun log(message: String)

        companion object {
            val DEFAULT: Logger = Logger { message ->
                Log.i(DEFAULT_TAG, message)
            }
        }
    }

    companion object {
        private const val DEFAULT_TAG = "HttpLog"
        private const val HTTP_CONTINUE_STATUS = 100
        private const val SEPARATOR = "\n"
        private const val REDACTED_VALUE = "██"
        private const val DEFAULT_MAX_BODY_BYTES = 64L * 1024L
        private val UTF8: Charset = Charset.forName("UTF-8")
        private val DEFAULT_REDACTED_HEADERS = setOf(
            "Authorization",
            "Cookie",
            "Set-Cookie",
        )

        internal fun isPlaintext(buffer: Buffer): Boolean {
            return try {
                val prefix = Buffer()
                val byteCount = minOf(buffer.size, 64)
                buffer.copyTo(prefix, 0, byteCount)
                for (index in 0 until 16) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                true
            } catch (_: EOFException) {
                false
            }
        }
    }
}
