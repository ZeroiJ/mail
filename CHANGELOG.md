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

## [0.0.0] - 2026-09-09
- Project scaffolded: AGENTS.md system prompt, STRUCTURE.md layout reference, and initial data/Room layer.
