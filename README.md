# ExactUploadFixer

Resizes and compresses JPEG images to meet explicit size and dimension targets you set (width, height, max bytes). Includes a free manual tier and Pro presets that encode sourced size constraints for specific upload routes.

**Presets are constraint sets, not eligibility certificates.** Meeting a preset's dimensions and byte cap does not validate identity, pose, background, image content, or final acceptance — every receiving authority reviews and decides. No guaranteed-acceptance claim is made or implied.

Each official preset cites its source (`authorityUrl`), the specific application context, the verification date (`lastVerified`), and how the authority's written byte limit was interpreted. Presets older than the 90-day recheck window are visibly flagged for recheck rather than presented as automatically current. Per-preset source/context/date/interpretation table: [docs/engineering/preset-sources.md](docs/engineering/preset-sources.md).

Byte limits are stated in exact bytes. Where an authority writes "kB" without defining it (e.g. the US State Dept "240 kB" rule), the stricter decimal reading (240,000 bytes) is applied so the output satisfies either interpretation; the cited interpretation is recorded per preset and covered by unit tests.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Google Play Billing (Amazon IAP for Amazon flavor), ExifInterface, SAF file export.

**Build (Google Play):** `.\gradlew.bat :app:assembleGooglePlayDebug`
**Build (Amazon):** `.\gradlew.bat :app:assembleAmazonDebug`
**Tests (Google Play):** `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
**Tests (Amazon):** `.\gradlew.bat :app:testAmazonDebugUnitTest`

**Detailed docs:** [CLAUDE.md](CLAUDE.md) | [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md) | [SHIP_CHECKLIST.md](SHIP_CHECKLIST.md)
