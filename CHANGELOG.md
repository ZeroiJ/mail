# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **App drawer (Wave 1 shell):** hamburger pill in the triage header opens a Nothing-styled drawer (black panel, N-Dot section dividers, inverted-pill selection). Sections: MAILBOX (Inbox), SMART BUNDLES (Purchases→receipt, Promotions→newsletter, Social, Updates→logistics — direct filter set, no more cycling), SYSTEM (Labels route). Header shows the signed-in account email. Version bumped to 1.2.3 (versionCode 6).

### Changed
- **Server drafts sync:** saving a draft now mirrors it to Gmail (`drafts.create` on first save, `drafts.update` after — with create-fallback if the server copy is gone). Deleting a draft removes the server copy too. The local save always succeeds; server failures degrade silently to offline mode. `saveDraft` returns the saved row with `serverDraftId` filled in so re-saves update instead of duplicating. Version bumped to 1.2.2 (versionCode 5).

### Changed
- **Reply/forward no longer prefills subject or body:** `prepareReply` now fills only recipients (To, plus Cc on Reply All) and the invisible `In-Reply-To`/`References` threading headers. Subject and body open blank; the quote/`Fwd:` block builders were removed. Version bumped to 1.2.1 (versionCode 4).

### Added
- **Conversation View:** inbox deck now groups emails by Gmail thread (`GROUP BY threadId` via correlated `MAX(timestamp)` subquery — no window functions on minSdk 26's SQLite 3.18). Each deck row is the newest message per thread with a per-thread **unread count badge** (red-bordered N-Dot pill, white sender for unread threads). Swipe actions operate on the whole thread (`threads.modify`: archive removes `INBOX`, delete adds `TRASH`). `EmailMessage` gained `isRead` (DB v6→v7 migration + `threadId` index) synced from the `UNREAD` label. Opening the reader marks the thread read locally + on the server (one `POST /threads/{id}/modify` call, skipped when already read). Reader shows a expandable `THREAD · N` section under the message body — tapping a sibling expands it and collapses the prior focused message; reply/labels target the expanded message. Version bumped to 1.2 (versionCode 3).

### Added
- **Label Management (v1):** `Label` entity + `EmailLabelCrossRef` many-to-many (DB v5→v6). `listLabels`/`createLabel`/`patchLabel`/`deleteLabel` endpoints with optimistic local apply + rollback on failure. `LabelManagerScreen` (`labels` route) for create/rename/delete with system labels read-only. Reader `LabelSection` shows applied chips (tap to remove) + ADD picker dialog with inline create and MANAGE link.

### Added
- **Reply / Reply All / Forward (v1):** `EmailMessage` stores `rfcMessageId`, `headerReferences`, `toRecipients`, `ccRecipients` (DB v4→v5). Reader has REPLY / REPLY ALL / FORWARD pills routing to `compose?replyTo=&mode=`; `ComposeViewModel.prepareReply` prefills recipients, `Re:`/`Fwd:` subjects, `> ` quoted body, and `In-Reply-To` + `References` threading headers. Reply All merges To/Cc minus your own address.

### Changed
- **Header search button removed:** search lives in the bottom dock now; header keeps compose + sync.

### Added
- **Compose (v1):** `repository.sendEmail()` builds an RFC822 plain-text payload (RFC2047-encoded subject) and posts via `messages.send`. `ComposeScreen` with To/CC (collapsible CC/BCC) / subject / body, SEND pill (disabled until To is filled, spinner while sending, error banner on failure) + SAVE DRAFT pill backed by a new `drafts` Room table (MailDatabase v3→v4). Entry via header compose button and FloatingIsland idle icon. Attachments, rich-text toolbar, auto-save, and signature deferred to fast-follow slices.

### Fixed
- **False OTP cards on every email:** `FallbackGenerator.extractOtp` matched the first bare 4–8 digit number in any body (years, order IDs, amounts). Now requires OTP keyword context (code, verification, passcode, 2fa, etc.) within 80 chars before the digits. Reader and triage widget both suppress the card when no contextual code is found — stale `isOTP` rows self-heal without a migration.

### Fixed
- **Deck scroll blocked by snooze gesture:** `detectDragGestures` consumed all drags including vertical, so the LazyColumn never scrolled. Replaced with `detectHorizontalDragGestures` and deleted the up-swipe snooze path entirely — swipe left/right still works, vertical drags now scroll.
- **Older emails unreachable:** sync capped at 20 messages with no pagination. Bumped batch to 50 and added `syncMoreEmails()` driven by Gmail `nextPageToken`, with a trailing LOAD MORE button in the deck (spinner while fetching, hidden when exhausted).

### Added
- **Email search (v1):** `repository.searchEmails(query)` hits Gmail network search, upserts results into Room, returns list. New `SearchScreen` with debounced (400ms) query input, results list, empty state, clear button. `search` route in NavHost + magnifier button in triage header. Operator chips and search history deferred to later.
- **HTML email rendering:** `ReaderScreen` now renders `bodyHtml` in a sandboxed WebView (JavaScript disabled, file/content access off per AGENTS.md payload sandboxing). Falls back to `bodyMarkdown` plain text when HTML is blank.

### Changed
- **App lock removed:** `BiometricGate` wrapper dropped from `MainActivity` — app opens directly to triage after auth per user request.
- **Full inbox sync:** query changed from `in:inbox newer_than:1d` to `in:inbox` — sync pulls all emails, not just last 24h.

## [1.1.0] - 2026-09-11

### Fixed
- **FloatingIsland blur removed:** RenderEffect backdrop blur was too aggressive even at 3f — reverted to the original crisp translucent gradient with border (the original defined look).
- **FloatingIsland actions not wired:** `TriageScreen` called `FloatingIsland(state = islandState)` with no callbacks — Reply/Archive/Delete had no implementation. Now wired to the first email in the triage queue: Reply opens reader, Archive/Delete trigger the existing repository actions.
- **Forced sign-in on every launch:** OAuth access tokens expire after 1 hour but `AuthManager` had no silent refresh — when the token lapsed, the `init` block set `TokenExpired` and the UI fell through to the sign-in screen. Now `init` optimistically sets `Authenticated` and fires `GoogleAuthUtil.getToken()` in a background thread; if the refresh succeeds the new token is stored and the user never sees the sign-in screen, if it fails the state flips to `SignedOut`.
- **FloatingIsland buttons unresponsive:** `FloatingIslandIconButton` rendered the icon but never wired `action.onClick` to a `clickable` modifier — the Reply/Archive/Star/Delete actions were purely decorative. Added `.clickable(onClick = action.onClick)` so taps actually fire.
- **Email body decoding garble:** `EmailRepositoryImpl.decodeBase64Url()` converted URL-safe base64 to standard but still used `Base64.DEFAULT`, which chokes on missing padding (Gmail omits `=`). Switched to `Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING` and removed the manual `-`→`+` / `_`→`/` rewrite — the Android decoder handles URL-safe directly.
- **FloatingIsland missing blur:** the "glassmorphic" pill used only semi-transparent alpha on a gradient background with no actual backdrop blur. Added `Modifier.graphicsLayer` with `RenderEffect.createBlurEffect(16f, 16f, CLAMP)` on API 31+ (Android 12+); pre-12 devices keep the translucent fallback since `RenderEffect` is unavailable.
- **Google sign-in silent failure:** `AuthManager.signIn()` wrapped `credentialManager.getCredential()` in `withContext(Dispatchers.IO)` — Credential Manager requires the main thread, so the bottom sheet never appeared. Removed the blanket IO dispatcher; only `GoogleAuthUtil.getToken()` now runs off-thread.
- **Missing `GET_ACCOUNTS` permission** required by `GoogleAuthUtil.getToken()` — added `GET_ACCOUNTS` and `USE_CREDENTIALS` to `AndroidManifest.xml`.
- **OAuth consent flow dead-end:** `UserRecoverableAuthException` (first-time `gmail.modify` consent) was caught by a blanket handler and the recovery intent was never launched. Now explicitly caught and `startActivity(e.intent)` is called so the user can approve the scope.

### Removed
- **GitHub Actions release workflow** (`.github/workflows/release.yml`) — it only ever produced a debug APK (`assembleDebug`) and had fragile publishing. Releases are now cut locally: a signed release APK is built by hand and published to the GitHub Release.

### Added
- **Release APK signing:** `app/build.gradle.kts` now defines a `release` signing config loaded from the gitignored `keystore.properties` at the repo root. The keystore itself lives outside the repo (`~/.android/keys/nothing-mail-release.jks`, 10,000-day RSA-2048 cert `CN=Nothing Mail`); on a fresh clone release builds stay unsigned, which is safe. `assembleRelease` now emits an installable, R8-minified `app-release.apk` (verified: `apksigner` accepts the cert, zero `android.util.Log` references survive in the dex).

## [1.0.0] - 2026-09-11

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
- **Two-way server sync (local gestures → Gmail):**
  - `GmailApiService.modifyMessage()`: `POST /gmail/v1/users/{userId}/messages/{id}/modify` with a new `ModifyMessageRequest` DTO (`addLabelIds` / `removeLabelIds`), enabling label mutations from the client.
  - `EmailRepository` replaced the local-only `deleteEmail(email)` with server-synced `archiveEmail(id)` / `deleteEmail(id)` / `snoozeEmail(id, untilTimestamp)`; each optimistically mutates Room **before** the network call and wraps it in `runCatching` on `Dispatchers.IO`, so the deck reacts instantly and failures degrade silently to offline mode.
  - Archive → remove `INBOX` label; Delete → add `TRASH` + remove `INBOX`; Snooze → remove `INBOX` on the server and persist `snoozedUntil` locally (Room migration v2→v3) so the row stays hidden from the deck until the timestamp passes.
  - Sync query narrowed to `in:inbox newer_than:1d` so archived/snoozed/trashed messages are not resurrected locally on the next poll.
- **Reader / detail view (Room-backed):**
  - `ReaderViewModel`: `@HiltViewModel` resolving the navigation argument `emailId` from `SavedStateHandle` and exposing the message as a reactive `StateFlow<EmailMessage?>` bound to Room — zero network latency on open.
  - `ReaderScreen`: Nothing-styled detail view (OLED black, N-Dot header with back affordance + sender/subject, dark gray divider) rendering the plain-text body; pins an `OtpCard` with a live countdown + copy when a message is flagged OTP, and an `ActionCard` per extracted DATE/TRACKING segment; shows a missing-state when the row no longer matches.
  - `HtmlStripper` (`util/`): converts raw email HTML to Reader-Mode plain text — strips nested `<table>` subtrees, `<style>/<script>/<head>` blocks, HTML comments and inline `style=` declarations via `HtmlCompat`, then normalizes whitespace/paragraph breaks.
  - Navigation: added `reader/{emailId}` route (`NavType.StringType`) to the `MainActivity` NavHost; triage deck rows are now tappable (`onEmailClick`) to open the reader, coexisting with the existing swipe gestures.
  - `EmailDao`/`EmailRepository`: replaced the suspend `getEmailById` with a `Flow<EmailMessage?>` reactive accessor so the reader re-emits when the row changes or is deleted.
- `README.md`: project overview, tech stack, architecture, design system, build instructions, and security roadmap.
- **Data lifecycle sweeps (background):**
  - `EmailDao.resetSnoozedEmails()`: `UPDATE email_messages SET snoozedUntil = 0 WHERE snoozedUntil > 0 AND snoozedUntil < :now` so snoozed emails resurface in the deck once their timer lapses.
  - `SyncWorker.doWork()` now runs `deleteExpiredOtps()` + `resetSnoozedEmails()` before `syncRecentEmails()`, expiring 24h OTPs and resurfacing expired snoozes on every poll without network.
  - `EmailRepository` exposes `resetSnoozedEmails()` alongside the existing `deleteExpiredOtps()`.
- **SQLCipher encrypted storage (AGENTS.md zero-trust):**
  - `util/security/CryptoManager`: 256-bit AES master key generated once in the Android Keystore (`AndroidKeyStore`, GCM, not biometric-bound so fingerprint changes never brick the inbox), returned as a `net.sqlcipher.database.SupportFactory`; on open failure the corrupted DB + sidecar files are deleted so a fresh encrypted store is recreated instead of crash-looping.
  - `DatabaseModule`: Room builder wired with `.openHelperFactory(CryptoManager.getOrCreateSupportFactory(context))`, so encryption applies from first creation; migrations preserved.
  - Added `net.zetetic:android-database-sqlcipher:4.5.4`.
- **Biometric content gate (AGENTS.md memory/screen isolation):**
  - `ui/components/BiometricGate`: prompts via `androidx.biometric:1.2.0-alpha05` on cold launch and every `ON_RESUME` (re-locking on background return), showing a pure-black N-Dot lock screen until success; allows `DEVICE_CREDENTIAL` fallback (PIN/pattern/password) so a device without biometrics is never silently bypassed; `isPromptShowing` guard prevents double-`authenticate()` races.
  - `MainActivity` switched from `ComponentActivity` to `AppCompatActivity` (BiometricPrompt requires a `FragmentActivity`) and wraps the post-auth NavHost in `BiometricGate` — email content composes only after biometric success.
  - `themes.xml` parent changed to `Theme.AppCompat.NoActionBar` (AppCompatActivity requirement); added `androidx.biometric` + `androidx.appcompat` dependencies.
- **Custom Gemini-style launcher icon (`res/`):**
  - `drawable/ic_logo.xml`: white capital "G" on a 512×512 viewport (scaled into the adaptive-icon safe zone), replacing the placeholder envelope glyph.
  - `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` now attach `@drawable/ic_logo` as the foreground over the pure OLED black background; the now-dead `ic_launcher_foreground.xml` was removed.
- **Network security hardening (AGENTS.md payload sandboxing):**
  - `res/xml/network_security_config.xml`: `<base-config cleartextTrafficPermitted="false">` bans cleartext HTTP platform-wide; wired into the manifest via `android:networkSecurityConfig`. The Gmail REST API only ever speaks HTTPS, so no legitimate traffic is affected.
- **Release build hardening (AGENTS.md build hardening):**
  - `app/build.gradle.kts`: the release build type now runs R8 (`isMinifyEnabled = true`) with resource shrinking (`isShrinkResources = true`).
  - `proguard-rules.pro`: `-assumenosideeffects` strips every `android.util.Log` call from the release binary (verified: zero `android.util.Log` references remain in the release dex), plus keep rules for Room (`RoomDatabase` subclasses), Hilt (`@HiltViewModel` constructors + `dagger.hilt.**`), and SQLCipher (`net.sqlcipher.**` / `net.zetetic.**` JNI-bound classes).
- **STRUCTURE.md** rewritten against the as-built tree: real file inventory for `data/remote/`, `domain/repository/`, `di/`, `ui/`, `util/`, and `worker/` (no more stale `(empty)` markers), resources listing, project-level files, and the three security phases marked **done** with as-implemented details.
- **Gradle wrapper committed:** `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` (Gradle 8.7) added so the `.github/workflows/release.yml` release pipeline can actually build in CI — previously it ran `chmod +x gradlew` but the wrapper was never committed.
- **Release pipeline fixes (`[1.0.0]`-blocking):** `gradle/wrapper/gradle-wrapper.properties` was being silently excluded by the `.gitignore` `*.properties` rule (only `!gradle.properties` was whitelisted) — now explicitly whitelisted so the wrapper has its distribution-URL file; the workflow now declares `permissions: contents: write` so the `GITHUB_TOKEN` can create the release (was failing with `403 Resource not accessible by integration`); `setup-java@v4` → `@v5` to clear the deprecation warning.

## [0.0.0] - 2026-09-09
- Project scaffolded: AGENTS.md system prompt, STRUCTURE.md layout reference, and initial data/Room layer.
