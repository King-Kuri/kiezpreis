pluginManagement {
    repositories {
        providers.gradleProperty("kiez.mavenMirror").orNull?.let { maven(it) }
        if (providers.gradleProperty("kiez.android").orNull == "true") {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        providers.gradleProperty("kiez.mavenMirror").orNull?.let { maven(it) }
        if (providers.gradleProperty("kiez.android").orNull == "true") {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
        }
        mavenCentral()
    }
}

rootProject.name = "kiezpreis"

include(":core")
include(":desktop")
// The Android module needs the Android Gradle Plugin from Google's Maven repository.
// It is enabled with -Pkiez.android=true (set in CI and on machines with the Android SDK).
if (providers.gradleProperty("kiez.android").orNull == "true") {
    include(":app")
}
