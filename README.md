# LinkCleaner

Android share-target app that rewrites TikTok / Instagram / YouTube share URLs into clean, account-neutral canonical URLs before you forward them.

## Build

Prerequisites:
- JDK 21 (`sdk use java 21.0.10-oracle`)
- Android SDK platform 36 and platform-tools (installed via Android Studio or `sdkmanager`)
- Either `ANDROID_HOME` exported (e.g. `export ANDROID_HOME="$HOME/Library/Android/sdk"`) or a `local.properties` at repo root with `sdk.dir=/absolute/path/to/sdk`

Build a debug APK:

```bash
./gradlew assembleDebug
```

Output lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Covers URL extraction, each platform cleaner, the redirect resolver (via OkHttp MockWebServer), and the `cleanLink()` orchestrator against the spec §9 test table.

Test report: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Sideload to a physical device

1. Enable Developer Options: Settings → About phone → tap "Build number" 7 times.
2. Enable USB debugging: Settings → Developer options → USB debugging.
3. Plug in via USB, accept the RSA key prompt on the phone.
4. Verify the device is visible:

   ```bash
   adb devices
   ```

5. Install the APK:

   ```bash
   adb install --user 0 app/build/outputs/apk/debug/app-debug.apk
   ```

   The `--user 0` flag limits install to the primary user. Without it, Samsung devices (and any phone with a secondary profile / Secure Folder / Knox workspace) may install a duplicate clone alongside the main app.

## Using it

- **As a share target:** open TikTok / Instagram / YouTube → tap Share → pick LinkCleaner. It resolves/cleans the URL, copies the clean version to the clipboard, and offers a "Share clean link" button.
- **Manually:** launch the app; it prefills the field from your clipboard if there's a URL there.

