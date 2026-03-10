package io.github.androidpoet.posthog.client.transport

import io.ktor.client.engine.HttpClientEngineFactory

internal expect fun platformEngine(): HttpClientEngineFactory<*>
