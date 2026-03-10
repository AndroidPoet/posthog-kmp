package io.github.androidpoet.posthog.core

import io.github.androidpoet.posthog.core.models.FeatureFlag
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeatureFlagTest {

    // ── isEnabled ────────────────────────────────────────────────────

    @Test
    fun test_isEnabled_trueForBooleanTrue() {
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive(true))

        assertTrue(flag.isEnabled)
    }

    @Test
    fun test_isEnabled_falseForBooleanFalse() {
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive(false))

        assertFalse(flag.isEnabled)
    }

    @Test
    fun test_isEnabled_trueForStringTrue() {
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive("true"))

        assertTrue(flag.isEnabled)
    }

    @Test
    fun test_isEnabled_trueForMultivariateString() {
        val flag = FeatureFlag(key = "variant", value = JsonPrimitive("control"))

        assertTrue(flag.isEnabled)
    }

    @Test
    fun test_isEnabled_falseForEmptyString() {
        val flag = FeatureFlag(key = "empty", value = JsonPrimitive(""))

        assertFalse(flag.isEnabled)
    }

    @Test
    fun test_isEnabled_falseForJsonNull() {
        val flag = FeatureFlag(key = "null", value = JsonNull)

        assertFalse(flag.isEnabled)
    }

    // ── stringValue ──────────────────────────────────────────────────

    @Test
    fun test_stringValue_returnsContentForStringPrimitive() {
        val flag = FeatureFlag(key = "variant", value = JsonPrimitive("control"))

        assertEquals("control", flag.stringValue)
    }

    @Test
    fun test_stringValue_returnsContentForBooleanPrimitive() {
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive(true))

        assertEquals("true", flag.stringValue)
    }

    @Test
    fun test_stringValue_returnsNullForJsonNull() {
        val flag = FeatureFlag(key = "null", value = JsonNull)

        assertNull(flag.stringValue)
    }

    // ── payload ──────────────────────────────────────────────────────

    @Test
    fun test_payload_defaultsToNull() {
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive(true))

        assertNull(flag.payload)
    }

    @Test
    fun test_payload_holdsValueWhenProvided() {
        val payload = JsonPrimitive("extra-data")
        val flag = FeatureFlag(key = "beta", value = JsonPrimitive(true), payload = payload)

        assertEquals(payload, flag.payload)
    }
}
