package io.github.androidpoet.posthog.client

import io.ktor.client.plugins.logging.LogLevel

/**
 * DSL builder for [PostHogConfig].
 *
 * ```kotlin
 * PostHog.create("phc_...") {
 *     host = "https://eu.i.posthog.com"
 *     flushAt = 10
 *     logging = true
 *     logLevel = LogLevel.HEADERS
 * }
 * ```
 */
@PostHogConfigDsl
public class PostHogConfigBuilder {
    /** PostHog ingestion host. Defaults to US cloud. */
    public var host: String = "https://us.i.posthog.com"

    /** Number of queued events that triggers an automatic flush. */
    public var flushAt: Int = 20

    /** Interval in milliseconds between automatic flushes. */
    public var flushIntervalMs: Long = 30_000L

    /** Maximum number of events held in memory before oldest are dropped. */
    public var maxQueueSize: Int = 1000

    /** Enable Ktor HTTP logging. Defaults to `false`. */
    public var logging: Boolean = false

    /** Ktor log level. Only effective when [logging] is `true`. */
    public var logLevel: LogLevel = LogLevel.NONE

    /** When `true`, all capture/identify/group calls become no-ops. */
    public var optOut: Boolean = false

    /** Eagerly fetch feature flags on client creation. */
    public var preloadFeatureFlags: Boolean = true

    /** Automatically send `$feature_flag_called` events. */
    public var sendFeatureFlagEvents: Boolean = true

    /** Controls when person profiles are created in PostHog. */
    public var personProfiles: PersonProfilesMode = PersonProfilesMode.IDENTIFIED_ONLY

    internal fun build(): PostHogConfig = PostHogConfig(
        host = host,
        flushAt = flushAt,
        flushIntervalMs = flushIntervalMs,
        maxQueueSize = maxQueueSize,
        logging = logging,
        logLevel = logLevel,
        optOut = optOut,
        preloadFeatureFlags = preloadFeatureFlags,
        sendFeatureFlagEvents = sendFeatureFlagEvents,
        personProfiles = personProfiles,
    )
}

/**
 * Immutable configuration snapshot for the PostHog client.
 */
public data class PostHogConfig(
    val host: String,
    val flushAt: Int,
    val flushIntervalMs: Long,
    val maxQueueSize: Int,
    val logging: Boolean,
    val logLevel: LogLevel,
    val optOut: Boolean,
    val preloadFeatureFlags: Boolean,
    val sendFeatureFlagEvents: Boolean,
    val personProfiles: PersonProfilesMode,
)

/**
 * Controls when PostHog creates person profiles for users.
 */
public enum class PersonProfilesMode {
    /** Person profiles are created only when [PostHogClient.identify] is called. */
    IDENTIFIED_ONLY,

    /** Person profiles are created for every distinct ID. */
    ALWAYS,
}

@DslMarker
public annotation class PostHogConfigDsl
