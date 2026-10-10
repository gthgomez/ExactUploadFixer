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
 * [minBytes]: optional published MINIMUM file size. Some authorities bound the
 * file from both sides (e.g. US passport online renewal: 54 KB - 10 MB). The
 * engine prefers the highest-fitting quality, so outputs naturally sit high;
 * when even maximum quality cannot reach [minBytes], processing fails
 * explicitly instead of exporting an under-limit file.
 *
 * [lastVerified]: ISO-8601 date (YYYY-MM-DD) the requirements were last manually
 * confirmed against the source. A preset older than the recheck window is stale
 * and must be visibly flagged ([needsRecheck]), never presented as current.
 */
data class Preset(
    val id: String,
    val minBytes: Long = 0,
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
 * US Passport Online Renewal — travel.state.gov renew-online photo rules are
 *                      NOT the visa spec: the official constraint is the file's
 *                      byte range (54 KB–10 MB; JPG/JPEG/PNG/HEIC/HEIF accepted).
 *                      The square 1200×1200 geometry is APP-CHOSEN convenience,
 *                      not a published renewal requirement. Conservative mapping
 *                      of the ambiguous "KB": floor is the higher reading (54 KiB
 *                      = 55,296 bytes so a passing file also satisfies a
 *                      54,000-byte checker), ceiling is the lower reading (10 MB
 *                      = 10,000,000 bytes). The earlier v1.1 draft wrongly reused
 *                      the visa "240 kB" cap here.
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
        note = "LinkedIn minimum 400×400 px — 300 KB cap is a conservative app default, not a LinkedIn limit (LinkedIn publishes an 8 MB profile-photo maximum)",
        lastVerified = "2026-10-09",
        authorityUrl = "https://www.linkedin.com/help/linkedin/answer/1271074",
        applicationContext = "LinkedIn profile photo upload (LinkedIn Help, photo requirements)",
        byteLimitInterpretation = "LinkedIn Help specifies profile photos up to 8 MB (PNG/JPEG); no useful byte-level floor. 307200 bytes (300 KiB) is an app-chosen conservative cap, source: LinkedIn Help article above. NOTE: an earlier draft cited '20 MB', which is the messaging-attachments limit — LinkedIn's photo-SHARING help states 5 MB per image; profile photos are capped at 8 MB"
    ),
    Preset(
        id = "government_id_form",
        label = "DV Lottery Photo (US)",
        width = 600,
        height = 600,
        maxBytes = 240_000L,
        note = "Portrait/photo fields only — not suitable for full document scans. Size only; does not establish eligibility",
        lastVerified = "2026-10-09",
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
        id = "passport_online_renewal",
        label = "US Passport Online Renewal",
        width = 1200,
        height = 1200,
        minBytes = 55_296L,      // 54 KiB: conservative floor for the published "54 KB" minimum
        maxBytes = 10_000_000L,  // 10 MB decimal: conservative ceiling for the published "10 MB" maximum
        note = "US passport online renewal: file must be 54 KB–10 MB (JPG/JPEG, PNG, HEIC, or HEIF accepted). The 1200×1200 square output is an APP-CHOSEN convenience, NOT a published renewal requirement — the State Dept asks for an original, unedited photo and provides its own crop tool",
        lastVerified = "2026-10-09",
        authorityUrl = "https://travel.state.gov/content/travel/en/passports/have-passport/renew-online.html",
        applicationContext = "US passport ONLINE RENEWAL photo upload (travel.state.gov renew-online page). OFFICIAL: byte range 54 KB–10 MB; JPG/JPEG/PNG/HEIC/HEIF accepted. NOT official: the 600×600-square/1200×1200 geometry (that rule belongs to the visa digital-image route); the renewal route expects an original, unedited photo and crops in its own tool. Distinct from the visa/DV spec — do not use this preset for visa applications",
        byteLimitInterpretation = "Authority states file size 'between 54 KB and 10 MB' without defining KB/MB; mapped conservatively as min 55296 bytes (54 KiB, the higher reading) and max 10000000 bytes (10 MB decimal, the lower reading), source: travel.state.gov passport online renewal page + MyTravelGov photo upload guidance"
    )
)
