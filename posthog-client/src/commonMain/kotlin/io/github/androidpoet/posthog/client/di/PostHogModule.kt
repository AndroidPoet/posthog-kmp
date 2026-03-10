package io.github.androidpoet.posthog.client.di

import io.github.androidpoet.posthog.client.PostHogClient
import io.github.androidpoet.posthog.client.PostHogClientImpl
import io.github.androidpoet.posthog.client.PostHogConfig
import io.github.androidpoet.posthog.client.transport.HttpTransport
import io.github.androidpoet.posthog.client.transport.platformEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Creates a Koin [Module] that provides [HttpTransport] and [PostHogClient]
 * as singletons.
 *
 * ```kotlin
 * startKoin {
 *     modules(postHogModule("phc_...", config))
 * }
 * ```
 */
public fun postHogModule(
    apiKey: String,
    config: PostHogConfig,
): Module = module {
    single {
        HttpTransport(
            config = config,
            engineFactory = platformEngine(),
        )
    }
    single<PostHogClient> {
        PostHogClientImpl(
            apiKey = apiKey,
            config = config,
            transport = get(),
        )
    }
}
