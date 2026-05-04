# Exact Upload Fixer Ship Checklist

This is the release gate for the current V1 app.

## 0. Release decision

- [ ] Google Play release build passes
- [x] Amazon release build passes and produces a signed APK
- [x] Amazon APK version is bumped for LAT/public draft uniqueness: `versionCode 2`, `versionName 1.0.1`
- [x] Amazon APK contains `assets/AppstoreAuthenticationKey.pem`
- [x] Amazon APK signature verifies with `apksigner verify --verbose`
- [x] Amazon APK zipalign verifies with `zipalign -c -p -v 4`
- [ ] Unit tests pass
- [ ] Instrumentation tests pass or are intentionally excluded with a reason
- [ ] Save and share flows are verified on-device
- [ ] Billing values are real, not placeholders

## 1. Core app correctness

- [ ] JPEG gate rejects PNG, WebP, HEIC, and renamed non-JPEG files
- [ ] Manual max-size flow works with no dimensions entered
- [ ] One-sided dimensions are rejected clearly
- [ ] Valid image always reaches Result screen or explicit failure
- [ ] Timeouts, decode failures, and compression failures show user-facing messages

## 2. Export and share

- [ ] Save writes the file to the selected location
- [ ] Save failure is reported if the destination cannot be opened
- [ ] Share uses a valid FileProvider URI
- [ ] Temp exports are cleaned up after save/share and on app start

## 3. Billing

- [ ] Google Play SKU is verified
- [x] Amazon RevenueCat public SDK key is set in `BillingConfig.kt`
- [x] Amazon Appstore shared secret is set in RevenueCat dashboard, not source code
- [x] Amazon entitlement ID is verified: `pro_access`
- [x] Amazon product ID is verified: `exact_upload_fixer_lifetime`
- [x] Amazon lifetime price is verified: USD 2.99
- [x] RevenueCat offering is configured as the source of truth for the paywall
- [ ] Paywall copy matches the actual plan model
- [x] Amazon Live App Test is submitted with tester group `Test`
- [ ] Amazon Live App Testing purchase grants `pro_access`

## 4. Store listing

- [ ] Description says "under X KB" or "max size limit"
- [ ] No claim of guaranteed exact byte size
- [x] Screenshots show the actual value proposition: fix upload rejections
- [x] Pricing is intentional for the lifetime purchase: USD 2.99
- [ ] Amazon IAP icon upload uses `store-assets/amazon-iap/exact-upload-fixer-pro-iap-512.png`
- [ ] Amazon IAP small icon upload uses `store-assets/amazon-iap/exact-upload-fixer-pro-iap-114.png`
- [x] Amazon listing/privacy copy does not claim "no internet" for the RevenueCat build
- [x] Amazon user data privacy is set to collect/transfer data for purchase entitlement checks
- [x] Amazon account creation is set to No

## 5. Manual QA

- [ ] Large JPEG on mid-range device
- [ ] EXIF-rotated JPEG
- [ ] Save to Downloads
- [ ] Share to Gmail
- [ ] Locked preset purchase flow
- [ ] Restart after purchase preserves entitlement
- [x] Amazon LAT test is initiated with the signed `versionCode 2` build
- [ ] Amazon LAT install works on target device using the signed `versionCode 2` build
- [ ] Amazon LAT purchase of `exact_upload_fixer_lifetime` unlocks Pro
- [ ] Free manual tier continues to work if billing/network is unavailable

## 6. Owner-side tasks

- [x] Provide the real Amazon RevenueCat public SDK key
- [x] Download Amazon `AppstoreAuthenticationKey.pem` and place it in `app/src/amazon/assets/`
- [x] Set Amazon Appstore shared secret in RevenueCat
- [x] Confirm the final lifetime price: USD 2.99
- [x] Provide release keystore env vars for signed Amazon build
- [ ] Certify Amazon export compliance on Review & submit
- [ ] Run the ship checklist on a physical device
- [ ] Approve the store listing copy
- [ ] Decide whether Amazon shipping is part of this release or deferred
