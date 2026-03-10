package io.github.androidpoet.posthog.client

import io.github.androidpoet.posthog.core.result.PostHogError
import io.github.androidpoet.posthog.core.result.PostHogResult
import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance configured for PostHog API responses.
 *
 * - Unknown keys are silently ignored (forward compatibility).
 * - Lenient parsing accepts unquoted values.
 * - Null fields are omitted from serialized output.
 */
public val defaultJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/**
 * Deserializes a raw JSON [PostHogResult] into a typed [T].
 */
public inline fun <reified T> PostHogResult<String>.deserialize(): PostHogResult<T> =
    when (this) {
        is PostHogResult.Success -> try {
            PostHogResult.Success(defaultJson.decodeFromString<T>(value))
        } catch (e: Exception) {
            PostHogResult.Failure(
                PostHogError(message = "Deserialization failed: ${e.message}"),
            )
        }
        is PostHogResult.Failure -> this
    }
