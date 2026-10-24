package de.kiezpreis.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Production [HttpClient]. Bodies above [maxBytes] are rejected to protect the device. */
class OkHttpClientAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val maxBytes: Long = 8L * 1024 * 1024,
) : HttpClient {

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return@use HttpResponse(response.code, "")
                if (body.contentLength() > maxBytes) throw HttpException("Antwort zu groß")
                HttpResponse(response.code, body.source().apply { request(maxBytes + 1) }.buffer.let { buf ->
                    if (buf.size > maxBytes) throw HttpException("Antwort zu groß")
                    buf.readUtf8()
                })
            }
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw HttpException(e.message ?: e.javaClass.simpleName, e)
        }
    }
}
