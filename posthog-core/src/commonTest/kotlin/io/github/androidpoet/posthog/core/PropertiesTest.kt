package io.github.androidpoet.posthog.core

import io.github.androidpoet.posthog.core.models.PropertiesBuilder
import io.github.androidpoet.posthog.core.models.emptyProperties
import io.github.androidpoet.posthog.core.models.properties
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PropertiesTest {

    @Test
    fun test_properties_stringValue() {
        val props = properties { "name" to "Alice" }

        assertEquals("Alice", props["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun test_properties_intValue() {
        val props = properties { "count" to 42 }

        assertEquals(42, props["count"]?.jsonPrimitive?.int)
    }

    @Test
    fun test_properties_longValue() {
        val props = properties { "big" to 9_999_999_999L }

        assertEquals(9_999_999_999L, props["big"]?.jsonPrimitive?.long)
    }

    @Test
    fun test_properties_doubleValue() {
        val props = properties { "ratio" to 3.14 }

        assertEquals(3.14, props["ratio"]?.jsonPrimitive?.double)
    }

    @Test
    fun test_properties_booleanValue() {
        val props = properties { "active" to true }

        assertEquals(true, props["active"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun test_properties_jsonElementValue() {
        val element = JsonPrimitive("raw")
        val props = properties { "raw" to element }

        assertEquals(element, props["raw"])
    }

    @Test
    fun test_properties_nullValue() {
        val props = properties { putNull("gone") }

        assertIs<JsonNull>(props["gone"])
    }

    @Test
    fun test_properties_nestedObject() {
        val props = properties {
            nested("address") {
                "city" to "SF"
                "zip" to 94105
            }
        }

        val address = props["address"]?.jsonObject
        assertEquals("SF", address?.get("city")?.jsonPrimitive?.content)
        assertEquals(94105, address?.get("zip")?.jsonPrimitive?.int)
    }

    @Test
    fun test_emptyProperties_returnsEmptyJsonObject() {
        val props = emptyProperties()

        assertTrue(props.isEmpty())
    }

    @Test
    fun test_properties_multipleEntries() {
        val props = properties {
            "a" to 1
            "b" to "two"
            "c" to true
        }

        assertEquals(3, props.size)
    }
}
