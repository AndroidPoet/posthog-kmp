package io.github.androidpoet.posthog.featureflags

import io.github.androidpoet.posthog.client.defaultJson
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Deserializes the payload of [key] into [T], or returns `null` when the
 * flag is absent or has no payload.
 */
public inline fun <reified T> FeatureFlagsClient.getTypedPayload(key: String): T? {
    val payload = getFeatureFlagPayload(key) ?: return null
    return defaultJson.decodeFromJsonElement<T>(payload)
}

/**
 * Returns the string value of the flag at [key], or [defaultValue] when
 * the flag is absent or has no string representation.
 */
public fun FeatureFlagsClient.getStringFlag(
    key: String,
    defaultValue: String = "",
): String = getFeatureFlag(key)?.stringValue ?: defaultValue

/**
 * Convenience alias for [FeatureFlagsClient.isFeatureEnabled].
 */
public fun FeatureFlagsClient.getBooleanFlag(
    key: String,
    defaultValue: Boolean = false,
): Boolean = isFeatureEnabled(key, defaultValue)
