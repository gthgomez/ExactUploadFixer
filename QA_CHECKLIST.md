# Exact Upload Fixer — Pre-Launch QA Checklist

Source: Gemini Phase 4 (most thorough) + ChatGPT risk audit cross-referenced.
Run every item on a real mid-range device before hitting Publish.

---

## 1. Engine Core — Compression Guarantees

These protect against 1-star reviews.

- [ ] **Normal JPEG (4 MB camera photo)** — output ≤ maxBytes and visually sharp
- [ ] **Already-small JPEG (< target)** — no unnecessary quality loss
- [ ] **Large 12–48 MP JPEG** — processes in < 5 s, no OOM, no ANR
- [ ] **Rotated/sideways JPEG (EXIF 90°)** — final orientation is upright and correct
- [ ] **Mirrored JPEG (EXIF flip)** — orientation still correct
- [ ] **Impossible target (e.g. 3000×3000 @ 20 KB)** — explicit failure message, never blurry export
- [ ] **Max-size only (no dimensions)** — compress works, no crash
- [ ] **Dimensions + max-size** — center-crop + compress produces exactly targetW × targetH
- [ ] **Quality floor hit at 35** — fileSizeBytes is still ≤ maxBytes, or explicit failure
- [ ] **Cancel during processing (app backgrounded)** — no crash on return
- [ ] **Repeat use (same photo 5×)** — no memory leak, cache stays clean

**Pass criteria: 11/11 before publishing.**

---

## 2. JPEG-Only Gate (ChatGPT Risk #2)

- [ ] Select a PNG → inline error "JPEG files only", stays on Pick screen
- [ ] Select a WebP → same
- [ ] Select a HEIC → same
- [ ] Select a JPEG → proceeds to Edit normally
- [ ] MIME check works (use a file renamed with .jpg extension but actually PNG)

---

## 3. UI and Free Flow

- [ ] Pick → manual max-size → Fix Upload → Result screen appears
- [ ] All number fields reject non-numeric input without crashing
- [ ] Width filled, height empty → "Enter both or neither" error, Fix button blocked
- [ ] Max size empty → "Enter a valid max file size" error
- [ ] Negative or zero values → error, no crash
- [ ] Fix button disabled during processing
- [ ] Processing spinner visible

---

## 4. Paid Flow and Paywall

- [ ] Tapping locked preset → billing dialog appears
- [ ] After test purchase (Play Console test account) → presets unlock immediately, no restart needed
- [ ] Preset tap → correct dimensions/size applied to input fields
- [ ] Preset tap → processed output matches preset dimensions exactly
- [ ] Billing offline (Airplane mode) → free manual tier still works
- [ ] isProUnlocked persists after app restart (queryPurchasesAsync on init)

---

## 5. Export and Share

- [ ] Save → file appears in user-chosen location with correct size
- [ ] Share → share sheet opens, recipient sees correct JPEG
- [ ] Share to Gmail → attachment loads correctly (FileProvider URI + MIME type)
- [ ] Cancel save → app returns cleanly, image is still in memory for retry
- [ ] Repeat export (save then share) → works twice without crash
- [ ] Cache is cleaned after save (no temp file accumulation)
- [ ] Low storage scenario → clear error message, no crash

---

## 6. Permissions and Platform Compliance

- [ ] **Manifest audit**: INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE are absent
- [ ] Android 14 device → Photo Picker works, no permission dialog
- [ ] Android 15 device → Photo Picker works, no permission dialog
- [ ] Low-end device (2 GB RAM) → large image test passes
- [ ] Dark mode → UI renders correctly
- [ ] Landscape orientation → no layout overflow or crash
- [ ] App killed mid-processing → returns to Pick screen cleanly on relaunch

---

## 7. Store Listing — False Advertising Check

- [ ] Description never claims "exact KB" or "exact byte size"
- [ ] Description says "max size limit" or "under X KB"
- [ ] Screenshots show "Upload Ready ✓" not "Photo Editor"
- [ ] Icon has no camera, paintbrush, or gallery imagery
- [ ] Privacy policy is store-specific: Google/offline claims are not reused for the Amazon RevenueCat build
- [ ] No analytics SDK in the APK (confirm with APK Analyzer)

---

## 8. Go / No-Go Gate

**Ship when all boxes are checked.**

If CompresssionCouldNotMeetMaxSize fires on a common real-world case
(e.g. a 3 MB photo to 300 KB), this is expected behavior — the failure
message must be clear, not a crash or a silent blurry export.

The one sentence that governs every decision:
> "If this does not directly help one rejected upload pass right now, cut it."
