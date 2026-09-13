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

// The vendored in-repo DesignSystem is committed and authoritative, so it is
// always used. This keeps CI and standalone clones self-contained. The previous
// "../DesignSystem" sibling fallback was unreachable (the in-repo copy always
// exists) and has been removed to make the precedence explicit.
includeBuild("DesignSystem")

rootProject.name = "ExactUploadFixer"
include(":app")
