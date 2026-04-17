# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android share-target app (Kotlin + Jetpack Compose) that rewrites TikTok / Instagram / YouTube share URLs into clean, account-neutral canonical URLs. Single activity, single screen, no DB, no DI framework.

## Commands

Prerequisites: JDK 21 active (`sdk use java 21.0.10-oracle`), Android SDK platform 36 installed, and either `ANDROID_HOME` exported or `local.properties` with `sdk.dir=...`.

- Build debug APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Unit tests (plain JVM, no emulator): `./gradlew :app:testDebugUnitTest`
- Single test: `./gradlew :app:testDebugUnitTest --tests "com.kurounin.linkcleaner.logic.LinkCleanerTest.tiktok_canonical_strips_query"`
- Test report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Sideload: `adb install --user 0 app/build/outputs/apk/debug/app-debug.apk` (the `--user 0` keeps Samsung devices from cloning it into Secure Folder / secondary profiles)

Toolchain is pinned in `gradle/libs.versions.toml` (AGP 8.9.1, Kotlin 2.0.21, Compose BOM 2024.12.01, OkHttp 4.12.0, coroutines 1.9.0, compileSdk 36, minSdk 29).

## Architecture — the boundary that matters

The codebase splits into two zones with a hard import rule:

- **`com.kurounin.linkcleaner.logic.*` and `com.kurounin.linkcleaner.util.*`** — pure JVM. May import only `java.*`, `kotlin.*`, `kotlinx.coroutines.*`, `okhttp3.*`. **No `android.*` or `androidx.*`.** URL parsing uses `java.net.URI`, never `android.net.Uri`.
- **`com.kurounin.linkcleaner.ui.*` and `MainActivity`** — Android layer. Owns Compose, clipboard, share intents, `android.net.Uri` if needed.

**Why the rule exists:** `android.net.Uri` is stubbed on the JVM test classpath and throws `"not mocked"` at runtime. Keeping `logic/` stdlib-only is what lets `./gradlew :app:testDebugUnitTest` run on plain JVM without Robolectric or an emulator. Violating the rule silently breaks tests. The rule is enforced by code review, not tooling.

## Key seams

- `LinkCleaner.cleanLink(input)` is the orchestrator. It dispatches by host to per-platform cleaners (`TikTokCleaner`, `InstagramCleaner`, `YouTubeCleaner`) or falls through to tracking-param stripping for unknown hosts. Returns a `CleanResult` sealed hierarchy (`Success` / `Unchanged` / `Error`).
- `RedirectResolver` wraps OkHttp with manual redirect following (bounded hops, HEAD→GET fallback on 403/405, 5s call timeout). Injected into `TikTokCleaner` and `InstagramCleaner` so tests can substitute a stub or MockWebServer — see `LinkCleanerTest.Resolver` and `RedirectResolverTest`.
- `MainActivity` is the sole entry; the `ACTION_SEND` + `text/plain` intent filter registers the app as a share target. Auto-clean fires when launched via share; manual mode prefills from clipboard.
- Tracking-param allowlist (`TRACKING_PARAMS` in `LinkCleaner.kt`) applies only to the unknown-host path. Platform cleaners strip queries their own way per spec §5.

## Publishing

App package and `applicationId`: `com.kurounin.linkcleaner` (set in `app/build.gradle.kts`). Bump `versionCode` / `versionName` before each release.
