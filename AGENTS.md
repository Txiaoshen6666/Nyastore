# AGENTS.md — GitStore (GitHub App Store)

Android app, single Gradle module `:app`. Kotlin 2.1.20 + Jetpack Compose (Material 3 Expressive). minSdk 33, compile/target 35.

## Build & verify

- **DO NOT run local builds** (e.g. `./gradlew assembleDebug`/`assembleRelease`) on this machine — rely on CI for build verification. Verify changes by code review instead of building locally.
- Requires **JDK 21** and Android SDK `platforms;android-35` + `build-tools;35.0.0`.
- There are **no automated tests** in this repo. Verification = a successful build (run by CI, not locally).
  - `./gradlew assembleRelease` (CI command; use `--no-daemon --stacktrace` like CI)
  - `./gradlew assembleDebug` for faster local builds
- Release APK (unsigned) lands at `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Release build runs **R8/ProGuard** (`proguard-rules.pro`); debug does not.

## Dependency management

- All versions live in `gradle/libs.versions.toml` and are referenced via `libs.*` / `libs.plugins.*` (version catalog + `pluginManagement`). Do not hardcode versions inline in `app/build.gradle.kts`.
- `dependencyResolutionManagement` is set to `FAIL_ON_PROJECT_REPOS`, so adding a repo must happen in `settings.gradle.kts`, not per-module.

## Gotchas

- **Room uses KSP, not kapt.** The compiler is wired via `ksp(libs.room.compiler)` and `room.generateKspApSchemas=true`. Do not switch to kapt or add another processor.
- `android.enableJetifier=true` is set in `gradle.properties` (legacy). Keep it unless migrating off; removing it can break AndroidX interop.
- In `app/build.gradle.kts`, the `targetSdk` line's `// 34` comment is **stale** — the real value (35) comes from `libs.versions.android.target.sdk`. Don't "fix" the comment by changing the code.
- minSdk is 33, so newer Android APIs (e.g. `PackageInfoFlags`, edge-to-edge, predictive back) need **no version guards** — don't add `Build.VERSION` checks for them.
- Single `Activity` (`MainActivity`); `GitHubAppStoreApp.kt` is the composition root / DI container holding the Room DB and repositories. Entrypoints are the app class, not a main().

## Notes

- README is in Chinese and is the best architecture overview; package layout mirrors the "目录结构" section.
- Network data is GitHub REST API (+ optional mirror proxy) and the F-Droid `index-v2.json`; app hosts no binaries.
