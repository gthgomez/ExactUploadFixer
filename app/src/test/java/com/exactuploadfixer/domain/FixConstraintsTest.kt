package com.exactuploadfixer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FixConstraintsTest {

    // ── hasDimensions ─────────────────────────────────────────────────────────

    @Test
    fun `hasDimensions is true when both width and height are set`() {
        val c = FixConstraints(maxBytes = 1024L, targetWidth = 400, targetHeight = 300)
        assertTrue(c.hasDimensions)
    }

    @Test
    fun `hasDimensions is false when both width and height are null`() {
        val c = FixConstraints(maxBytes = 1024L)
        assertFalse(c.hasDimensions)
    }

    @Test
    fun `hasDimensions is false when only width is set`() {
        val c = FixConstraints(maxBytes = 1024L, targetWidth = 400, targetHeight = null)
        assertFalse(c.hasDimensions)
    }

    @Test
    fun `hasDimensions is false when only height is set`() {
        val c = FixConstraints(maxBytes = 1024L, targetWidth = null, targetHeight = 600)
        assertFalse(c.hasDimensions)
    }

    // ── fromPreset ────────────────────────────────────────────────────────────

    @Test
    fun `fromPreset maps all preset fields correctly`() {
        val preset = Preset(
            id = "test", label = "Test", width = 800, height = 600,
            maxBytes = 512L * 1024L, note = "", lastVerified = "2026-01-01"
        )
        val c = FixConstraints.fromPreset(preset)
        assertEquals(preset.maxBytes, c.maxBytes)
        assertEquals(preset.width, c.targetWidth)
        assertEquals(preset.height, c.targetHeight)
        assertTrue(c.hasDimensions)
    }

    // ── fromKb ────────────────────────────────────────────────────────────────

    @Test
    fun `fromKb converts kilobytes to bytes correctly`() {
        val c = FixConstraints.fromKb(500L)
        assertEquals(500L * 1024L, c.maxBytes)
    }

    @Test
    fun `fromKb with dimensions sets hasDimensions true`() {
        val c = FixConstraints.fromKb(300L, width = 600, height = 600)
        assertTrue(c.hasDimensions)
        assertEquals(600, c.targetWidth)
        assertEquals(600, c.targetHeight)
    }

    @Test
    fun `fromKb without dimensions sets hasDimensions false`() {
        val c = FixConstraints.fromKb(300L)
        assertFalse(c.hasDimensions)
    }

    // ── boundary ──────────────────────────────────────────────────────────────

    @Test
    fun `maxBytes of zero is accepted by the data class`() {
        val c = FixConstraints(maxBytes = 0L)
        assertEquals(0L, c.maxBytes)
    }

    @Test
    fun `maxBytes of Long MAX_VALUE is accepted by the data class`() {
        val c = FixConstraints(maxBytes = Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, c.maxBytes)
    }
}
