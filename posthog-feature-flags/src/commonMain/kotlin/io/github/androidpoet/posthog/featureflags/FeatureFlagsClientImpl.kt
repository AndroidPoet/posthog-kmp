package io.github.androidpoet.posthog.featureflags

import io.github.androidpoet.posthog.client.PostHogClient
import io.github.androidpoet.posthog.client.defaultJson
import io.github.androidpoet.posthog.client.deserialize
import io.github.androidpoet.posthog.core.models.DecideRequest
import io.github.androidpoet.posthog.core.models.DecideResponse
import io.github.androidpoet.posthog.core.models.FeatureFlag
import io.github.androidpoet.posthog.core.result.PostHogResult
import io.github.androidpoet.posthog.core.result.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Default [FeatureFlagsClient] implementation.
 *
 * Uses [PostHogClient.post] to call the `/decide/?v=3` endpoint and caches
 * the resulting flag map in a [MutableStateFlow] for synchronous reads.
 */
internal class FeatureFlagsClientImpl(
    private val client: PostHogClient,
) : FeatureFlagsClient {

    private val _flagsFlow = MutableStateFlow<Map<String, FeatureFlag>>(emptyMap())
    override val flagsFlow: StateFlow<Map<String, FeatureFlag>> = _flagsFlow.asStateFlow()

    override suspend fun loadFlags(
        distinctId: String,
        groups: JsonObject?,
        personProperties: JsonObject?,
        groupProperties: JsonObject?,
    ): PostHogResult<Map<String, FeatureFlag>> {
        val request = DecideRequest(
            apiKey = client.apiKey,
            distinctId = distinctId,
            groups = groups,
            personProperties = personProperties,
            groupProperties = groupProperties,
        )
        val body = defaultJson.encodeToString(request)

        return client.post(
            url = "${client.config.host}/decide/?v=3",
            body = body,
        ).deserialize<DecideResponse>().map { decide ->
            val flags = decide.featureFlags.map { (key, value) ->
                key to FeatureFlag(
                    key = key,
                    value = value,
                    payload = decide.featureFlagPayloads[key],
                )
            }.toMap()
            _flagsFlow.value = flags
            flags
        }
    }

    override fun isFeatureEnabled(key: String, defaultValue: Boolean): Boolean =
        _flagsFlow.value[key]?.isEnabled ?: defaultValue

    override fun getFeatureFlag(key: String): FeatureFlag? =
        _flagsFlow.value[key]

    override fun getFeatureFlagPayload(key: String): JsonElement? =
        _flagsFlow.value[key]?.payload

    override fun getAllFlags(): Map<String, FeatureFlag> =
        _flagsFlow.value

    override suspend fun reloadFlags(
        distinctId: String,
        groups: JsonObject?,
        personProperties: JsonObject?,
        groupProperties: JsonObject?,
    ): PostHogResult<Map<String, FeatureFlag>> =
        loadFlags(distinctId, groups, personProperties, groupProperties)

    override fun clearCache() {
        _flagsFlow.value = emptyMap()
    }
}
