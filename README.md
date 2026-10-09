# ExactUploadFixer

Resizes and compresses photos (JPEG, PNG, or WebP input; JPEG output) to meet explicit size and dimension targets you set (width, height, max bytes; 1 KB = 1,000 bytes). Includes a free manual tier and Pro presets that encode sourced, dated size constraints for specific upload routes — file limits only, never acceptance guarantees. See PLAY_RELEASE.md for release status.

**Presets are constraint sets, not eligibility certificates.** Meeting a preset's dimensions and byte cap does not validate identity, pose, background, image content, or final acceptance — every receiving authority reviews and decides. No guaranteed-acceptance claim is made or implied.

Each official preset cites its source (`authorityUrl`), the specific application context, the verification date (`lastVerified`), and how the authority's written byte limit was interpreted. Presets older than the 90-day recheck window are visibly flagged for recheck rather than presented as automatically current. Per-preset source/context/date/interpretation table: [docs/engineering/preset-sources.md](docs/engineering/preset-sources.md).

Byte limits are stated in exact bytes. Where an authority writes "kB" without defining it (e.g. the US State Dept "240 kB" rule), the stricter decimal reading (240,000 bytes) is applied so the output satisfies either interpretation; the cited interpretation is recorded per preset and covered by unit tests.

> **Status: proprietary.** This repository is public for source visibility and
> transparency. It is **not open source** — there is no license grant to reuse,
> modify, or redistribute this code. See [LICENSE](LICENSE).

**Tech stack:** Kotlin, Jetpack Compose, Material3, Google Play Billing (Amazon IAP for Amazon flavor), ExifInterface, SAF file export.

**Build (Google Play):** `.\gradlew.bat :app:assembleGooglePlayDebug`
**Build (Amazon):** `.\gradlew.bat :app:assembleAmazonDebug`
**Tests (Google Play):** `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
**Tests (Amazon):** `.\gradlew.bat :app:testAmazonDebugUnitTest`

**Project docs:** [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md) | [SHIP_CHECKLIST.md](SHIP_CHECKLIST.md)

**Agent instructions:** [AGENTS.md](AGENTS.md); technical context in [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) (task data).
