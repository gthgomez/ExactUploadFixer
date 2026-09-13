# ExactUploadFixer

Resizes and compresses images to meet exact upload requirements (dimensions + file size). Supports a free manual tier and Pro presets for LinkedIn, State Department DV, and passport photo requirements.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Google Play Billing (Amazon IAP for Amazon flavor), ExifInterface, SAF file export.

**Build (Google Play):** `.\gradlew.bat :app:assembleGooglePlayDebug`
**Build (Amazon):** `.\gradlew.bat :app:assembleAmazonDebug`
**Tests (Google Play):** `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
**Tests (Amazon):** `.\gradlew.bat :app:testAmazonDebugUnitTest`

**Detailed docs:** [CLAUDE.md](CLAUDE.md) | [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | [STATUS.md](STATUS.md) | [PRIVACY.md](PRIVACY.md) | [QA_CHECKLIST.md](QA_CHECKLIST.md) | [SHIP_CHECKLIST.md](SHIP_CHECKLIST.md)
