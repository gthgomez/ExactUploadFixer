package com.exactuploadfixer.domain

import java.time.LocalDate

/**
 * A one-tap preset that fills all three fields (width, height, maxBytes).
 *
 * Every preset is a size/dimension constraint set, not an eligibility certificate.
 * Matching a preset's geometry and byte cap does NOT validate identity, pose,
 * background, image content, or final acceptance by any reviewing authority.
 *
 * [authorityUrl]: the official source the constraint set was taken from, or null
 * for presets that are explicitly practical defaults with no official source.
 *
 * [applicationContext]: the specific upload route the constraints were verified
 * against. Never generalized to "all government forms" or similar.
 *
 * [byteLimitInterpretation]: how the authority's written byte/kB limit was mapped
 * to this preset's [maxBytes], with the source wording. Authorities often write
 * "240 kB" without defining kB as 1000 vs 1024 bytes; we never guess silently.
 *
 * [lastVerified]: ISO-8601 date (YYYY-MM-DD) the requirements were last manually
 * confirmed against the source. A preset older than the recheck window is stale
 * and must be visibly flagged ([needsRecheck]), never presented as current.
 */
data class Preset(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val maxBytes: Long,
    val note: String,
    val lastVerified: String,
    val authorityUrl: String? = null,
    val applicationContext: String,
    val byteLimitInterpretation: String = ""
) {
    /**
     * Recheck-window policy (90 days) per the portfolio status contract
     * (DESIGNS/TRUTH_STATUS.md: external photo presets get a 90-day review
     * threshold). This is a maintenance policy, not a guarantee the rule is
     * still valid inside the window.
     */
    fun needsRecheck(asOf: LocalDate): Boolean =
        LocalDate.parse(lastVerified).plusDays(90) < asOf
}

/**
 * The 4 locked V1 presets.
 *
 * Job Portal Avatar  — LinkedIn floor: 400×400. Conservative 300 KB app cap.
 * DV Lottery Photo   — US State Dept DV/visa digital image route: 600×600 min,
 *                      ≤240 kB. The authority writes "240 kB" without defining
 *                      kB; we apply the stricter 240,000-byte reading so the
 *                      output satisfies either interpretation. Byte-limit only.
 * Email Attachment   — No universal standard. 1600×1200 @ 500 KB is a practical default.
 * US Passport Digital Photo — US State Dept digital photo square range
 *                      (600–1200 px); 1200×1200 chosen for quality. The 500 KB
 *                      cap is an app-chosen conservative default, NOT an
 *                      authority-published byte limit.
 *
 * None of these validate identity, pose, background, or acceptance.
 */
val PRESETS: List<Preset> = listOf(
    Preset(
        id = "job_portal_avatar",
        label = "LinkedIn Profile Photo",
        width = 400,
        height = 400,
        maxBytes = 300L * 1024L,
        note = "LinkedIn minimum 400×400 px — 300 KB cap is a conservative app default, not a LinkedIn limit",
        lastVerified = "2026-04-20",
        authorityUrl = "https://www.linkedin.com/help/linkedin/answer/a549049",
        applicationContext = "LinkedIn profile photo upload (LinkedIn Help, photo requirements)",
        byteLimitInterpretation = "No published byte limit; 307200 bytes (300 KiB) is an app-chosen conservative cap, source: LinkedIn Help article above"
    ),
    Preset(
        id = "government_id_form",
        label = "DV Lottery Photo (US)",
        width = 600,
        height = 600,
        maxBytes = 240_000L,
        note = "Portrait/photo fields only — not suitable for full document scans. Size only; does not establish eligibility",
        lastVerified = "2026-04-20",
        authorityUrl = "https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos/digital-image-requirements.html",
        applicationContext = "US State Dept visa/DV lottery digital image submission (square 600×600–1200×1200 px, JPEG, color)",
        byteLimitInterpretation = "Authority states '240 kB (kilobytes)' without defining kB; applied as max 240000 bytes (stricter reading), source: travel.state.gov digital image requirements page"
    ),
    Preset(
        id = "email_attachment",
        label = "Email Attachment",
        width = 1600,
        height = 1200,
        maxBytes = 500L * 1024L,
        note = "Practical default — no universal email image size standard",
        lastVerified = "2026-04-20",
        authorityUrl = null,
        applicationContext = "Generic email attachments; practical default with no official source",
        byteLimitInterpretation = "No authority; 512000 bytes (500 KiB) is an app-chosen practical default"
    ),
    Preset(
        id = "passport_square",
        label = "US Passport Digital Photo",
        width = 1200,
        height = 1200,
        maxBytes = 500L * 1024L,
        note = "US State Dept digital photo square range is 600–1200 px; 1200×1200 chosen for quality. Dimensions only — acceptance is reviewed, not guaranteed",
        lastVerified = "2026-04-20",
        authorityUrl = "https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos/digital-image-requirements.html",
        applicationContext = "US State Dept digital photo square dimension range (600–1200 px), checked against the visa digital-image requirements page",
        byteLimitInterpretation = "No passport-specific byte limit cited; 512000 bytes (500 KiB) is an app-chosen conservative cap, source: none published for this route"
    )
)
