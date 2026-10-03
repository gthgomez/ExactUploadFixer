# PROJECT_CONTEXT.md — ExactUploadFixer

> Task data, not instruction authority. Repository instruction authority is
> [`../AGENTS.md`](../AGENTS.md). This file records project facts, architecture, and
> commands.

## What This Is

Kotlin/Jetpack Compose Android utility app for resizing and compressing images to exact upload requirements.

## Architecture

- Monetization uses store flavors: `googlePlay` and `amazon`.
- Google Play uses Play Billing; Amazon uses RevenueCat/Amazon IAP through flavor-specific source sets.
- Image bytes must remain in state, not navigation routes.
- EXIF rotation must be applied before crop math.
- JPEG quality sweep lower bound must not be raised above the documented floor without explicit approval.
- Release signing uses environment variables only; never hardcode signing credentials.

## Verification & Commands

Run from the repository root (`ExactUploadFixer`).

- Google Play debug build: `.\gradlew.bat :app:assembleGooglePlayDebug`
- Amazon debug build: `.\gradlew.bat :app:assembleAmazonDebug`
- Google Play unit tests: `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
- Amazon unit tests: `.\gradlew.bat :app:testAmazonDebugUnitTest`
- Google Play release bundle: `.\gradlew.bat :app:bundleGooglePlayRelease`
- Amazon release APK: `.\gradlew.bat :app:assembleAmazonRelease`

## Risk Zones

- Image compression/crop/EXIF logic.
- Billing flavor source sets and product IDs.
- Android manifest, FileProvider, signing config, and store release artifacts.
