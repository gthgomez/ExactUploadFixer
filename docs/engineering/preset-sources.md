# ExactUploadFixer — preset sources and interpretations

Every official preset in `app/src/main/java/com/exactuploadfixer/domain/Preset.kt`
is a **size/dimension constraint set only**. It does not validate identity, pose,
background, image content, or acceptance. No preset guarantees acceptance by any
authority; final acceptance is always decided by the receiving site.

## Preset source table

| Preset id | Label | Constraints | authorityUrl | Application context | Byte-limit interpretation | lastVerified |
| --- | --- | --- | --- | --- | --- | --- |
| `job_portal_avatar` | LinkedIn Profile Photo | 400×400 px, ≤ 307200 bytes (300 KiB) | https://www.linkedin.com/help/linkedin/answer/1271074 | LinkedIn profile photo upload (LinkedIn Help, "Add & Edit Your Profile Photo"). 400×400 is the LinkedIn recommended minimum; LinkedIn publishes an **8 MB** profile-photo max. The 300 KiB byte cap is a **conservative app default, not a LinkedIn limit**. | LinkedIn Help specifies 8 MB for profile photos; app-chosen conservative cap. (CORRECTION 2026-10-09: an earlier draft cited 20 MB — that is the shared-image limit, not profile photos.) | 2026-10-09 |
| `government_id_form` | DV Lottery / visa digital image (US) | 600×600 px, ≤ **240000 bytes** | https://travel.state.gov/content/travel/en/us-visas/visa-information-resources/photos/digital-image-requirements.html | US State Dept visa/DV lottery digital image submission (square 600×600–1200×1200 px, JPEG, color). This route is NOT generalized to other government forms. | Authority page states "240 kB (kilobytes)" without defining kB as 1000 vs 1024 bytes. Applied as **max 240000 bytes** — the stricter decimal reading — so output satisfies either interpretation. Checked against the live page on 2026-10-09. | 2026-10-09 |
| `email_attachment` | Email Attachment | 1600×1200 px, ≤ 512000 bytes (500 KiB) | *(none — no universal standard exists)* | Generic email attachments; explicit practical default, explicitly not an official spec. | No authority; app-chosen practical default. | 2026-04-20 |
| `passport_online_renewal` | US Passport Online Renewal | 1200×1200 px, **55296–10000000 bytes** | https://travel.state.gov/content/travel/en/passports/have-passport/renew-online.html | **US passport ONLINE RENEWAL** photo upload. CORRECTION (2026-10-09 review): online renewal is NOT the visa spec — it publishes a bounded range of 54 KB–10 MB (600×600 px minimum square). Earlier drafts wrongly reused the visa "≤ 240 kB" cap and, before that, a 500 KiB app-chosen cap. The floor uses the higher KB reading (54 KiB = 55,296 bytes) and the ceiling the lower MB reading (10 MB decimal = 10,000,000 bytes), so an output satisfying this preset also satisfies either interpretation of both bounds. Checked 2026-10-09. | 2026-10-09 |

## Bytes versus KB/KiB

All limits are enforced in **exact bytes** (`Preset.maxBytes`, plus
`Preset.minBytes` where an authority publishes a floor). Display strings use
decimal KB. No preset relies on an ambiguous "KB"
display without an exact byte value behind it. Covered by
`PresetTest.government_id_form byte cap uses the stricter 240000-byte interpretation...`.

## Freshness / recheck policy

Per the status contract (90-day review threshold for external photo presets),
`Preset.needsRecheck(asOf)` flags any preset whose `lastVerified` is more than
90 days old. The UI renders "Source last verified … — recheck recommended" for
such presets. This threshold is a maintenance policy, **not** a guarantee that a
rule remains valid inside the window. As of 2026-10-09/10 the sourced presets
were re-verified against their live pages (LinkedIn Help, travel.state.gov visa
digital-image + DV pages, travel.state.gov passport online-renewal page). The
passport preset was rebuilt as `passport_online_renewal` around its actual
published bound (54 KB–10 MB) after two earlier drafts got it wrong (first a
500 KiB app cap, then a wrong reuse of the visa 240 kB cap). Lesson recorded:
a source URL must support the SPECIFIC route, units, and format claimed —
not just the general topic.

## Non-guarantee disclaimer (rendered in-app)

"Meets size and dimension targets only — does not validate identity, pose,
background, or acceptance." (`edit_preset_disclaimer` in
`app/src/main/res/values/strings.xml`, rendered in the preset detail card.)
