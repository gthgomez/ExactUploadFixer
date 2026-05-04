# PROJECT_CONTEXT.md - ExactUploadFixer

## What This Is

Kotlin/Jetpack Compose Android utility app for resizing and compressing images to exact upload requirements.

Parent `Project_Android/PROJECT_CONTEXT.md` provides shared Android workspace rules. This file is the app-local agent-neutral context.

## Startup Sequence

1. Read `C:\Workspace\ENGINEERING.md`.
2. Read `C:\Workspace\AGENTS.md`.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md`.
4. Read this file.
5. Read `QA_CHECKLIST.md` for release/user-flow checks.

## Architecture & Invariants

- Monetization uses store flavors: `googlePlay` and `amazon`.
- Google Play uses Play Billing; Amazon uses RevenueCat/Amazon IAP through flavor-specific source sets.
- Image bytes must remain in state, not navigation routes.
- EXIF rotation must be applied before crop math.
- JPEG quality sweep lower bound must not be raised above the documented floor without explicit approval.
- Release signing uses environment variables only; never hardcode signing credentials.

## Verification & Commands

Run from `C:\Workspace\Project_Android\ExactUploadFixer`.

- Debug build: `.\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat :app:testDebugUnitTest`
- Google Play release bundle: `.\gradlew.bat :app:bundleGooglePlayRelease`
- Amazon release APK: `.\gradlew.bat :app:assembleAmazonRelease`

Release builds/signing and store submission require explicit user approval.

## Risk Zones

- Image compression/crop/EXIF logic.
- Billing flavor source sets and product IDs.
- Android manifest, FileProvider, signing config, and store release artifacts.
