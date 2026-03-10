package io.github.androidpoet.posthog.client.transport

import io.github.androidpoet.posthog.client.PostHogConfig
import io.github.androidpoet.posthog.core.result.PostHogError
import io.github.androidpoet.posthog.core.result.PostHogResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val CLIENT_VERSION = "posthog-kmp/0.1.0"

/**
 * Low-level HTTP transport layer for PostHog API communication.
 *
 * Wraps a Ktor [HttpClient] configured with content negotiation, optional
 * logging, and the PostHog User-Agent header. Higher-level modules delegate
 * all network I/O here.
 */
internal class HttpTransport(
    private val config: PostHogConfig,
    engineFactory: HttpClientEngineFactory<*>,
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    internal val httpClient: HttpClient = HttpClient(engineFactory) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                },
            )
        }

        if (config.logging) {
            install(Logging) {
                level = config.logLevel
            }
        }

        defaultRequest {
            header("User-Agent", CLIENT_VERSION)
        }
    }

    // ── HTTP verbs ──────────────────────────────────────────────────────

    suspend fun post(
        url: String,
        body: String,
    ): PostHogResult<String> = execute {
        httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun get(
        url: String,
    ): PostHogResult<String> = execute {
        httpClient.get(url)
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    fun close() {
        httpClient.close()
    }

    // ── Internals ───────────────────────────────────────────────────────

    private suspend inline fun execute(
        crossinline request: suspend () -> io.ktor.client.statement.HttpResponse,
    ): PostHogResult<String> =
        try {
            val response = request()
            val text = response.bodyAsText()
            if (response.status.isSuccess()) {
                PostHogResult.Success(text)
            } else {
                val error = parseError(text, response.status.value)
                PostHogResult.Failure(error)
            }
        } catch (e: Exception) {
            PostHogResult.Failure(
                PostHogError(message = e.message ?: "Unknown network error"),
            )
        }

    private fun parseError(body: String, statusCode: Int): PostHogError =
        try {
            errorJson.decodeFromString<PostHogError>(body)
        } catch (_: Exception) {
            PostHogError(
                message = body.ifBlank { "HTTP $statusCode" },
                statusCode = statusCode,
            )
        }
}
