package com.exactuploadfixer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FixResultTest {

    private fun fakeImage(bytes: Int = 100, width: Int = 400, height: Int = 400) =
        ProcessedImage(
            bytes = ByteArray(bytes),
            width = width,
            height = height,
            qualityUsed = 75,
            fileSizeBytes = bytes.toLong()
        )

    @Test
    fun `Success carries processed image`() {
        val img = fakeImage(bytes = 200)
        val result: FixResult = FixResult.Success(img)
        assertTrue(result is FixResult.Success)
        assertEquals(img, (result as FixResult.Success).image)
    }

    @Test
    fun `Success isSuccess is true`() {
        val result: FixResult = FixResult.Success(fakeImage())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Degraded isSuccess is true`() {
        val result: FixResult = FixResult.Degraded(
            image = fakeImage(),
            reasons = listOf(FixDegradation.DimensionReduced(scaleFactor = 0.8f, pass = 2))
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Degraded carries reasons`() {
        val reason = FixDegradation.MemoryConstrained(sampleSize = 4, outputWidth = 1000, outputHeight = 800)
        val result = FixResult.Degraded(fakeImage(), listOf(reason))
        assertEquals(listOf(reason), result.reasons)
    }

    @Test
    fun `Failure isSuccess is false`() {
        val result: FixResult = FixResult.Failure(FixFailure.InvalidInput)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `Failure carries InvalidInput reason`() {
        val result = FixResult.Failure(FixFailure.InvalidInput)
        assertEquals(FixFailure.InvalidInput, result.reason)
    }

    @Test
    fun `Failure carries UnsupportedMimeType reason`() {
        val result = FixResult.Failure(FixFailure.UnsupportedMimeType)
        assertEquals(FixFailure.UnsupportedMimeType, result.reason)
    }

    @Test
    fun `Failure carries DecodeFailed reason`() {
        val result = FixResult.Failure(FixFailure.DecodeFailed)
        assertEquals(FixFailure.DecodeFailed, result.reason)
    }

    @Test
    fun `Failure carries MemoryBudgetExceeded reason`() {
        val result = FixResult.Failure(FixFailure.MemoryBudgetExceeded)
        assertEquals(FixFailure.MemoryBudgetExceeded, result.reason)
    }

    @Test
    fun `Failure carries ProcessingTimedOut reason`() {
        val result = FixResult.Failure(FixFailure.ProcessingTimedOut)
        assertEquals(FixFailure.ProcessingTimedOut, result.reason)
    }

    @Test
    fun `Failure carries InvalidTargetDimensions reason`() {
        val result = FixResult.Failure(FixFailure.InvalidTargetDimensions)
        assertEquals(FixFailure.InvalidTargetDimensions, result.reason)
    }

    @Test
    fun `Failure carries CompressionCouldNotMeetMaxSize reason`() {
        val result = FixResult.Failure(FixFailure.CompressionCouldNotMeetMaxSize)
        assertEquals(FixFailure.CompressionCouldNotMeetMaxSize, result.reason)
    }

    @Test
    fun `Failure carries SaveFailed reason`() {
        val result = FixResult.Failure(FixFailure.SaveFailed)
        assertEquals(FixFailure.SaveFailed, result.reason)
    }

    @Test
    fun `ProcessedImage equals based on content not reference`() {
        val bytes = ByteArray(50) { it.toByte() }
        val a = ProcessedImage(bytes, 400, 400, 80, 50L)
        val b = ProcessedImage(bytes.copyOf(), 400, 400, 80, 50L)
        assertEquals(a, b)
    }
}
