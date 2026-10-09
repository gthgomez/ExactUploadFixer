# Google Play Release Package — Exact Upload Fixer

**Status: repository-side release preparation.** Every owner-side action is listed
explicitly in "Owner blockers" — nothing here fabricates Console configuration,
contact details, or test results.

## 1. Build & version

- Version: `versionCode 3`, `versionName 1.1.0` (see `app/build.gradle.kts`)
- Flavor: `googlePlay` only for the Play artifact — `./gradlew :app:bundleGooglePlayRelease` → AAB
- Signing: upload key via `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` /
  `KEY_PASSWORD` environment variables. The build fails fast when they are missing
  (Amazon flavor) and produces an **unsigned** AAB otherwise — an unsigned artifact
  is NOT proof of a signed, Play-ready build.
- Target SDK 36 (meets current Play target-API policy), minSdk 26, R8 + resource
  shrinking enabled.

## 2. Verified in CI / locally (as of 2026-10-09)

- 300 JVM unit tests across both flavors, 0 failures
- Lint: 0 errors both flavors (CI fails on errors; warnings reviewed, not blocking)
- `assembleGooglePlayDebug`, `assembleAmazonDebug`, `assembleGooglePlayRelease`,
  `bundleGooglePlayRelease` all green (release artifacts unsigned without credentials)

## 3. Store listing draft

**Title** (30 chars max): `Exact Upload Fixer — KB & Size`

**Short description** (80 chars max):
```
Resize photos to an exact KB limit and dimensions. On-device, verified output.
```

**Full description:**
```
Some upload forms reject photos for one reason: the file is a few KB too big or
a few pixels too small. Exact Upload Fixer makes the file fit — fast, on-device,
with proof.

• Set a maximum file size in KB (1 KB = 1,000 bytes — stated plainly)
• Optionally set exact width × height: crop to fill, or fit the whole image
• One-tap presets with cited sources: US visa/DV digital image (240 kB), US
  passport online renewal, LinkedIn profile photo, email attachments
• Processing runs entirely on your device — your photo is never uploaded
• The result screen shows the measured output size, dimensions, and format, and
  verifies the limits you set were met
• Save anywhere or share straight to the form that rejected you

Presets encode technical file requirements only — they do not judge identity,
pose, or background, and acceptance is always decided by the receiving site.

Free tier: unlimited manual resizing. Pro (one-time): sourced presets.
```

**Feature graphic / icon / screenshots:** the icon source lives at
`icon_production_final.svg` (repo root). Screenshots must be captured from a real
device/emulator — **owed, blocked on emulator availability** (no KVM on the current
host). Roborazzi goldens cover logic states, not store-quality marketing shots.

## 4. Data safety form (preparation — owner must submit)

Answers implied by the code and PRIVACY.md (Google Play build):
- Does your app collect or share any of the required user data types? **No.**
  (Photo processing is on-device; no analytics; billing handled by Google Play.)
- Is all of the user data collected by your app encrypted in transit? N/A (nothing collected)
- Do you provide a way for users to request that their data is deleted? N/A (nothing collected)
- Privacy policy URL: required — see owner blockers.

## 5. Purchase testing (closed testing / internal)

1. Create the in-app product `exact_upload_fixer_pro` (one-time, lifetime) in
   Play Console → Monetize → Products, and a license-tester list.
2. Install the signed internal-testing AAB.
3. Verify: (a) paywall shows the **store-localized price**; (b) purchase completes
   and unlocks presets immediately; (c) kill + relaunch → entitlement restored via
   `queryPurchasesAsync` on resume; (d) purchase left PENDING (test card) → app
   stays on free tier and shows the pending explanation; (e) canceled sheet → no
   unlock, no error spam; (f) refunded/canceled purchase → unlock disappears on
   next resume.
4. Acknowledgment: purchases are acknowledged server-side-of-the-client
   immediately with retry-on-failure; verify no refund emails after 3 days for
   tester purchases.

## 6. Owner blockers (exact list)

1. **Signing credentials** — upload keystore is external by policy; without it no
   signed AAB can be produced or verified here.
2. **Privacy policy URL + support contact** — PRIVACY.md is repository-hosted with
   placeholder contact section; production URL and a real support email must be
   supplied by the owner before submission.
3. **Play Console configuration** — app entry, product `exact_upload_fixer_pro`
   with the final price (owner set USD 2.99 on Amazon; confirm Play price),
   closed testing track, and data-safety submission.
4. **Store screenshots / feature graphic** — need a device or emulator (no KVM here).
5. **On-device QA pass** — QA_CHECKLIST sweep incl. HEIC input decode on real
   hardware and Play purchase flow with license testers.
