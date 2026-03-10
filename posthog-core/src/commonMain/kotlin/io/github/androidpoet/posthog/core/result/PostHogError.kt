package io.github.androidpoet.posthog.core.result

import kotlinx.serialization.Serializable

/**
 * Represents an error returned by the PostHog API.
 *
 * PostHog errors carry a human-readable [message] plus optional metadata:
 * an HTTP [statusCode] and a machine-readable error [type].
 */
@Serializable
public data class PostHogError(
    public val message: String,
    public val statusCode: Int? = null,
    public val type: String? = null,
)

/**
 * Exception wrapper around [PostHogError] for use in throw/catch flows.
 */
public class PostHogException(
    public val error: PostHogError,
) : Exception(error.message)

/**
 * Converts this [PostHogError] into a throwable [PostHogException].
 */
public fun PostHogError.toException(): PostHogException = PostHogException(this)
