package com.exactuploadfixer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetTest {

    // ── Catalog integrity ─────────────────────────────────────────────────────

    @Test
    fun `PRESETS list contains exactly 4 entries`() {
        assertEquals(4, PRESETS.size)
    }

    @Test
    fun `all preset IDs are unique`() {
        val ids = PRESETS.map { it.id }
        assertEquals("Duplicate preset IDs found", ids.size, ids.toSet().size)
    }

    @Test
    fun `all presets have positive width`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: width must be > 0", preset.width > 0)
        }
    }

    @Test
    fun `all presets have positive height`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: height must be > 0", preset.height > 0)
        }
    }

    @Test
    fun `all presets have positive maxBytes`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: maxBytes must be > 0", preset.maxBytes > 0L)
        }
    }

    @Test
    fun `all presets have non-blank labels`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: label must not be blank", preset.label.isNotBlank())
        }
    }

    @Test
    fun `all presets have non-blank lastVerified`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: lastVerified must not be blank", preset.lastVerified.isNotBlank())
        }
    }

    @Test
    fun `all presets have lastVerified in ISO-8601 format`() {
        val iso8601 = Regex("""\d{4}-\d{2}-\d{2}""")
        PRESETS.forEach { preset ->
            assertTrue(
                "${preset.id}: lastVerified '${preset.lastVerified}' must match YYYY-MM-DD",
                iso8601.matches(preset.lastVerified)
            )
        }
    }

    @Test
    fun `all presets have non-blank notes`() {
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: note must not be blank", preset.note.isNotBlank())
        }
    }

    // ── Known preset values (regression guard) ────────────────────────────────

    @Test
    fun `job_portal_avatar preset is 400x400 with 300KB cap`() {
        val p = PRESETS.first { it.id == "job_portal_avatar" }
        assertEquals(400, p.width)
        assertEquals(400, p.height)
        assertEquals(300L * 1024L, p.maxBytes)
    }

    @Test
    fun `government_id_form preset is 600x600 with 240KB cap`() {
        val p = PRESETS.first { it.id == "government_id_form" }
        assertEquals(600, p.width)
        assertEquals(600, p.height)
        assertEquals(240L * 1024L, p.maxBytes)
    }

    @Test
    fun `email_attachment preset is 1600x1200 with 500KB cap`() {
        val p = PRESETS.first { it.id == "email_attachment" }
        assertEquals(1600, p.width)
        assertEquals(1200, p.height)
        assertEquals(500L * 1024L, p.maxBytes)
    }

    @Test
    fun `passport_square preset is 1200x1200 with 500KB cap`() {
        val p = PRESETS.first { it.id == "passport_square" }
        assertEquals(1200, p.width)
        assertEquals(1200, p.height)
        assertEquals(500L * 1024L, p.maxBytes)
    }

    // ── fromPreset round-trip ─────────────────────────────────────────────────

    @Test
    fun `FixConstraints fromPreset round-trips all PRESETS correctly`() {
        PRESETS.forEach { preset ->
            val c = FixConstraints.fromPreset(preset)
            assertEquals("${preset.id}: maxBytes mismatch", preset.maxBytes, c.maxBytes)
            assertEquals("${preset.id}: width mismatch", preset.width, c.targetWidth)
            assertEquals("${preset.id}: height mismatch", preset.height, c.targetHeight)
            assertTrue("${preset.id}: hasDimensions must be true", c.hasDimensions)
        }
    }
}
