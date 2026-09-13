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

// Prefer the in-repo DesignSystem (CI + standalone clone). Fall back to the
// workspace sibling for local monorepo development.
val inRepoDesign = file("DesignSystem")
val siblingDesign = file("../DesignSystem")
when {
    inRepoDesign.resolve("build.gradle.kts").isFile -> includeBuild("DesignSystem")
    siblingDesign.resolve("build.gradle.kts").isFile -> includeBuild("../DesignSystem")
    else -> throw GradleException(
        "DesignSystem composite not found. Expected ./DesignSystem or ../DesignSystem."
    )
}

rootProject.name = "ExactUploadFixer"
include(":app")
