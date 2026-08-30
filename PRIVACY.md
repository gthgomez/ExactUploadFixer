# Privacy Policy — Exact Upload Fixer

**Last updated:** 2026-08-13
**Privacy URL in the app:** `https://github.com/gthgomez/ExactUploadFixer/blob/main/PRIVACY.md`
**Google Play requirement:** replace the repository-hosted URL with a production privacy-policy URL in Play Console before closed testing / production (Data safety form + store listing).

Exact Upload Fixer is published in two store flavors. What stays on your device—and what leaves it—depends on which build you install.

## Google Play build

- **Photo processing:** JPEG resize and compression run entirely on your device. Selected image bytes are not uploaded to our servers for processing.
- **Photo selection:** Uses the Android Photo Picker. The app receives only the image you choose; we do not scan your gallery.
- **Purchases:** Lifetime Pro access is sold through Google Play Billing. Google processes payment and purchase records according to [Google Play's policies](https://policies.google.com/privacy).
- **Network use:** The Google Play build does not require internet for core photo fixing. Internet is used only when you initiate a purchase or when Google Play services refresh purchase state.

## Amazon Appstore build

- **Photo processing:** Same on-device JPEG engine as the Google Play build. Your photos are not uploaded for resizing or compression.
- **Photo selection:** Same Photo Picker flow as the Google Play build.
- **Purchases & entitlements:** The Amazon build uses RevenueCat with Amazon In-App Purchasing. To verify Pro access, purchase and entitlement data **leave your device** and are processed by RevenueCat and Amazon according to their respective privacy policies.
- **Do not assume offline-only:** Unlike the Google Play build, the Amazon build may contact RevenueCat/Amazon servers to confirm entitlement—even when you are not actively purchasing.

## Data we do not collect (both builds)

- We do not operate a backend that stores your photos.
- We do not sell personal data.
- We do not use analytics SDKs in the current V1 codebase.

## Exported files

When you save or share a fixed JPEG, the file is written to a location you choose (Storage Access Framework) or shared via Android's share sheet. Those actions follow Android's normal app-to-app data flows.

## Contact

Replace this section with a support email or web form before public release.

## Store-specific summary (for listings)

| Topic | Google Play | Amazon |
|-------|-------------|--------|
| Photo bytes sent to developer servers | No | No |
| Purchase data processed by third parties | Google Play Billing | RevenueCat + Amazon IAP |
| Entitlement checks may use network | Via Google Play services | Yes — RevenueCat/Amazon |
| "Everything stays on device" | Accurate for photo processing | **Not accurate** — entitlement data leaves device |
