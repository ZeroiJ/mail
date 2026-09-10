# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Data layer (Room + Paging 3):**
  - `EmailMessage` Room entity matching the AGENTS.md schema (including `isOTP`, `expiresAt`, `bodyMarkdown`).
  - `EmailDao` with insert/update/delete CRUD and paged queries for the triage queue, all emails, and OTP cards.
  - `MailDatabase` singleton with DAO accessor.
  - `EmailRepository` split into a pure domain interface (`domain/repository/`) and a Room-backed `EmailRepositoryImpl` wiring Paging 3 `Pager`s.
- **Remote layer (Gmail REST API):**
  - `GmailApiService` Retrofit interface (`listMessages`, `getMessage`).
  - `AuthInterceptor` attaching an OAuth2 Bearer token to every request.
  - Gmail DTOs for message list/detail payloads.
  - `NetworkModule` (Hilt) providing `OkHttpClient` + Retrofit with Kotlinx Serialization.
- **Nothing OS design system (`ui/theme/`):**
  - Strict monochrome palette (`Color.kt`): OLED black, dark gray surfaces, muted secondary, stark red reserved for alerts/destructive/OTP.
  - Typography mapping (`Type.kt`): NDot (dot-matrix) for Display/Headline, Geist (sans-serif) for Title/Body/Label.
  - `NothingTheme` with a fixed dark scheme and dynamic color disabled.
  - Monochrome XML theme + font resource placeholders.
- **Tooling:**
  - `.github/workflows/release.yml`: build debug APK and publish a GitHub Release on `v*` tags.
- **Reusable UI components (`ui/components/`):**
  - `FloatingIsland`: translucent pill-shaped glassmorphic action island with idle and thread-selected states (Reply/Archive/Star/Delete).
  - `OtpCard`: dashed-border ephemeral OTP widget with dot-matrix code, StarkRed countdown, and "TAP TO COPY" pill.
  - `ActionCard`: boxy widget for extracted dates / tracking IDs / links.
  - `CHANGELOG.md` + git workflow conventions documented in `AGENTS.md`.

### Changed
- `EmailRepository` was a concrete data-layer class; refactored into `domain` interface + `data` implementation.

### Added
- Bundled font binaries: Geist Regular/Medium/Bold (OFL-1.1) and N-Dot 57 (OpenType) into `app/src/main/res/font/`, resolving `R.font.*` references in `ui/theme/Type.kt`.
- **Triage queue screen (`ui/screens/triage/`):**
  - `TriageViewModel`: `@HiltViewModel` injecting `EmailRepository`, exposing paged triage queue and OTP flows, with `delete()`, `archive()`, `snooze()` action handlers.
  - `TriageScreen`: Nothing-themed triage UI with N-Dot header, OTP widget section, paginated card-deck with directional swipe gestures (left→delete/red, right→archive, up→snooze), and anchored `FloatingIsland` overlay.
- **On-device AI integration (`util/`):**
  - `GeminiProcessor`: Google AI Edge / Android AICore wrapper with `generateThreadSummary()`, `extractActionableData()`, and `extractOtp()` — falls back to regex/heuristic when Gemini Nano is unavailable.
- **App entry & navigation:**
  - `MailApp`: `@HiltAndroidApp` Application class registered in `AndroidManifest.xml`.
  - `MainActivity`: `@AndroidEntryPoint` with Compose `NavHost`, start destination → `TriageScreen`.
  - Added Hilt + Navigation Compose dependencies to `build.gradle.kts`.
  - Added `AndroidManifest.xml` with INTERNET permission and activity declaration.
- **Data ingestion pipeline (`util/` + `data/repository/`):**
  - `TrackerStripper`: detects and neutralizes 1×1 transparent tracking pixels from email HTML using dimension, inline style, and known-domain heuristics.
  - `AutoBundler`: classifies emails into bundles (RECEIPT, NEWSLETTER, LOGISTICS, SOCIAL, OTP, PERSONAL) via sender domain and subject-line regex patterns.
  - `EmailRepositoryImpl.syncRecentEmails()`: full pipeline — fetches Gmail REST → extracts HTML → strips trackers → AI OTP detection → auto-bundles → upserts Room entities, all on `Dispatchers.IO`.
  - `TriageViewModel.syncRecentEmails()` + `isSyncing` StateFlow for pull-to-refresh loading state.
- **Google Sign-In & OAuth (`util/` + `data/remote/`):**
  - `AuthManager`: Credential Manager bottom-sheet account picker → `GoogleAuthUtil.getToken()` with `gmail.modify` scope → `EncryptedSharedPreferences` storage.
  - `AuthInterceptor` now reads tokens dynamically from `AuthManager` instead of a static lambda.
  - `NetworkModule` provides `AuthManager` → `AuthInterceptor` → `OkHttpClient` chain.
  - Added Credential Manager, Play Services Auth, and Security Crypto dependencies.
