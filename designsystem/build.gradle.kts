plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

group = "com.workspace"
version = "1.0"

android {
    namespace = "com.workspace.design"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    // HazeState appears in GlassCard's public signature, so this must be `api`
    // for consumers of the DesignSystem to resolve the type.
    api(libs.haze)
}
