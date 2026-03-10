package io.github.androidpoet.posthog.core

import io.github.androidpoet.posthog.core.types.ApiKey
import io.github.androidpoet.posthog.core.types.DistinctId
import io.github.androidpoet.posthog.core.types.FeatureFlagKey
import io.github.androidpoet.posthog.core.types.GroupKey
import io.github.androidpoet.posthog.core.types.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals

class IdsTest {

    @Test
    fun test_distinctId_wrapsString() {
        val id = DistinctId("user_abc123")
        assertEquals("user_abc123", id.value)
    }

    @Test
    fun test_apiKey_wrapsString() {
        val key = ApiKey("phc_1234567890abcdef")
        assertEquals("phc_1234567890abcdef", key.value)
    }

    @Test
    fun test_featureFlagKey_wrapsString() {
        val key = FeatureFlagKey("new-dashboard")
        assertEquals("new-dashboard", key.value)
    }

    @Test
    fun test_groupType_wrapsString() {
        val type = GroupType("company")
        assertEquals("company", type.value)
    }

    @Test
    fun test_groupKey_wrapsString() {
        val key = GroupKey("acme-inc")
        assertEquals("acme-inc", key.value)
    }
}
