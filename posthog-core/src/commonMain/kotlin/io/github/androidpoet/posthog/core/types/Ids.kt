package io.github.androidpoet.posthog.core.types

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** A PostHog distinct ID identifying a user or device. */
@JvmInline
@Serializable
public value class DistinctId(public val value: String)

/** A PostHog project API key. */
@JvmInline
@Serializable
public value class ApiKey(public val value: String)

/** The key of a feature flag. */
@JvmInline
@Serializable
public value class FeatureFlagKey(public val value: String)

/** A group type identifier (e.g. `"company"`, `"project"`). */
@JvmInline
@Serializable
public value class GroupType(public val value: String)

/** A group key identifying a specific group instance. */
@JvmInline
@Serializable
public value class GroupKey(public val value: String)
