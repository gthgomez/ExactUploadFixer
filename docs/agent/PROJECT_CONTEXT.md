# PROJECT_CONTEXT.md - ExactUploadFixer

## What This Is

Kotlin/Jetpack Compose Android utility app for resizing and compressing images to exact upload requirements.

Parent `Project_Android/PROJECT_CONTEXT.md` provides shared Android workspace rules. This file is the app-local agent-neutral context.

## Startup Sequence

1. Read `AGENTS.md` in this directory — project-local agent guidance.
2. Read this file (`PROJECT_CONTEXT.md`) — directory map and invariants.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md` — workspace-wide context.
4. Read `C:\Workspace\Project_Android\CLAUDE.md` — behavioral rules and Android patterns.
5. Review `C:\Workspace\Project_Android\tasks\lessons.md` if it exists.
6. Read `QA_CHECKLIST.md` when release or workflow behavior is in scope.

## Architecture & Invariants

- Monetization uses store flavors: `googlePlay` and `amazon`.
- Google Play uses Play Billing; Amazon uses RevenueCat/Amazon IAP through flavor-specific source sets.
- Image bytes must remain in state, not navigation routes.
- EXIF rotation must be applied before crop math.
- JPEG quality sweep lower bound must not be raised above the documented floor without explicit approval.
- Release signing uses environment variables only; never hardcode signing credentials.

## Verification & Commands

Run from `C:\Workspace\Project_Android\ExactUploadFixer`.

- Google Play debug build: `.\gradlew.bat :app:assembleGooglePlayDebug`
- Amazon debug build: `.\gradlew.bat :app:assembleAmazonDebug`
- Google Play unit tests: `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
- Amazon unit tests: `.\gradlew.bat :app:testAmazonDebugUnitTest`
- Google Play release bundle: `.\gradlew.bat :app:bundleGooglePlayRelease`
- Amazon release APK: `.\gradlew.bat :app:assembleAmazonRelease`

Release builds/signing and store submission require explicit user approval.

## Risk Zones

- Image compression/crop/EXIF logic.
- Billing flavor source sets and product IDs.
- Android manifest, FileProvider, signing config, and store release artifacts.
