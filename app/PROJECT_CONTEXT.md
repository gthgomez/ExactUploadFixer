# PROJECT_CONTEXT.md - ExactUploadFixer App Module

## What This Is

Android application module for ExactUploadFixer.

## Startup Sequence

1. Read `C:\Workspace\ENGINEERING.md`.
2. Read `C:\Workspace\AGENTS.md`.
3. Read `C:\Workspace\Project_Android\ExactUploadFixer\PROJECT_CONTEXT.md`.
4. Read this file.

## Architecture & Invariants

- Parent app context remains authoritative.
- Store flavors, signing, Play Billing, RevenueCat, and Amazon IAP are release-sensitive.
- Edit module implementation carefully and verify from the parent root.

## Verification & Commands

- From parent root: `.\gradlew.bat assembleDebug`
- From parent root: `.\gradlew.bat test`
- From parent root: `.\gradlew.bat connectedAndroidTest` when device/emulator verification is needed.
