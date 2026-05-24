package io.github.androidpoet.posthog.featureflags

import io.github.androidpoet.posthog.client.PostHogClient

public fun createFeatureFlagsClient(client: PostHogClient): FeatureFlagsClient =
    FeatureFlagsClientImpl(client)
