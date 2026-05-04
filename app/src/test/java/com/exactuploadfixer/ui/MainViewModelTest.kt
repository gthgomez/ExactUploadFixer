package com.exactuploadfixer.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixDegradation
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.ProcessedImage
import com.exactuploadfixer.domain.PRESETS
import com.exactuploadfixer.fakes.FakeBillingGateway
import com.exactuploadfixer.fakes.FakeUploadFixerEngine
import com.exactuploadfixer.fakes.fakeFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * Unit tests for [MainViewModel].
 *
 * Uses Robolectric so that android.net.Uri is available on the JVM.
 * Uses StandardTestDispatcher so coroutine execution is explicit —
 * call advanceUntilIdle() after each action before asserting state.
 *
 * State is read directly via vm.uiState (mutableStateOf) — no Turbine needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val cleanupFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        cleanupFiles.forEach { it.delete() }
        cleanupFiles.clear()
        Dispatchers.resetMain()
    }

    private fun buildVm(
        engine: FakeUploadFixerEngine = FakeUploadFixerEngine(),
        billing: FakeBillingGateway = FakeBillingGateway()
    ) = MainViewModel(
        ApplicationProvider.getApplicationContext<Application>(),
        engine,
        billing
    )

    private fun buildJpegUri(): Uri {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = File(context.cacheDir, "vm_test_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        cleanupFiles += file
        return Uri.fromFile(file)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Pick screen not processing not pro`() = runTest(testDispatcher) {
        val vm = buildVm()
        advanceUntilIdle()

        assertEquals(AppScreen.Pick, vm.uiState.screen)
        assertFalse(vm.uiState.isProcessing)
        assertFalse(vm.uiState.isProUnlocked)
        assertNull(vm.uiState.selectedUri)
        assertNull(vm.uiState.result)
        assertNull(vm.uiState.resultFailure)
    }

    // ── onPhotoPicked ─────────────────────────────────────────────────────────

    @Test
    fun `onPhotoPicked with valid URI advances to Edit screen`() = runTest(testDispatcher) {
        val vm = buildVm()
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)

        assertEquals(AppScreen.Edit, vm.uiState.screen)
        assertEquals(jpegUri, vm.uiState.selectedUri)
    }

    @Test
    fun `onPhotoPicked with null URI is ignored`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onPhotoPicked(null)

        assertEquals(AppScreen.Pick, vm.uiState.screen)
        assertNull(vm.uiState.selectedUri)
    }

    @Test
    fun `onPhotoPicked clears previous result and error`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        // Simulate a prior result by picking, filling, processing
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("500")
        vm.onProcessClick()
        advanceUntilIdle()

        // Pick a new photo — should reset
        vm.onPhotoPicked(jpegUri)
        assertNull(vm.uiState.result)
        assertNull(vm.uiState.resultFailure)
        assertNull(vm.uiState.editError)
        assertEquals("", vm.uiState.maxSizeKbInput)
    }

    // ── onProcessClick — success path ─────────────────────────────────────────

    @Test
    fun `onProcessClick with valid inputs sets isProcessing then delivers success`() =
        runTest(testDispatcher) {
            val engine = FakeUploadFixerEngine()
            val vm = buildVm(engine = engine)
            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            vm.onMaxSizeChanged("500")

            vm.onProcessClick()
            // Before advancing: engine hasn't run yet
            assertTrue(vm.uiState.isProcessing)

            advanceUntilIdle()
            // After: result delivered
            assertFalse(vm.uiState.isProcessing)
            assertNotNull(vm.uiState.result)
            assertNull(vm.uiState.resultFailure)
            assertEquals(AppScreen.Result, vm.uiState.screen)
        }

    @Test
    fun `onProcessClick passes correct constraints to engine`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("300")
        vm.onWidthChanged("600")
        vm.onHeightChanged("600")

        vm.onProcessClick()
        advanceUntilIdle()

        val constraints = engine.lastConstraints
        assertNotNull(constraints)
        assertEquals(300L * 1024L, constraints!!.maxBytes)
        assertEquals(600, constraints.targetWidth)
        assertEquals(600, constraints.targetHeight)
    }

    @Test
    fun `onProcessClick calls engine exactly once per click`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("500")

        vm.onProcessClick()
        advanceUntilIdle()

        assertEquals(1, engine.callCount)
    }

    // ── onProcessClick — failure path ─────────────────────────────────────────

    @Test
    fun `onProcessClick with engine failure delivers failure state to Result screen`() =
        runTest(testDispatcher) {
            val engine = FakeUploadFixerEngine()
            engine.emissions = listOf(fakeFailure(FixFailure.DecodeFailed))
            val vm = buildVm(engine = engine)
            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            vm.onMaxSizeChanged("500")

            vm.onProcessClick()
            advanceUntilIdle()

            assertFalse(vm.uiState.isProcessing)
            assertNull(vm.uiState.result)
            assertEquals(FixFailure.DecodeFailed, vm.uiState.resultFailure)
            assertEquals(AppScreen.Result, vm.uiState.screen)
        }

    @Test
    fun `onProcessClick with CompressionCouldNotMeetMaxSize failure is surfaced correctly`() =
        runTest(testDispatcher) {
            val engine = FakeUploadFixerEngine()
            engine.emissions = listOf(fakeFailure(FixFailure.CompressionCouldNotMeetMaxSize))
            val vm = buildVm(engine = engine)
            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            vm.onMaxSizeChanged("1") // impossibly small

            vm.onProcessClick()
            advanceUntilIdle()

            assertEquals(FixFailure.CompressionCouldNotMeetMaxSize, vm.uiState.resultFailure)
        }

    @Test
    fun `onProcessClick with degraded output keeps result and marks fallback`() =
        runTest(testDispatcher) {
            val degradedImage = ProcessedImage(
                bytes = ByteArray(512),
                width = 320,
                height = 240,
                qualityUsed = 68,
                fileSizeBytes = 512L
            )
            val engine = FakeUploadFixerEngine()
            engine.emissions = listOf(
                FixResult.Processing(quality = 68),
                FixResult.Fallback(scaleFactor = 0.8f, pass = 2),
                FixResult.Degraded(
                    image = degradedImage,
                    reasons = listOf(
                        FixDegradation.DimensionReduced(scaleFactor = 0.8f, pass = 2)
                    )
                )
            )
            val vm = buildVm(engine = engine)
            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            vm.onMaxSizeChanged("500")

            vm.onProcessClick()
            advanceUntilIdle()

            assertFalse(vm.uiState.isProcessing)
            assertTrue(vm.uiState.isFallback)
            assertEquals(degradedImage, vm.uiState.result)
            assertNull(vm.uiState.resultFailure)
            assertEquals(AppScreen.Result, vm.uiState.screen)
        }

    // ── onProcessClick — input validation ─────────────────────────────────────

    @Test
    fun `onProcessClick without selectedUri is a no-op`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        // No photo picked — selectedUri is null

        vm.onProcessClick()
        advanceUntilIdle()

        assertEquals(0, engine.callCount)
        assertEquals(AppScreen.Pick, vm.uiState.screen)
    }

    @Test
    fun `onProcessClick with empty maxSizeKb sets editError`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        // maxSizeKbInput left empty

        vm.onProcessClick()

        assertNotNull(vm.uiState.editError)
        assertEquals(0, engine.callCount)
    }

    @Test
    fun `onProcessClick with only width set sets editError`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("500")
        vm.onWidthChanged("600")
        // height left empty — one-sided

        vm.onProcessClick()

        assertNotNull(vm.uiState.editError)
        assertEquals(0, engine.callCount)
    }

    // ── onPresetSelected ──────────────────────────────────────────────────────

    @Test
    fun `onPresetSelected fills all three input fields`() = runTest(testDispatcher) {
        val vm = buildVm()
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        val preset = PRESETS.first { it.id == "job_portal_avatar" }

        vm.onPresetSelected(preset)

        assertEquals(preset.width.toString(), vm.uiState.widthInput)
        assertEquals(preset.height.toString(), vm.uiState.heightInput)
        assertEquals((preset.maxBytes / 1024L).toString(), vm.uiState.maxSizeKbInput)
        assertEquals(preset, vm.uiState.selectedPreset)
        assertNull(vm.uiState.editError)
    }

    @Test
    fun `manual field edit after preset clears selectedPreset`() = runTest(testDispatcher) {
        val vm = buildVm()
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onPresetSelected(PRESETS.first())

        vm.onWidthChanged("800")

        assertNull(vm.uiState.selectedPreset)
    }

    // ── onBackToEdit / onStartOver ────────────────────────────────────────────

    @Test
    fun `onBackToEdit returns to Edit screen and clears result`() = runTest(testDispatcher) {
        val vm = buildVm()
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("500")
        vm.onProcessClick()
        advanceUntilIdle()

        vm.onBackToEdit()

        assertEquals(AppScreen.Edit, vm.uiState.screen)
        assertNull(vm.uiState.result)
        assertNull(vm.uiState.resultFailure)
    }

    @Test
    fun `onStartOver resets to Pick screen preserving billing state`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        billing.simulatePurchase()
        val vm = buildVm(billing = billing)
        advanceUntilIdle() // let billing state propagate

        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onStartOver()

        assertEquals(AppScreen.Pick, vm.uiState.screen)
        assertNull(vm.uiState.selectedUri)
        // Billing state is preserved across start-over
        assertTrue(vm.uiState.isProUnlocked)
    }

    // ── Billing state ─────────────────────────────────────────────────────────

    @Test
    fun `billing purchase flips isProUnlocked to true`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        val vm = buildVm(billing = billing)
        advanceUntilIdle()
        assertFalse(vm.uiState.isProUnlocked)

        billing.simulatePurchase()
        advanceUntilIdle()

        assertTrue(vm.uiState.isProUnlocked)
    }

    @Test
    fun `billing revoke flips isProUnlocked back to false`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        billing.simulatePurchase()
        val vm = buildVm(billing = billing)
        advanceUntilIdle()
        assertTrue(vm.uiState.isProUnlocked)

        billing.simulateRevoke()
        advanceUntilIdle()

        assertFalse(vm.uiState.isProUnlocked)
    }

    @Test
    fun `billing connect and refreshEntitlement are called on init`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        buildVm(billing = billing)
        advanceUntilIdle()

        assertTrue(billing.connectCallCount >= 1)
        assertTrue(billing.refreshCallCount >= 1)
    }

    // ── onResume re-verification ──────────────────────────────────────────────

    @Test
    fun `onResume calls refreshEntitlement again`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        val vm = buildVm(billing = billing)
        advanceUntilIdle()
        val countAfterInit = billing.refreshCallCount

        vm.onResume()
        advanceUntilIdle()

        assertTrue(
            "refreshEntitlement must be called again on resume",
            billing.refreshCallCount > countAfterInit
        )
    }

    @Test
    fun `onResume re-verification picks up a revoked entitlement`() = runTest(testDispatcher) {
        val billing = FakeBillingGateway()
        billing.simulatePurchase()
        val vm = buildVm(billing = billing)
        advanceUntilIdle()
        assertTrue(vm.uiState.isProUnlocked)

        // Simulate backend revoke happening while app was backgrounded
        billing.simulateRevoke()
        vm.onResume()
        advanceUntilIdle()

        assertFalse(vm.uiState.isProUnlocked)
    }

    // ── Preset-gated processing ───────────────────────────────────────────────

    @Test
    fun `onProcessClick with preset passes preset constraints to engine`() =
        runTest(testDispatcher) {
            val engine = FakeUploadFixerEngine()
            val billing = FakeBillingGateway()
            billing.simulatePurchase()
            val vm = buildVm(engine = engine, billing = billing)
            advanceUntilIdle()

            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            val preset = PRESETS.first { it.id == "passport_square" }
            vm.onPresetSelected(preset)
            vm.onProcessClick()
            advanceUntilIdle()

            val constraints = engine.lastConstraints
            assertNotNull(constraints)
            assertEquals(preset.maxBytes, constraints!!.maxBytes)
            assertEquals(preset.width, constraints.targetWidth)
            assertEquals(preset.height, constraints.targetHeight)
            assertTrue(constraints.hasDimensions)
        }

    @Test
    fun `onProcessClick with preset succeeds and lands on Result screen`() =
        runTest(testDispatcher) {
            val engine = FakeUploadFixerEngine()
            val billing = FakeBillingGateway()
            billing.simulatePurchase()
            val vm = buildVm(engine = engine, billing = billing)
            advanceUntilIdle()

            val jpegUri = buildJpegUri()
            vm.onPhotoPicked(jpegUri)
            vm.onPresetSelected(PRESETS.first { it.id == "government_id_form" })
            vm.onProcessClick()
            advanceUntilIdle()

            assertEquals(AppScreen.Result, vm.uiState.screen)
            assertNotNull(vm.uiState.result)
            assertNull(vm.uiState.resultFailure)
        }

    @Test
    fun `onProcessClick is blocked when isProcessing is true`() = runTest(testDispatcher) {
        val engine = FakeUploadFixerEngine()
        val vm = buildVm(engine = engine)
        val jpegUri = buildJpegUri()
        vm.onPhotoPicked(jpegUri)
        vm.onMaxSizeChanged("500")

        // First click starts processing; second click before idle should be a no-op
        vm.onProcessClick()
        vm.onProcessClick()
        advanceUntilIdle()

        assertEquals(1, engine.callCount)
    }
}
