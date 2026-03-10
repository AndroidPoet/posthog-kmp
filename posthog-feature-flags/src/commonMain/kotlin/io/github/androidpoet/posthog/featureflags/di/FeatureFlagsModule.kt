package io.github.androidpoet.posthog.featureflags.di

import io.github.androidpoet.posthog.featureflags.FeatureFlagsClient
import io.github.androidpoet.posthog.featureflags.FeatureFlagsClientImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module providing [FeatureFlagsClient].
 *
 * Requires a [io.github.androidpoet.posthog.client.PostHogClient] to be
 * available in the dependency graph (typically from `postHogModule`).
 */
public val featureFlagsModule: Module = module {
    singleOf(::FeatureFlagsClientImpl) bind FeatureFlagsClient::class
}
