package io.github.androidpoet.posthog.featureflags

import io.github.androidpoet.posthog.core.models.FeatureFlag
import io.github.androidpoet.posthog.core.result.PostHogResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Client for evaluating PostHog feature flags via the `/decide` endpoint.
 *
 * Flags are fetched from the server, cached in memory, and exposed as both
 * synchronous lookups and a reactive [flagsFlow]. Higher-level consumers
 * can use [isFeatureEnabled] for boolean checks or [getFeatureFlag] for
 * multivariate string values.
 */
public interface FeatureFlagsClient {

    /** Fetches all feature flags for the given [distinctId] from the server. */
    public suspend fun loadFlags(
        distinctId: String,
        groups: JsonObject? = null,
        personProperties: JsonObject? = null,
        groupProperties: JsonObject? = null,
    ): PostHogResult<Map<String, FeatureFlag>>

    /** Returns whether [key] is enabled, falling back to [defaultValue] when uncached. */
    public fun isFeatureEnabled(key: String, defaultValue: Boolean = false): Boolean

    /** Returns the cached [FeatureFlag] for [key], or `null` if absent. */
    public fun getFeatureFlag(key: String): FeatureFlag?

    /** Returns the payload attached to [key], or `null` if absent. */
    public fun getFeatureFlagPayload(key: String): JsonElement?

    /** Returns all currently cached flags. */
    public fun getAllFlags(): Map<String, FeatureFlag>

    /**
     * Reloads flags from the server, replacing the cache.
     *
     * This is a convenience alias for [loadFlags] that makes intent explicit
     * at the call site.
     */
    public suspend fun reloadFlags(
        distinctId: String,
        groups: JsonObject? = null,
        personProperties: JsonObject? = null,
        groupProperties: JsonObject? = null,
    ): PostHogResult<Map<String, FeatureFlag>>

    /** Reactive stream of the cached flag map, updated after every [loadFlags] call. */
    public val flagsFlow: StateFlow<Map<String, FeatureFlag>>

    /** Clears all cached flags, resetting [flagsFlow] to an empty map. */
    public fun clearCache()
}
