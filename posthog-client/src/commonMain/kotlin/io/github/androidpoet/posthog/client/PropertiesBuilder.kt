package io.github.androidpoet.posthog.client

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * DSL builder for constructing event properties as a [JsonObject].
 *
 * ```kotlin
 * client.capture("button_clicked") {
 *     "screen" to "home"
 *     "button_id" to "cta_main"
 *     "count" to 3
 * }
 * ```
 */
@PostHogConfigDsl
public class PropertiesBuilder {
    private val map = mutableMapOf<String, JsonElement>()

    public infix fun String.to(value: String) {
        map[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Number) {
        map[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Boolean) {
        map[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: JsonElement) {
        map[this] = value
    }

    internal fun build(): JsonObject = JsonObject(map.toMap())
}
