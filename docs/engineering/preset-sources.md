# ExactUploadFixer — preset sources and interpretations

Every official preset in `app/src/main/java/com/exactuploadfixer/domain/Preset.kt`
is a **size/dimension constraint set only**. It does not validate identity, pose,
background, image content, or acceptance. No preset guarantees acceptance by any
authority; final acceptance is always decided by the receiving site.

## Preset source table

| Preset id | Label | Constraints | authorityUrl | Application context | Byte-limit interpretation | lastVerified |
| --- | --- | --- | --- | --- | --- | --- |
| `job_portal_avatar` | LinkedIn Profile Photo | 400×400 px, ≤ 307200 bytes (300 KiB) | https://www.linkedin.com/help/linkedin/answer/a549049 | LinkedIn profile photo upload (LinkedIn Help, photo requirements). 400×400 is the LinkedIn minimum; the 300 KiB byte cap is a **conservative app default, not a LinkedIn limit**. | No published byte limit; app-chosen conservative cap. | 2026-04-20 |
| `government_id_form` | DV Lottery Photo (US) | 600×600 px, ≤ **240000 bytes** | https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos/digital-image-requirements.html | US State Dept visa/DV lottery digital image submission (square 600×600–1200×1200 px, JPEG, color). This route is NOT generalized to other government forms. | Authority page states "240 kB (kilobytes)" without defining kB as 1000 vs 1024 bytes. Applied as **max 240000 bytes** — the stricter decimal reading — so output satisfies either interpretation. Checked against the live page on 2026-09-29 (E65). | 2026-04-20 |
| `email_attachment` | Email Attachment | 1600×1200 px, ≤ 512000 bytes (500 KiB) | *(none — no universal standard exists)* | Generic email attachments; explicit practical default, explicitly not an official spec. | No authority; app-chosen practical default. | 2026-04-20 |
| `passport_square` | US Passport Digital Photo | 1200×1200 px, ≤ 512000 bytes (500 KiB) | https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos/digital-image-requirements.html | US State Dept digital photo square dimension range (600–1200 px); 1200×1200 chosen for quality within the cited range. | No passport-specific byte limit is cited by the source; the 500 KiB cap is an **app-chosen conservative default**, not an authority-published limit. | 2026-04-20 |

## Bytes versus KB/KiB

All limits are enforced in **exact bytes** (`Preset.maxBytes`). Display strings
show both bytes and KiB (bytes ÷ 1024). No preset relies on an ambiguous "KB"
display without an exact byte value behind it. Covered by
`PresetTest.government_id_form byte cap uses the stricter 240000-byte interpretation...`.

## Freshness / recheck policy

Per the status contract (90-day review threshold for external photo presets),
`Preset.needsRecheck(asOf)` flags any preset whose `lastVerified` is more than
90 days old. The UI renders "Source last verified … — recheck recommended" for
such presets. This threshold is a maintenance policy, **not** a guarantee that a
rule remains valid inside the window. As of 2026-09-29 all presets
(verified 2026-04-20) are **past the window and need recheck** — the flags are
expected to show.

## Non-guarantee disclaimer (rendered in-app)

"Meets size and dimension targets only — does not validate identity, pose,
background, or acceptance." (`edit_preset_disclaimer` in
`app/src/main/res/values/strings.xml`, rendered in the preset detail card.)
