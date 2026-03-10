package io.github.androidpoet.posthog.client.transport

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual fun platformEngine(): HttpClientEngineFactory<*> = CIO
