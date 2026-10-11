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
    fun `government_id_form preset is 600x600 with the cited 240000-byte cap`() {
        val p = PRESETS.first { it.id == "government_id_form" }
        assertEquals(600, p.width)
        assertEquals(600, p.height)
        assertEquals(240_000L, p.maxBytes)
    }

    @Test
    fun `email_attachment preset is 1600x1200 with 500KB cap`() {
        val p = PRESETS.first { it.id == "email_attachment" }
        assertEquals(1600, p.width)
        assertEquals(1200, p.height)
        assertEquals(500L * 1024L, p.maxBytes)
    }

    @Test
    fun `passport_online_renewal preset models the published 54 KB-10 MB bounded range`() {
        val p = PRESETS.first { it.id == "passport_online_renewal" }
        assertEquals(1200, p.width)
        assertEquals(1200, p.height)
        // Online renewal is NOT the visa spec: bounded file size 54 KB–10 MB.
        // Regression guard: earlier drafts wrongly reused the visa 240 kB cap
        // and later a 500 KiB cap — both produced inapplicable constraints.
        assertEquals(10_000_000L, p.maxBytes)
        // Conservative floor: 54 KiB (55,296 bytes), which also satisfies a
        // strict 54,000-byte checker — the higher of the two readings.
        assertEquals(55_296L, p.minBytes)
        assertTrue("min must be below max", p.minBytes < p.maxBytes)
        assertEquals(
            "https://travel.state.gov/content/travel/en/passports/have-passport/renew-online.html",
            p.authorityUrl
        )
        // Audit guard: the renewal route publishes byte bounds only. Geometry
        // (600×600 square / 1200×1200) belongs to the visa route and must be
        // presented as an app-chosen convenience, never an official rule.
        assertTrue(
            "preset must disclaim the square geometry as app-chosen",
            p.note.contains("APP-CHOSEN", ignoreCase = true)
        )
        assertTrue(
            "applicationContext must separate OFFICIAL from NOT-official claims",
            p.applicationContext.contains("OFFICIAL") &&
                p.applicationContext.contains("NOT official")
        )
        assertTrue(
            "renewal route accepts more formats than JPEG; note must not claim JPEG-only",
            p.note.contains("PNG", ignoreCase = true)
        )
    }

    @Test
    fun `visa and passport presets do not conflate their routes`() {
        val visa = PRESETS.first { it.id == "government_id_form" }
        val passport = PRESETS.first { it.id == "passport_online_renewal" }
        // The visa 240 kB cap must never leak into the passport renewal preset
        assertTrue(passport.maxBytes != visa.maxBytes)
        assertTrue(passport.applicationContext.contains("ONLINE RENEWAL"))
        assertTrue(visa.applicationContext.contains("visa"))
    }

    // ── EX01: sourced constraints, no eligibility claims ──────────────────────

    @Test
    fun `sourced presets carry a non-blank authorityUrl and application context`() {
        val sourcedIds = setOf("job_portal_avatar", "government_id_form", "passport_online_renewal")
        PRESETS.forEach { preset ->
            assertTrue("${preset.id}: applicationContext must not be blank", preset.applicationContext.isNotBlank())
            if (preset.id in sourcedIds) {
                assertTrue(
                    "${preset.id}: an officially sourced preset must cite authorityUrl",
                    !preset.authorityUrl.isNullOrBlank()
                )
            }
        }
    }

    @Test
    fun `government_id_form cites the DV lottery route instead of a generic government label`() {
        val p = PRESETS.first { it.id == "government_id_form" }
        assertTrue(
            "government_id_form: label '${p.label}' must name the specific verified route, not a generic government/ID form",
            !p.label.contains("Government", ignoreCase = true) && !p.label.contains("ID", ignoreCase = true)
        )
        val url = requireNotNull(p.authorityUrl)
        assertTrue(
            "government_id_form: authority must be a travel.state.gov source, got '$url'",
            url.startsWith("https://travel.state.gov/")
        )
    }

    @Test
    fun `government_id_form byte cap uses the stricter 240000-byte interpretation of the 240 kB rule`() {
        val p = PRESETS.first { it.id == "government_id_form" }
        assertTrue(
            "government_id_form: maxBytes ${p.maxBytes} must not exceed 240000 (the stricter cited reading of the authority's '240 kB' rule)",
            p.maxBytes <= 240_000L
        )
        assertTrue(
            "government_id_form: byteLimitInterpretation must document the cited reading",
            p.byteLimitInterpretation.isNotBlank() &&
                p.byteLimitInterpretation.contains("240000") &&
                p.byteLimitInterpretation.contains("source", ignoreCase = true)
        )
    }

    @Test
    fun `presets past the 90-day recheck window are visibly flagged by needsRecheck`() {
        PRESETS.forEach { preset ->
            val atWindowEdge = java.time.LocalDate.parse(preset.lastVerified).plusDays(90)
            assertTrue(
                "${preset.id}: must not need recheck exactly at the 90-day boundary",
                !preset.needsRecheck(atWindowEdge)
            )
            assertTrue(
                "${preset.id}: must need recheck the day after the 90-day window",
                preset.needsRecheck(atWindowEdge.plusDays(1))
            )
        }
    }

    @Test
    fun `no preset note or application context claims guaranteed acceptance`() {
        val banned = listOf("will pass", "no rejection", "always accept", "guaranteed acceptance")
        PRESETS.forEach { preset ->
            val text = "${preset.note} ${preset.applicationContext}".lowercase()
            banned.forEach { phrase ->
                assertTrue(
                    "${preset.id}: acceptance-guarantee language '$phrase' is not allowed",
                    !text.contains(phrase)
                )
            }
        }
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
