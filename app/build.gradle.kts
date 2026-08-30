plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Roborazzi Gradle plugin omitted: version 1.28.0 uses the removed TestedExtension API
    // from AGP < 9.0. Library-only mode is used instead — see screenshot test README comments.
}

android {
    namespace = "com.exactuploadfixer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.exactuploadfixer"
        minSdk = 26                  // Android 8.0 — safe floor for Samsung/Pixel
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Store flavors ─────────────────────────────────────────────────────────
    // Each flavor provides its own BillingModule.kt via a flavor source set.
    // The BillingGateway interface and all other code stay in src/main.
    //
    //   googlePlay → src/googlePlay/  uses PlayBillingGateway
    //                build command:   ./gradlew bundleGooglePlayRelease   → AAB for Play Console
    //
    //   amazon     → src/amazon/      uses RevenueCatBillingGateway (Amazon IAP via RC)
    //                build command:   ./gradlew assembleAmazonRelease     → APK for Amazon Developer Console
    //
    // Source: skill_amazon_appstore Step 8 (dual-store build strategy)
    flavorDimensions += "store"
    productFlavors {
        create("googlePlay") {
            dimension = "store"
        }
        create("amazon") {
            dimension = "store"
        }
    }

    // ── Signing ───────────────────────────────────────────────────────────────
    // Credentials via environment variables only — NEVER hardcode in build files.
    // Set before running a release build:
    //   export KEYSTORE_PATH=/path/to/your.jks
    //   export KEYSTORE_PASSWORD=...
    //   export KEY_ALIAS=...
    //   export KEY_PASSWORD=...
    //
    // Same keystore works for both Google Play (upload key) and Amazon.
    // Source: skill_android_release_build Step 2
    // Signing config is created only when KEYSTORE_PATH env var is present.
    // Without it: debug builds work; release builds produce unsigned APK.
    // This avoids "Cannot convert '' to File" when env vars are absent (CI, dev machines).
    val keystorePath = System.getenv("KEYSTORE_PATH")
    if (!keystorePath.isNullOrEmpty()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Apply signing config only if it was created (env vars present)
            val signingCfg = runCatching { signingConfigs.getByName("release") }.getOrNull()
            if (signingCfg != null) signingConfig = signingCfg
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Forward roborazzi mode properties from Gradle project properties to test JVM.
                // Record goldens: ./gradlew :app:testGooglePlayDebugUnitTest -Proborazzi.test.record=true
                // Verify goldens: ./gradlew :app:testGooglePlayDebugUnitTest -Proborazzi.test.verify=true
                // Default (neither): RECORD_IF_MISSING — captures if no golden exists, else compares.
                listOf("roborazzi.test.record", "roborazzi.test.verify").forEach { key ->
                    project.findProperty(key)?.let { value ->
                        test.systemProperty(key, value.toString())
                    }
                }

                // Amazon's IAP SDK ships bytecode that Robolectric's JVM test
                // runner rejects during class verification, before app code runs.
                // This is test-only; Android/Fire runtime verification is unchanged.
                if (test.name.contains("Amazon", ignoreCase = true)) {
                    test.jvmArgs("-noverify")
                }
            }
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — pins all Compose versions together
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Photo Picker — zero storage permissions
    implementation(libs.androidx.activity)

    // EXIF orientation normalization (ChatGPT risk #4)
    implementation(libs.androidx.exifinterface)

    // Google Play Billing — googlePlay flavor ONLY
    // Not included in amazon APK: Play Billing crashes on Fire OS (skill_amazon_appstore Step 2)
    "googlePlayImplementation"(libs.billing)

    // RevenueCat — amazon flavor ONLY (RC v9 requires both core + store adapter)
    // RC core (purchases) provides the Purchases class and CustomerInfo API.
    // purchases-store-amazon routes RC to Amazon IAP when configured with AmazonConfiguration.
    // Play Billing classes are present in the AAR (purchases core dep) but never called —
    // RC routes to Amazon IAP based on configuration, not classpath presence.
    "amazonImplementation"(libs.revenuecat)
    "amazonImplementation"(libs.revenuecat.amazon)
    "amazonImplementation"(libs.revenuecat.ui)

    // FileProvider for share
    implementation(libs.androidx.core)

    // Image loading for thumbnail preview
    implementation(libs.coil.compose)

    // Vendored in-repo DesignSystem — GlassCard components, AppTheme, brand tokens
    implementation(project(":designsystem"))

    debugImplementation(libs.androidx.ui.tooling)


    // Test dependencies (unit tests — no APK impact)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    // Instrumented test dependencies
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    // Screenshot tests (Roborazzi — runs on JVM via Robolectric, no device needed)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ── BillingConfig placeholder guard ──────────────────────────────────────────
// Fails any amazon release build if BillingConfig.kt still contains the placeholder
// API key. Covers both APK (assembleAmazonRelease) and AAB (bundleAmazonRelease).
// Prevents a misconfigured build from silently shipping to the store.
afterEvaluate {
    tasks.matching {
        it.name == "preAmazonReleaseBuild" ||
            it.name == "assembleAmazonRelease" ||
            it.name == "bundleAmazonRelease"
    }
        .configureEach {
            doFirst {
                val requiredSigningEnv = listOf(
                    "KEYSTORE_PATH",
                    "KEYSTORE_PASSWORD",
                    "KEY_ALIAS",
                    "KEY_PASSWORD"
                )
                val missing = requiredSigningEnv.filter { System.getenv(it).isNullOrBlank() }
                if (missing.isNotEmpty()) {
                    error(
                        "Amazon release builds must be signed.\n" +
                        "Missing environment variables: ${missing.joinToString(", ")}\n" +
                        "Set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD " +
                        "before running assembleAmazonRelease."
                    )
                }

                val keystoreFile = file(System.getenv("KEYSTORE_PATH")!!)
                if (!keystoreFile.isFile) {
                    error("KEYSTORE_PATH does not point to a keystore file: ${keystoreFile.absolutePath}")
                }
            }

            doFirst {
                val configFile = file(
                    "src/amazon/java/com/exactuploadfixer/billing/BillingConfig.kt"
                )
                val configText = if (configFile.exists()) configFile.readText() else ""
                if (configText.contains("REPLACE_ME") || configText.contains("test_")) {
                    error(
                        "BillingConfig.kt still contains placeholder values.\n" +
                        "Replace REVENUECAT_API_KEY (and verify ENTITLEMENT_PRO + AMAZON_PRODUCT_IDS) " +
                        "before building a release artifact."
                    )
                }
            }
        }
}
