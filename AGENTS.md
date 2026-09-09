# Project AGENTS.md

## Core Technical Constraints
- **UI Framework:** Build exclusively with Jetpack Compose and Material 3.
- **Local Storage:** Implement Room Database with Paging 3 for offline-first data.
- **API Protocol:** Use Gmail REST API over IMAP for backend communication.
- **Authentication:** Register app as "Internal" or "Testing" in Google Cloud Console; add your Gmail as test user to bypass verification.

## Design System (Nothing OS Aesthetic)
- **Color Palette:** Strict monochrome: pure black backgrounds, white text, dark gray borders.
- **Accent Color:** Use stark red ONLY for alerts, destructive actions, and ephemeral countdown timers.
- **Typography:** Use dot-matrix font (e.g., N-Dot) for headers, sender names, section dividers, big numbers. Use clean sans-serif (e.g., Inter/Roboto) for email bodies, previews, small buttons.
- **UI Components:**
  - Build translucent, pill-shaped glassmorphic "Contextual Action Floating Island" at bottom for reply/forward/star/archive (show only on thread selection or scroll).
  - Provide toggle between "Compact" (one-line previews) and "Digest" (two-line summaries with avatars).
  - Format extracted action cards and OTPs as native Nothing home screen widgets with dashed borders and dot-matrix headers.

## Feature Logic & Workflows
- **High-Speed Triage:** Implement finite daily inbox or card-deck swipe view for "Inbox Zero" in <2 minutes.
- **Gesture Mapping:** Swipe left=delete, right=archive, up=snooze.
- **Reader Mode:** Automatically strip HTML tables, inline styles, marketing headers; convert to clean Markdown-style plain text.
- **Privacy:** Detect and neutralize 1x1 transparent tracking pixels before image loading.
- **Ephemeral OTPs:** Auto-detect 4-8 digit codes; display in glanceable card; auto-delete email after 24 hours.
- **VIP Notifications:** Trigger push notifications ONLY for starred contacts or urgent keywords (e.g., "urgent", "offer", "contract").
- **On-Device TL;DR:** Use local processing (e.g., Gemini Nano) to generate 2-sentence bullet summaries for long threads.
- **Action Cards:** Parse flight numbers, meeting invites, tracking IDs, calendar dates into interactive widgets pinned to thread top.

## Database Schema & Caching
- **Smart Auto-Bundles:** Group receipts, logistics, newsletters locally using regex/string-matching on sender domains and subjects; tag with `bundle_type` (e.g., `RECEIPT`, `NEWSLETTER`).
- **Room Entity `EmailMessage`:**
  - `id` (String, Primary Key - Gmail Message ID)
  - `threadId` (String)
  - `sender` (String)
  - `subject` (String)
  - `snippet` (String)
  - `bodyHtml` (String - original HTML)
  - `bodyMarkdown` (String - parsed clean text for Reader Mode)
  - `timestamp` (Long)
  - `isOTP` (Boolean - flags for ephemeral widget rendering)
  - `expiresAt` (Long - timestamp for auto-deletion)

## Project Structure
- **Layering:** Strictly separate `data/` (Room + Gmail REST), `domain/` (pure models + interfaces), `ui/` (Compose), `util/` (stateless helpers). Package root: `com.example.mail`.
- **Placement:** Room/DB → `data/local/`; Gmail REST → `data/remote/`; repository impls → `data/repository/`; repository interfaces + pure models → `domain/`; composables → `ui/`; regex/tracker parsing → `util/`.
- **Rule:** Full layout reference lives in `STRUCTURE.md`. Consult it before placing any new file.

## Security & Anti-Exploit Protocol
- **Zero-Trust Storage:** All Room databases MUST be encrypted using SQLCipher (`net.zetetic:sqlcipher-android`). The master key MUST be generated and stored EXCLUSIVELY in the Android Keystore, never in code, SharedPreferences, or plain files. `di/DatabaseModule.kt` must inject the SQLCipher `SupportFactory` into the Room database builder so encryption applies from first creation.
- **Memory/Screen Isolation:** `MainActivity` MUST set `WindowManager.LayoutParams.FLAG_SECURE` before `setContent` to block OS background snapshots and screen recording. App launch and every resume-from-background state MUST be gated by a `BiometricPrompt` before any email content is rendered.
- **Component Isolation:** Every component declared in `AndroidManifest.xml` (Activity, Service, Receiver, Provider) MUST enforce `android:exported="false"` unless it is the launcher activity. No component may be exported for implicit external access.
- **Payload Sandboxing:** Any `WebView` rendering raw email HTML MUST explicitly disable JavaScript, local file access, and content access. A `res/xml/network_security_config.xml` MUST ban cleartext traffic (`cleartextTrafficPermitted="false"`), referenced from the application-level `android:networkSecurityConfig`.
- **Build Hardening:** Release builds MUST enable R8 minification/obfuscation (`isMinifyEnabled = true`) and ship ProGuard rules that strip all `android.util.Log` calls (`-assumenosideeffects class android.util.Log { ... }`) plus shrink resources. Zero secrets (client IDs, API keys, tokens) may be compiled into release binaries — all credentials resolve at runtime.
- **Biometric Fallback:** If the device has no biometric hardware enrolled, fall back to device credential (PIN/pattern/password) authentication — never a silent bypass.

## Git Workflow (Commit & Push Discipline)
- **Remote:** Push to `origin` / branch `main` (https://github.com/ZeroiJ/mail.git).
- **Commit frequency:** Commit and push in small, well-scoped increments — one logical unit per commit. Do not batch unrelated work into a single commit.
- **Message quality:** Write clean, self-explanatory commit messages that a stranger to the codebase could read and understand exactly what changed and why. Use imperative mood, an optional short scope prefix, and a body for non-obvious decisions.
  - Format: `type(scope): short summary` (e.g. `feat(data): add EmailMessage Room entity`, `refactor(repo): route paging through EmailRepository`). Choose `feat` / `fix` / `refactor` / `chore` / `docs` / `test` as appropriate.
  - When a commit is logically non-trivial, add a short body (blank line, then bullet points) explaining intent — not a recap of the code.
- **Avoid:** placeholder messages ("wip", "stuff", "changes"), highly generic messages, or dumping unrelated formatting changes into feature commits.
- **Changelog:** Keep `CHANGELOG.md` up to date under `## [Unreleased]`. Add a bullet for every user-visible or structural change, grouped under `Added` / `Changed` / `Fixed` / `Removed`. On a version release, move the entries into a dated `## [x.y.z]` section and tag it. Never let a working turn end with code changes but no changelog entry.
- Pick the right moment to leave the repo in a clean, pushable state at the end of each working turn (full commit + push), but still split the intermediate steps into clean atomic commits rather than one giant push.

## Project Workflow Rules
- All UI must follow Nothing OS design system.
- All email data must be cached locally in Room for offline-first access.
- All API calls must use Gmail REST API; no IMAP direct connections.
- All authentication must use internal/testing status with bypass.
- All triage must be finite and gesture-based.
- All newsletters must have reader mode and tracker stripping.
- All OTPs must be ephemeral with auto-delete.
- All notifications must be VIP-only or keyword-based.
- All thread summaries must be on-device generated.
- All actionable data must be extracted into widget cards.
