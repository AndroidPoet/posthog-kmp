package io.github.androidpoet.posthog.client

import io.github.androidpoet.posthog.client.transport.HttpTransport
import io.github.androidpoet.posthog.client.transport.platformEngine

/**
 * Entry point for creating a [PostHogClient].
 *
 * ```kotlin
 * val client = PostHog.create("phc_...") {
 *     host = "https://eu.i.posthog.com"
 *     flushAt = 10
 *     logging = true
 *     logLevel = LogLevel.BODY
 * }
 * ```
 */
public object PostHog {

    /**
     * Creates a new [PostHogClient] for the given project.
     *
     * @param apiKey    The PostHog project API key (e.g. `phc_...`).
     * @param configure Optional DSL block to customize host, batching, logging, etc.
     */
    public fun create(
        apiKey: String,
        configure: PostHogConfigBuilder.() -> Unit = {},
    ): PostHogClient {
        val config = PostHogConfigBuilder().apply(configure).build()
        val transport = HttpTransport(
            config = config,
            engineFactory = platformEngine(),
        )
        return PostHogClientImpl(
            apiKey = apiKey,
            config = config,
            transport = transport,
        )
    }
}
