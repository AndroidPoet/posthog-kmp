pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "posthog-kmp"

include(":posthog-core")
include(":posthog-client")
include(":posthog-feature-flags")
