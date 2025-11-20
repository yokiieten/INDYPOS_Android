package com.indybrain.indypos_Android.data.remote.interceptor

import android.util.Log
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Simple interceptor that prints a curl command equivalent for every outbound request.
 */
class CurlLoggingInterceptor(
    private val logger: (String) -> Unit = { Log.d(TAG, it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val curlBuilder = StringBuilder("curl")

        curlBuilder.append(" -X ").append(request.method.uppercase())
        appendHeaders(request.headers, curlBuilder)
        appendBody(request.body, curlBuilder)

        curlBuilder.append(" '").append(request.url).append("'")

        logger(curlBuilder.toString())

        return chain.proceed(request)
    }

    private fun appendHeaders(headers: Headers, builder: StringBuilder) {
        for (index in 0 until headers.size) {
            val name = headers.name(index)
            val value = headers.value(index)
            builder.append(" -H ")
                .append("'")
                .append("$name: $value")
                .append("'")
        }
    }

    private fun appendBody(body: RequestBody?, builder: StringBuilder) {
        if (body == null) return

        val buffer = Buffer()
        body.writeTo(buffer)

        val charset: Charset = body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        val bodyAsString = buffer.readString(charset)

        if (bodyAsString.isNotEmpty()) {
            builder.append(" --data-raw ")
                .append("'")
                .append(bodyAsString.replace("'", "\\'"))
                .append("'")
        }
    }

    companion object {
        private const val TAG = "CurlLogger"
    }
}


