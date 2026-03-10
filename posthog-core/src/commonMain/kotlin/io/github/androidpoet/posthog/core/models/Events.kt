package io.github.androidpoet.posthog.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A single analytics event captured by PostHog. */
@Serializable
public data class PostHogEvent(
    @SerialName("event") public val event: String,
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("properties") public val properties: JsonObject? = null,
    @SerialName("timestamp") public val timestamp: String? = null,
)

/** Request body for the `/capture` endpoint. */
@Serializable
public data class CaptureRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("event") public val event: String,
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("properties") public val properties: JsonObject? = null,
    @SerialName("timestamp") public val timestamp: String? = null,
    @SerialName("set") public val set: JsonObject? = null,
    @SerialName("set_once") public val setOnce: JsonObject? = null,
)

/** Request body for the `/batch` endpoint. */
@Serializable
public data class BatchRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("batch") public val batch: List<CaptureRequest>,
)

/** Request body for the `$identify` event. */
@Serializable
public data class IdentifyRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("event") public val event: String = "\$identify",
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("properties") public val properties: JsonObject? = null,
    @SerialName("\$set") public val set: JsonObject? = null,
    @SerialName("\$set_once") public val setOnce: JsonObject? = null,
)

/** Request body for the `$create_alias` event. */
@Serializable
public data class AliasRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("event") public val event: String = "\$create_alias",
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("properties") public val properties: JsonObject,
)

/** Request body for the `$groupidentify` event. */
@Serializable
public data class GroupRequest(
    @SerialName("api_key") public val apiKey: String,
    @SerialName("event") public val event: String = "\$groupidentify",
    @SerialName("distinct_id") public val distinctId: String,
    @SerialName("properties") public val properties: JsonObject,
)
