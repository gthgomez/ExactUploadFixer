# Showcase evidence

Inspected on 2026-10-30 at [`b102ab367ce2f8686d6553d0686e53072d5ddd18`](https://github.com/gthgomez/ExactUploadFixer/tree/b102ab367ce2f8686d6553d0686e53072d5ddd18).

## Existing snapshots

The README embeds the original PNGs under `app/src/test/snapshots/` without
changing or re-recording golden pixels. Their source is
[`ExactUploadFixerScreenshotTest.kt`](../../app/src/test/java/com/exactuploadfixer/screenshot/ExactUploadFixerScreenshotTest.kt):
Robolectric API 35, `w411dp-h891dp-xxhdpi`, native graphics, and Roborazzi capture.
They exercise isolated Compose screens with injected UI state. The fit/padding
preview uses a generated solid-color image and the test's deterministic image
loader. No personal images are used.

The result-screen golden is intentionally omitted: it stages a `ProcessedImage`
with prefilled byte-count/dimensions, rather than running the compression engine,
and therefore cannot demonstrate a successful real image conversion. Likewise,
existing UI marketing text is not a preset acceptance guarantee.

[`provenance.json`](provenance.json) records the three embedded files and SHA-256
hashes. This pass visually inspected those files; it did not rerun Android tests.

## Reproduce UI snapshot verification

With the repo's JDK 17 / Android SDK 36 setup installed, run from the repo root:

```sh
./gradlew :app:testGooglePlayDebugUnitTest --tests 'com.exactuploadfixer.screenshot.*' -Proborazzi.test.verify=true
```

The convenience `verifyRoborazzi*` tasks are not registered in this repository.

## Runtime capture still needed

This cloud environment has JDK 21 but no Android SDK, `adb`, `emulator`,
`sdkmanager`, `/dev/kvm`, or attached Android device. No fresh debug build,
compression/export check, billing flow, or device video was executed. The
existing snapshots are evidence of checked-in UI states only.

Next setup: JDK 17, Android SDK platform 36, and Android 8.0+ test device/emulator.
Build/install the debug flavor, then use only a synthetic/public-safe image with
known dimensions and no personal metadata. Record the actual picker → constraints
→ crop/fit → processing → output details → export journey. Confirm output JPEG
pixel dimensions and exact byte count independently. Keep purchases/store flows
out of the demo. Preset selection never establishes a receiving authority's
acceptance of an image.

Capture device output using platform tools after opening the debug app:

```sh
adb exec-out screencap -p > docs/showcase/device-screen.png
adb shell screenrecord --time-limit 20 /sdcard/showcase.mp4
adb pull /sdcard/showcase.mp4 docs/showcase/demo.mp4
adb shell rm /sdcard/showcase.mp4
```

Record source commit, build flavor, device/OS, fixture provenance, and executed
steps beside future media. Inspect and compress clips before committing; retain
explicit labels separating snapshot tests from a device recording.
