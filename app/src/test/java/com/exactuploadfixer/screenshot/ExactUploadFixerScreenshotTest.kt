package com.exactuploadfixer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.PRESETS
import com.exactuploadfixer.domain.ProcessedImage
import com.exactuploadfixer.ui.AppScreen
import com.exactuploadfixer.ui.AppUiState
import com.exactuploadfixer.ui.EditScreen
import com.exactuploadfixer.ui.PickScreen
import com.exactuploadfixer.ui.ResultScreen
import com.exactuploadfixer.ui.theme.ExactUploadFixerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot tests for ExactUploadFixer.
 *
 * Run record:  ./gradlew :app:recordRoborazziGooglePlayDebug
 * Run verify:  ./gradlew :app:verifyRoborazziGooglePlayDebug
 *
 * Goldens are stored in src/test/snapshots/ and committed to version control.
 * Never use dynamicColorScheme() — it is non-reproducible on Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExactUploadFixerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun ThemedContent(
        darkTheme: Boolean,
        content: @Composable () -> Unit
    ) {
        ExactUploadFixerTheme(darkTheme = darkTheme, content = content)
    }

    // ── EditScreen ────────────────────────────────────────────────────────────

    @Test
    fun pickScreen_lightTheme_defaultState() {
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                PickScreen(onPhotoPicked = {})
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/pickScreen_lightTheme_defaultState.png")
    }

    @Test
    fun editScreen_lightTheme_defaultState() {
        val state = AppUiState(screen = AppScreen.Edit)
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                EditScreen(
                    ui = state,
                    onMaxSizeChanged = {},
                    onWidthChanged = {},
                    onHeightChanged = {},
                    onPresetSelected = {},
                    onEntitlementRefreshRequested = {},
                    onProcessClick = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/editScreen_lightTheme_defaultState.png")
    }

    @Test
    fun editScreen_darkTheme_defaultState() {
        val state = AppUiState(screen = AppScreen.Edit)
        composeTestRule.setContent {
            ThemedContent(darkTheme = true) {
                EditScreen(
                    ui = state,
                    onMaxSizeChanged = {},
                    onWidthChanged = {},
                    onHeightChanged = {},
                    onPresetSelected = {},
                    onEntitlementRefreshRequested = {},
                    onProcessClick = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/editScreen_darkTheme_defaultState.png")
    }

    @Test
    fun editScreen_lightTheme_presetSelected_proLocked() {
        val state = AppUiState(
            screen = AppScreen.Edit,
            selectedPreset = PRESETS.first(),
            isProUnlocked = false
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                EditScreen(
                    ui = state,
                    onMaxSizeChanged = {},
                    onWidthChanged = {},
                    onHeightChanged = {},
                    onPresetSelected = {},
                    onEntitlementRefreshRequested = {},
                    onProcessClick = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/editScreen_presetSelected_proLocked.png")
    }

    @Test
    fun editScreen_lightTheme_presetSelected_proUnlocked() {
        val state = AppUiState(
            screen = AppScreen.Edit,
            selectedPreset = PRESETS.first(),
            isProUnlocked = true
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                EditScreen(
                    ui = state,
                    onMaxSizeChanged = {},
                    onWidthChanged = {},
                    onHeightChanged = {},
                    onPresetSelected = {},
                    onEntitlementRefreshRequested = {},
                    onProcessClick = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/editScreen_presetSelected_proUnlocked.png")
    }

    @Test
    fun editScreen_lightTheme_withError() {
        val state = AppUiState(
            screen = AppScreen.Edit,
            editError = "Enter a valid size target"
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                EditScreen(
                    ui = state,
                    onMaxSizeChanged = {},
                    onWidthChanged = {},
                    onHeightChanged = {},
                    onPresetSelected = {},
                    onEntitlementRefreshRequested = {},
                    onProcessClick = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/editScreen_withError.png")
    }

    // ── ResultScreen ──────────────────────────────────────────────────────────

    @Test
    fun resultScreen_lightTheme_success() {
        val state = AppUiState(
            screen = AppScreen.Result,
            selectedPreset = PRESETS.first(),
            result = ProcessedImage(
                bytes = ByteArray(30_720),
                width = 400,
                height = 400,
                qualityUsed = 72,
                fileSizeBytes = 30_720L
            )
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                ResultScreen(
                    ui = state,
                    onBackToEdit = {},
                    onStartOver = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/resultScreen_success.png")
    }

    @Test
    fun resultScreen_lightTheme_compressionFailure() {
        val state = AppUiState(
            screen = AppScreen.Result,
            resultFailure = FixFailure.CompressionCouldNotMeetMaxSize
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                ResultScreen(
                    ui = state,
                    onBackToEdit = {},
                    onStartOver = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/resultScreen_compressionFailure.png")
    }

    @Test
    fun resultScreen_lightTheme_decodeFailed() {
        val state = AppUiState(
            screen = AppScreen.Result,
            resultFailure = FixFailure.DecodeFailed
        )
        composeTestRule.setContent {
            ThemedContent(darkTheme = false) {
                ResultScreen(
                    ui = state,
                    onBackToEdit = {},
                    onStartOver = {}
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage("src/test/snapshots/resultScreen_decodeFailed.png")
    }
}
