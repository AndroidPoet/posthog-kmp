package io.github.androidpoet.posthog.client

import io.github.androidpoet.posthog.core.result.PostHogResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The primary interface for the PostHog analytics SDK.
 *
 * Consumers obtain an instance via [PostHog.create] and use it to capture
 * events, identify users, create aliases, and manage groups. Events are
 * queued in memory and flushed automatically or on demand.
 */
public interface PostHogClient {

    /** The project API key used for authentication. */
    public val apiKey: String

    /** The active configuration snapshot. */
    public val config: PostHogConfig

    /** The current distinct ID for this device/user. */
    public val distinctId: String

    // ── Event capture ───────────────────────────────────────────────────

    /** Captures an event with optional pre-built [properties]. */
    public suspend fun capture(event: String, properties: JsonObject? = null)

    /** Captures an event with properties built via a [PropertiesBuilder] DSL. */
    public suspend fun capture(event: String, properties: PropertiesBuilder.() -> Unit)

    // ── User identification ─────────────────────────────────────────────

    /**
     * Associates a [distinctId] with known user properties.
     *
     * @param distinctId         The unique user identifier.
     * @param userProperties     Properties to `$set` (overwrites existing values).
     * @param userPropertiesSetOnce Properties to `$set_once` (only sets if not already present).
     */
    public suspend fun identify(
        distinctId: String,
        userProperties: JsonObject? = null,
        userPropertiesSetOnce: JsonObject? = null,
    )

    // ── Alias ───────────────────────────────────────────────────────────

    /** Creates an alias linking [alias] to the current [distinctId]. */
    public suspend fun alias(alias: String)

    // ── Group ───────────────────────────────────────────────────────────

    /** Associates the current user with a group. */
    public suspend fun group(
        groupType: String,
        groupKey: String,
        groupProperties: JsonObject? = null,
    )

    // ── Super properties ────────────────────────────────────────────────

    /** Registers a super property that is merged into every subsequent event. */
    public fun register(key: String, value: JsonElement)

    /** Removes a previously registered super property. */
    public fun unregister(key: String)

    // ── Opt in/out ──────────────────────────────────────────────────────

    /** Re-enables event capture after [optOut]. */
    public fun optIn()

    /** Disables all event capture. Events are silently dropped. */
    public fun optOut()

    /** Returns `true` when event capture is disabled. */
    public val isOptedOut: Boolean

    // ── Queue management ────────────────────────────────────────────────

    /** Flushes all queued events to the PostHog server immediately. */
    public suspend fun flush()

    /** Resets the client: generates a new distinct ID, clears super properties and the queue. */
    public fun reset()

    // ── Raw HTTP ──────────────────────────────────────────────────────

    /** Performs a POST request to the given absolute [url] with a JSON [body]. */
    public suspend fun post(
        url: String,
        body: String,
    ): PostHogResult<String>

    // ── Lifecycle ───────────────────────────────────────────────────────

    /** Flushes remaining events, cancels timers, and releases HTTP resources. */
    public fun close()
}
