# Nothing Mail

A triage-first email client for Android, built with Jetpack Compose and Material 3 in the Nothing OS monochrome aesthetic. Pulls from Gmail, processes everything on-device, and gets out of your way in under two minutes.

---

## Features

- **Card-deck triage** — Swipe left to delete, right to archive, up to snooze. Inbox Zero in a finite queue, never an infinite scroll.
- **Reader Mode** — Strips HTML tables, inline styles, and marketing cruft to clean plain text via `HtmlStripper`.
- **Ephemeral OTPs** — Auto-detected 4–8 digit codes rendered in a live countdown card, auto-deleted after 24 hours.
- **Action Cards** — Extracted dates, tracking IDs, and flight numbers pinned to the top of each thread.
- **On-device AI** — Gemini Nano (when available) for thread summaries and data extraction, falling back to regex/heuristics.
- **Server sync** — Two-way label mutations: archive, delete, and snooze flow from your phone to Gmail.
- **Tracker stripping** — Detects and neutralizes 1×1 transparent tracking pixels before they load.
- **Smart auto-bundling** — Groups receipts, logistics, newsletters, and OTPs locally by sender domain and subject patterns.
- **Background sync** — WorkManager polls the inbox every 15 minutes (no Cloud Pub/Sub required).

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose · Material 3 · Navigation Compose |
| Local DB | Room 2.6 · Paging 3 |
| Network | Retrofit 2.11 · OkHttp 4.12 · Kotlinx Serialization 1.6 |
| Auth | Google Credential Manager · Play Services Auth · OAuth2 Bearer |
| AI | Google AI Edge / LiteRT (Gemini Nano) · regex fallback |
| DI | Hilt 2.48 · KSP |
| Background | WorkManager 2.9 |
| Secure Storage | EncryptedSharedPreferences · Android Keystore |

Min SDK 26 · Target SDK 34 · Kotlin 2.0.20 · Compose BOM 2023.08

---

## Architecture

Layered Android architecture (data → domain → UI), enforced by AGENTS.md.

```
data/local/          Room entities, DAOs, MailDatabase
data/remote/         Gmail REST API (Retrofit), DTOs, AuthInterceptor
data/repository/     EmailRepositoryImpl — bridges local + remote
domain/repository/   EmailRepository interface (UI depends only on this)
ui/screens/          Feature screens: triage, reader
ui/components/       Reusable composables: OtpCard, ActionCard, FloatingIsland
ui/theme/            Nothing OS design system: Color.kt, Type.kt, NothingTheme
util/                Stateless helpers: TrackerStripper, AutoBundler, HtmlStripper, GeminiProcessor
di/                  Hilt modules: DatabaseModule, NetworkModule
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1) or later
- Android SDK 34
- JDK 17
- A Google Cloud project with Gmail API enabled

### Google Cloud Setup

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Gmail API** for the project.
3. Register your app as **Internal** or **Testing** under OAuth consent.
4. Add your Gmail address as a test user.
5. Create an OAuth 2.0 Client ID (Android app type) using your debug/release SHA-1 fingerprint.
6. Copy the client ID into the project (see `AuthManager` / `NetworkModule`).

### Build

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

---

## Design System

Strict monochrome, inspired by Nothing OS:

- **Background:** OLED black (`#000000`), dark surfaces (`#121212`, `#1E1E1E`)
- **Text:** pure white, muted gray for secondary
- **Accent:** stark red (`#D71921`) — alerts, destructive actions, OTP countdowns only
- **Typography:** N-Dot (dot-matrix) for headers, sender names, and big numbers; Geist (sans-serif) for body text
- **Components:** translucent pill-shaped Floating Island, dashed-border OTP/Action widgets, minimal pill buttons

---

## Project Structure

```
com.example.mail/
├── data/
│   ├── local/        EmailMessage, EmailDao, MailDatabase
│   ├── remote/       GmailApiService, AuthInterceptor, DTOs
│   └── repository/   EmailRepositoryImpl
├── domain/
│   └── repository/   EmailRepository (interface)
├── di/               DatabaseModule, NetworkModule
├── ui/
│   ├── theme/        Color, Type, NothingTheme
│   ├── components/   OtpCard, ActionCard, FloatingIsland, BiometricGate
│   └── screens/
│       ├── triage/   TriageScreen, TriageViewModel
│       └── reader/   ReaderScreen, ReaderViewModel
├── util/             TrackerStripper, AutoBundler, HtmlStripper, GeminiProcessor
└── worker/           SyncWorker (WorkManager)
```

See [STRUCTURE.md](STRUCTURE.md) for the full layout and placement rules.

---

## Security

Per AGENTS.md "Security & Anti-Exploit Protocol":

### Implemented
- **SQLCipher** database encryption — 256-bit AES master key held in the Android Keystore, injected into Room via `SupportFactory` (`util/security/CryptoManager`, `DatabaseModule`).
- **Biometric / device-credential gate** — `BiometricGate` re-locks on every resume; PIN/pattern/password fallback, never a silent bypass.
- **FLAG_SECURE** on `MainActivity` blocks OS background snapshots and screen recording.
- **R8 minification + resource shrinking** — release APKs are minified/obfuscated with all `android.util.Log` calls stripped via `-assumenosideeffects` (no PII leaks through logcat), with keep rules for Room/Hilt/SQLCipher.
- **Cleartext banned** — `network_security_config.xml` sets `cleartextTrafficPermitted="false"` platform-wide; the Gmail REST API is HTTPS-only.

### Planned
- **WebView hardening** — JavaScript disabled and local file/content access off, if a WebView is ever introduced (reader mode currently strips HTML to plain text instead).

---

## License

See [LICENSE](LICENSE) if present; otherwise all rights reserved.
