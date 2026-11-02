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
        // Yalnızca libadb-android (saate kablosuz kurulum, debug) ve bağımlılığı spake2 için.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\.MuntashirAkon.*") }
        }
    }
}

rootProject.name = "Akis"

include(":shared", ":mobile", ":wear")
