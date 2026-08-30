pluginManagement {
    repositories {
        google()
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

// Vendored in-repo DesignSystem as a regular subproject (not a composite build).
// Keeping it inside this repository makes the repo self-contained: a fresh clone
// builds without sibling-directory dependencies, and it shares the root build's
// Android SDK resolution (local.properties / ANDROID_HOME) — required for CI.
include(":designsystem")
project(":designsystem").projectDir = file("designsystem")

rootProject.name = "ExactUploadFixer"
include(":app")