- **Security & Anti-Exploit Protocol (docs):**
  - `AGENTS.md`: new `## Security & Anti-Exploit Protocol` section — SQLCipher zero-trust storage with Keystore master keys, `FLAG_SECURE` + biometric gating, `android:exported="false"` component isolation, WebView/payload sandboxing with cleartext ban, R8 release hardening with Log stripping.
  - `STRUCTURE.md`: added `util/security/CryptoManager.kt`, `ui/components/BiometricGate.kt`, SQLCipher `SupportFactory` wiring in `di/DatabaseModule.kt`, and a 3-phase Implementation Strategy (Storage → UI Hardening → Network & Build).
- **Gradle build infrastructure:**
  - `settings.gradle.kts`, root `build.gradle.kts`, and `app/build.gradle.kts` split into a proper multi-module layout with AGP 8.5.0, Kotlin 2.0.20, Compose compiler plugin, Hilt 2.48, KSP with Room schema export.
  - `gradle.properties`, `local.properties`, `app/proguard-rules.pro`, adaptive launcher icons; `res/font/README.md` moved to `FONTS.md` so the resource merger accepts the font directory.
- **Authentication gate:**
  - `MainActivity` now sets `FLAG_SECURE` before `setContent` and gates the `NavHost` behind `AuthManager.authState` — unauthenticated users see `SignInScreen`.
  - `SignInScreen`: Nothing OS-styled sign-in (N-Dot header, monochrome palette, StarkRed error state).
  - `AuthManager`: real `WEB_CLIENT_ID`, Google ID tokens via Credential Manager with `googleid` artifact, failure diagnostics, and token-free `AuthState` (Bearer tokens never reach the UI layer).
- **Triage manual sync:**
  - `TriageViewModel.syncRecentEmails()` + `isSyncing` exposed to the UI; `TriageScreen` gained a manual sync trigger pill with spinner.
- **Data ingestion pipeline hardening (`data/repository/`):**
  - `GmailApiService` endpoints now take a `userId` path param (default `"me"`), keeping the URL scheme `users/{userId}/messages` and `users/{userId}/messages/{id}`.
  - `EmailRepositoryImpl.syncRecentEmails()` rewritten: runs strictly on `Dispatchers.IO`, fetches the latest 20 message IDs, fetches full payloads **concurrently** (`async`/`awaitAll`), strips trackers, generates on-device summaries + extracts OTPs via `GeminiProcessor`, classifies bundles via `AutoBundler`, then upserts via `insertAll()`.
  - Network calls wrapped in `runCatching`; per-message failures are logged and skipped without aborting the batch; the `TriageViewModel` `finally` block always clears the syncing indicator.
  - `EmailMessage` gained `summary` and `bundle_type` columns (Room migration v1→v2, exported schema); `insertEmails` renamed to `insertAll`.
- **Native background sync (WorkManager, replacing Cloud Pub/Sub):**
  - `worker/SyncWorker`: `@HiltWorker` `CoroutineWorker` pulling `emailRepository.syncRecentEmails()` inside a `runCatching` block — `Result.success()` on completion, `Result.retry()` on network failure, `CancellationException` rethrown.
  - `MailApp` implements `Configuration.Provider`, injects `HiltWorkerFactory`, and enqueues a unique `PeriodicWorkRequest` for `SyncWorker` every 15 minutes with a `NetworkType.CONNECTED` constraint (`ExistingPeriodicWorkPolicy.KEEP`), so polling starts quietly on first launch.
  - Default `WorkManagerInitializer` removed from the manifest so workers are built exclusively through Hilt.
  - Added `androidx.work:work-runtime-ktx` + `androidx.hilt:hilt-work` + `androidx.hilt:hilt-compiler` dependencies.
- **Paging 3 triage interface (Room → Compose):**
  - `EmailDao.getPagedEmails()`: `SELECT * FROM email_messages ORDER BY timestamp DESC` as a `PagingSource<Int, EmailMessage>`.
  - `EmailRepository.getPagedEmails()`: pages through the inbox with `PagingConfig(pageSize = 20)`; exposed via `TriageViewModel.emails` with `cachedIn(viewModelScope)`.
  - `TriageScreen` collects the deck with `collectAsLazyPagingItems()` into a `LazyColumn`; swipe gestures mapped: left→delete (StarkRed), right→archive (green), up→snooze (blue) — custom gesture modifiers since `SwipeToDismissBox` cannot express vertical swipes.
  - Rows render the Nothing OS compact aesthetic: N-Dot sender + subject, and the on-device AI `summary` (Geist, fallback to snippet) — no main-thread DB access (all Room ops suspend/off-main via paging).
  - Removed the bounded 24h triage-queue flow (`getTriageQueuePaged`/`getTriageQueueFlow`) — superseded by the full paged deck, which satisfies AGENTS.md's "finite daily inbox **or** card-deck swipe view".

## [0.0.0] - 2026-09-09
- Project scaffolded: AGENTS.md system prompt, STRUCTURE.md layout reference, and initial data/Room layer.
