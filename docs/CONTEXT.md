# Brushforge Android – Codex Context Bundle

## Project Snapshot
- Brushforge Android is a Compose/Hilt app that helps hobbyists manage miniature paint collections, mixes, and priming references across Converter, MyPaints, Palettes, Primed, and Profile tabs (`app/src/main/java/io/brushforge/brushforge_android_app/BrushforgeApp.kt:1`).
- Catalog holds ~4.3k paints seeded from bundled JSON assets; performance guidance stresses testing release builds on real hardware (`PERFORMANCE_GUIDE.md:1`).
- Core flows: load catalog, sync owned/wishlist state, allow custom paints/mixes, and surface palette/priming utilities.

## Modules & Architecture
- `:app` hosts nav graph + edge-to-edge `MainActivity` with Hilt entry point (`app/src/main/java/io/brushforge/brushforge_android_app/MainActivity.kt:1`).
- `:core:common` centralizes utility abstractions including injected coroutine dispatchers (`core/common/src/main/kotlin/io/brushforge/brushforge/core/common/CoroutineDispatchers.kt:1`).
- `:core:ui` supplies `BrushforgeTheme` and shared Material 3 components.
- `:domain` contains models, repositories, color analysis helpers, and use cases (`domain/src/main/kotlin/io/brushforge/brushforge/domain/usecase`).
- `:data` implements Room, DataStore, and catalog ingestion; Hilt modules wire repositories and migrations (`data/src/main/java/io/brushforge/brushforge/data/di/DatabaseModule.kt:1`).
- Feature modules (`:feature:converter`, `:feature:mypaints`, `:feature:palettes`, `:feature:primed`, `:feature:profile`) expose isolated Compose screens (example `feature/mypaints/src/main/java/io/brushforge/brushforge/feature/mypaints/MyPaintsScreen.kt:1`).

## Key Files
- `app/build.gradle.kts:1` — Compose-enabled Android app config, minSdk 26, target/compile 36, Hilt + Firebase wiring.
- `data/src/main/java/io/brushforge/brushforge/data/catalog/AssetCatalogProvider.kt:1` — Loads/decodes paint JSON assets on IO dispatcher.
- `data/src/main/java/io/brushforge/brushforge/data/database/BrushforgeDatabase.kt:1` — Room schema (catalog + user paints, migrations 1→3).
- `quick-test-instructions.md:1` — Fast release-build validation steps.

## Build & Test
- `./gradlew assembleDebug` for day-to-day development; `./gradlew assembleRelease` + `adb install -r app/build/outputs/apk/release/app-release-unsigned.apk` for performance checks.
- `./gradlew testDebugUnitTest` covers local JVM tests; `./gradlew connectedDebugAndroidTest` for instrumented suites.
- `./gradlew lint` or module-specific `lintDebug` keeps Compose/Android lint clean.
- Use `./gradlew :app:installDebug` for quick device installs when `adb` already connected.

## Conventions
- Compose UI with Material 3, state hoisted via `StateFlow` + `collectAsStateWithLifecycle`; previews live under each feature module.
- Hilt DI across modules; repositories exposed via interfaces in `:domain` and bound in `DatabaseModule`.
- Long-running work must switch threads using injected `CoroutineDispatchers`.
- Persisted user state flows through Room DAOs and `UserPreferencesRepositoryImpl` (DataStore).

## Docs & Assets
- `PERFORMANCE_GUIDE.md:1` and `quick-test-instructions.md:1` summarize expected latencies and profiling tips.
- Paint sources live in `app/src/main/assets/paints` (example `app/src/main/assets/paints/Citadel.json:1`); regenerate database by bumping migrations.
- iOS sister project sits in `TheBrushForgeIOS/` and can be ignored unless doing cross-platform coordination.

## Environment Notes
- Toolchain: Kotlin 2.0.21, AGP 8.13.0, Compose BOM 2024.10.0, coroutines 1.9 (`gradle/libs.versions.toml:1`).
- `google-services.json` is optional; without it Firebase is stubbed (`app/build.gradle.kts:9`).
- JVM target 17 across modules; Gradle daemon uses `-Xmx2048m` (`gradle.properties:1`).
- Ads/Billing/UMP libs included; mock/stub when running without production keys.

## Open Work
- Keep this section updated with active tasks, wip PRs, or blockers before starting a new session.
