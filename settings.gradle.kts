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
// Do NOT re-add the JDK toolchain-resolver plugin here.
// F-Droid's source scanner blocks it: it auto-downloads JDKs from a
// non-FOSS-verified service. If the IDE re-adds it on Gradle sync,
// strip it before committing. Pin the build JDK via JAVA_HOME / the
// IDE Gradle JDK setting instead.

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OfficeBreak"
include(":app")
