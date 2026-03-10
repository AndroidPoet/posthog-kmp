package io.github.androidpoet.posthog.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Request body for the `/decide` endpoint. */
@Serializable
public data class DecideRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("groups") public val groups: JsonObject? = null,
    @SerialName("person_properties") public val personProperties: JsonObject? = null,
    @SerialName("group_properties") public val groupProperties: JsonObject? = null,
)

/** Response from the `/decide` endpoint containing feature flags. */
@Serializable
public data class DecideResponse(
    @SerialName("featureFlags") public val featureFlags: Map<String, JsonElement> = emptyMap(),
    @SerialName("featureFlagPayloads") public val featureFlagPayloads: Map<String, JsonElement> = emptyMap(),
    @SerialName("errorsWhileComputingFlags") public val errorsWhileComputingFlags: Boolean = false,
)

/** A resolved feature flag with its key, value, and optional payload. */
@Serializable
public data class FeatureFlag(
    public val key: String,
    public val value: JsonElement,
    public val payload: JsonElement? = null,
) {
    /** Whether this flag evaluates to an enabled state. */
    public val isEnabled: Boolean
        get() = when {
            value is JsonNull -> false
            value is JsonPrimitive && value.booleanOrNull != null -> value.boolean
            value is JsonPrimitive && value.contentOrNull.isNullOrEmpty() -> false
            value is JsonPrimitive -> true // multivariate string
            else -> false
        }

    /** The string representation of the flag value, if it is a primitive. */
    public val stringValue: String?
        get() = (value as? JsonPrimitive)?.contentOrNull
}
