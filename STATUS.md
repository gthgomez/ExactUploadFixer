# ExactUploadFixer Status

**Last verified:** 2026-08-01
**Status:** usable
**Confidence:** high

## Purpose

Resizes and compresses images to meet exact upload requirements (dimensions + max file size) with a free manual tier and Pro tier presets (LinkedIn, State Dept DV, passport).

## Current State

The app is fully structured as a single-Activity Compose application with dual build flavors (Google Play Billing and Amazon RevenueCat IAP). Engine handles JPEG quality sweeping, EXIF rotation pre-processing, and file size targeted compression.

## Verified Capabilities

- Single-Activity Jetpack Compose architecture with manual ViewModel dependency wiring.
- Image compression and dimension adjustment engine with JPEG quality floor sweep (down to 35).
- Dual flavor support: Google Play Billing and Amazon RevenueCat integration.
- Storage Access Framework (SAF) export and FileProvider sharing.

## Recent Evidence

- `SHIP_CHECKLIST.md` records Amazon release build passing and signed APK generated (`versionCode 2`, `versionName 1.0.1`).
- `QA_CHECKLIST.md` specifies 11-point compression quality gate and JPEG-only mime gate.

## In Progress

- Amazon Live App Testing (LAT) on-device purchase verification.
- Final physical device QA sweep across mid-range and low-end target hardware.

## Blockers

- Requires physical device on-device QA execution and store certification before public store submission.

## Risks and Unknowns

- OEM-specific SAF document picker quirks on low-memory devices.
- Play Store vs Amazon Appstore entitlement sync if users cross platforms.

## Verification

- Google Play unit tests: `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
- Amazon unit tests: `.\gradlew.bat :app:testAmazonDebugUnitTest`
- Google Play debug build: `.\gradlew.bat :app:assembleGooglePlayDebug`
- Amazon debug build: `.\gradlew.bat :app:assembleAmazonDebug`
- Note: Unflavored `assembleDebug` / `testDebugUnitTest` are not valid for this dual-flavor project — use the store-specific tasks above.

## Next Actions

1. Run physical device manual QA pass using `QA_CHECKLIST.md`.
2. Verify Amazon Live App Testing (LAT) purchase unlocks Pro tier on real device.
3. Perform final Play Console / Amazon Appstore submission preflight.

## Evidence Sources

- [README.md](file:///C:/Workspace/Project_Android/ExactUploadFixer/README.md)
- [QA_CHECKLIST.md](file:///C:/Workspace/Project_Android/ExactUploadFixer/QA_CHECKLIST.md)
- [SHIP_CHECKLIST.md](file:///C:/Workspace/Project_Android/ExactUploadFixer/SHIP_CHECKLIST.md)
