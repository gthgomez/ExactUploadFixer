# AGENTS.md — ExactUploadFixer (Gemini 3 Flash Override)

> Inherits from root [AGENTS.md](file:///C:/Workspace/Project_Android/AGENTS.md). General guidance is in [CLAUDE.md](./CLAUDE.md).

## Gemini-Specific Risks
- Hallucinated BitmapFactory decode options — always bounds-decode first (`inJustDecodeBounds=true`)
- Incorrect EXIF orientation math — swap width/height for 90°/270° rotated images BEFORE crop/resize
- JPEG quality floor drift — never raise the floor above 35 in the 100→35 sweep
- Hallucinated Play Billing method signatures or product ID constants

**Verification gate (flavored):**
- Google Play: `.\gradlew.bat :app:assembleGooglePlayDebug` / `.\gradlew.bat :app:testGooglePlayDebugUnitTest`
- Amazon: `.\gradlew.bat :app:assembleAmazonDebug` / `.\gradlew.bat :app:testAmazonDebugUnitTest`
