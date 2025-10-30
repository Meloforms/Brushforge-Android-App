pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Brushforge-Android-App"
include(
    ":app",
    ":core:common",
    ":core:ui",
    ":domain",
    ":data",
    ":feature:converter",
    ":feature:mypaints",
    ":feature:palettes",
    ":feature:primed",
    ":feature:profile"
)
