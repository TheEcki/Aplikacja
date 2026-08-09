pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "VitalTrack"

include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:settings")
include(":core:security")
include(":core:bluetooth")
include(":core:sync")
include(":core:healthconnect")
include(":core:analytics")
include(":core:testing")
include(":core:log")
include(":device:api")
include(":device:amazfit")
include(":feature:today")
include(":feature:activity")
include(":feature:heartrate")
include(":feature:sleep")
include(":feature:devices")
include(":feature:trends")
include(":feature:profile")
include(":feature:onboarding")
