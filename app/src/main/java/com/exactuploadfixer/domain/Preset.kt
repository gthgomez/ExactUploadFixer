package com.exactuploadfixer.domain

/**
 * A one-tap preset that fills all three fields (width, height, maxBytes).
 * Dimensions and limits are grounded in real-world upload requirements.
 *
 * [lastVerified]: ISO-8601 date (YYYY-MM-DD) the requirements were last manually confirmed
 * against the source portal. Update this whenever you re-check the spec. If a preset drifts
 * from the real portal, this is the forcing function that surfaces it.
 */
data class Preset(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val maxBytes: Long,
    val note: String,
    val lastVerified: String
)

/**
 * The 4 locked V1 presets.
 *
 * Job Portal Avatar  — LinkedIn floor: 400×400. Conservative 300 KB cap.
 * Government/ID Form — US State Dept DV program: 600×600, ≤240 KB.
 *                      NOTE: center-crop only — not safe for full ID-card scans.
 * Email Attachment   — No universal standard. 1600×1200 @ 500 KB is a practical default.
 * Passport Square    — US State Dept digital photo: 600–1200px square, JPEG.
 *                      Using 1200×1200 for max quality within spec.
 */
val PRESETS: List<Preset> = listOf(
    Preset(
        id = "job_portal_avatar",
        label = "Job Portal Avatar",
        width = 400,
        height = 400,
        maxBytes = 300L * 1024L,
        note = "Square headshot — LinkedIn minimum is 400×400",
        lastVerified = "2026-04-20"
    ),
    Preset(
        id = "government_id_form",
        label = "Government / ID Form",
        width = 600,
        height = 600,
        maxBytes = 240L * 1024L,
        note = "Portrait/photo fields only — not suitable for full document scans",
        lastVerified = "2026-04-20"
    ),
    Preset(
        id = "email_attachment",
        label = "Email Attachment",
        width = 1600,
        height = 1200,
        maxBytes = 500L * 1024L,
        note = "Practical default — no universal email image size standard",
        lastVerified = "2026-04-20"
    ),
    Preset(
        id = "passport_square",
        label = "Passport Square",
        width = 1200,
        height = 1200,
        maxBytes = 500L * 1024L,
        note = "US Dept of State digital photo spec: 600–1200px square, JPEG",
        lastVerified = "2026-04-20"
    )
)
