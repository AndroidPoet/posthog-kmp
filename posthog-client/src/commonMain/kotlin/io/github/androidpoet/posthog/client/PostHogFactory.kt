package io.github.androidpoet.posthog.client

import io.github.androidpoet.posthog.client.transport.HttpTransport
import io.github.androidpoet.posthog.client.transport.platformEngine

public fun createPostHogClient(
    apiKey: String,
    config: PostHogConfig,
): PostHogClient =
    PostHogClientImpl(
        apiKey = apiKey,
        config = config,
        transport = HttpTransport(config = config, engineFactory = platformEngine()),
    )
