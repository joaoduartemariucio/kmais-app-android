package br.com.joaoduartemariucio.rodaro.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HttpLogInterceptor(
    private val log: (String) -> Unit,
    private val clock: () -> Long = System::nanoTime,
) : Interceptor {
    private val counter = AtomicInteger()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val id = counter.incrementAndGet()
        val target = "${request.method} ${url(request.url)}"
        log(requestBlock(id, target, request))
        val start = clock()
        val response =
            try {
                chain.proceed(request)
            } catch (error: IOException) {
                log(block("#$id ✕ FAILED $target after ${elapsed(start)} ms", listOf("${error::class.simpleName}: ${error.message}")))
                throw error
            }
        log(responseBlock(id, target, response, elapsed(start)))
        return response
    }

    private fun requestBlock(
        id: Int,
        target: String,
        request: Request,
    ): String {
        val body = request.body
        val bodyText =
            when {
                body == null -> null
                body.isOneShot() || body.isDuplex() -> "(not logged)"
                else -> Buffer().also(body::writeTo).let { text(it.readByteArray(), body.contentType()) }
            }
        val contentType = body?.contentType()?.let { "Content-Type: $it" }
        return block("#$id → $target", section("Headers", headers(request.headers) + listOfNotNull(contentType)) + bodySection(bodyText))
    }

    private fun responseBlock(
        id: Int,
        target: String,
        response: Response,
        millis: Long,
    ): String {
        val peeked = response.peekBody(MAX_BODY_BYTES)
        val bodyText = text(peeked.bytes(), peeked.contentType())
        val status = listOf(response.code.toString(), response.message).filter { it.isNotBlank() }.joinToString(" ")
        return block(
            "#$id ← $status $target in $millis ms",
            section("Headers", headers(response.headers)) + bodySection(bodyText),
        )
    }

    private fun block(
        title: String,
        lines: List<String>,
    ): String =
        buildString {
            append("┌ ").append(title)
            lines.forEach { append("\n│ ").append(it) }
            append("\n└")
        }

    private fun section(
        name: String,
        lines: List<String>,
    ): List<String> = if (lines.isEmpty()) emptyList() else listOf("$name:") + lines.map { "  $it" }

    private fun bodySection(text: String?): List<String> =
        when {
            text == null -> emptyList()
            text.isEmpty() -> listOf("Body: (empty)")
            else -> listOf("Body:") + text.lines().map { "  $it" }
        }

    private fun headers(headers: Headers): List<String> =
        headers.map { (name, value) -> "$name: ${if (name.lowercase() in HIDDEN_HEADERS) REDACTED else value}" }

    private fun text(
        bytes: ByteArray,
        contentType: MediaType?,
    ): String {
        if (bytes.isEmpty()) return ""
        val isText =
            contentType == null || contentType.type == "text" || contentType.subtype.contains("json") || contentType.subtype.contains("xml")
        if (!isText) return "(${bytes.size} bytes of $contentType)"
        val raw = bytes.toString(Charsets.UTF_8)
        val pretty =
            if (contentType?.subtype?.contains("json") != false) {
                runCatching { PRETTY.encodeToString(JsonElement.serializer(), PRETTY.parseToJsonElement(raw)) }.getOrDefault(raw)
            } else {
                raw
            }
        return if (pretty.length <=
            MAX_BODY_CHARS
        ) {
            pretty
        } else {
            pretty.take(MAX_BODY_CHARS) + "\n… ${pretty.length - MAX_BODY_CHARS} more chars"
        }
    }

    private fun url(url: HttpUrl): String {
        val query =
            url.encodedQuery
                ?.split("&")
                ?.filterNot { it.substringBefore("=") in HIDDEN_PARAMETERS }
                ?.joinToString("&")
                .orEmpty()
        val port = if (url.port == HttpUrl.defaultPort(url.scheme)) "" else ":${url.port}"
        return "${url.scheme}://${url.host}$port${url.encodedPath}${if (query.isEmpty()) "" else "?$query"}"
    }

    private fun elapsed(start: Long): Long = TimeUnit.NANOSECONDS.toMillis(clock() - start)

    private companion object {
        const val MAX_BODY_BYTES = 64L * 1024
        const val MAX_BODY_CHARS = 2_500
        const val REDACTED = "██ (hidden)"
        val HIDDEN_PARAMETERS = setOf("api_key", "key", "token")
        val HIDDEN_HEADERS = setOf("authorization", "cookie", "set-cookie", "x-api-key")
        val PRETTY = Json { prettyPrint = true }
    }
}
