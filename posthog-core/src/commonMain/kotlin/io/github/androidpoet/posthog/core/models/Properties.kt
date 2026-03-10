package io.github.androidpoet.posthog.core.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Marks DSL scope for [PropertiesBuilder]. */
@DslMarker
public annotation class PropertiesDsl

/**
 * Type-safe builder for constructing [JsonObject] property bags.
 *
 * Usage:
 * ```kotlin
 * val props = properties {
 *     "plan" to "pro"
 *     "seats" to 5
 *     "active" to true
 *     nested("address") {
 *         "city" to "San Francisco"
 *     }
 * }
 * ```
 */
@PropertiesDsl
public class PropertiesBuilder {
    private val map = mutableMapOf<String, JsonElement>()

    public infix fun String.to(value: String) { map[this] = JsonPrimitive(value) }
    public infix fun String.to(value: Int) { map[this] = JsonPrimitive(value) }
    public infix fun String.to(value: Long) { map[this] = JsonPrimitive(value) }
    public infix fun String.to(value: Double) { map[this] = JsonPrimitive(value) }
    public infix fun String.to(value: Boolean) { map[this] = JsonPrimitive(value) }
    public infix fun String.to(value: JsonElement) { map[this] = value }

    /** Sets [key] to JSON null. */
    public fun putNull(key: String) { map[key] = JsonNull }

    /** Adds a nested object under [key]. */
    public fun nested(key: String, block: PropertiesBuilder.() -> Unit) {
        map[key] = PropertiesBuilder().apply(block).build()
    }

    /** Builds the accumulated entries into a [JsonObject]. */
    public fun build(): JsonObject = JsonObject(map.toMap())
}

/** Creates a [JsonObject] using the [PropertiesBuilder] DSL. */
public inline fun properties(block: PropertiesBuilder.() -> Unit): JsonObject =
    PropertiesBuilder().apply(block).build()

/** Returns an empty [JsonObject]. */
public fun emptyProperties(): JsonObject = JsonObject(emptyMap())
