# Project Structure

Layered Android architecture (data → domain → UI). Every layer is strictly separated;
follow this layout for all new files. Package root: `com.example.mail`.

## Source tree (as-built)

```
app/src/main/java/com/example/mail/
├── MailApp.kt                   # @HiltAndroidApp Application; HiltWorkerFactory provider
├── MainActivity.kt              # @AndroidEntryPoint; FLAG_SECURE, auth gate, NavHost
├── data/
│   ├── local/               # Room: entities, DAOs, database + migrations
│   │   ├── EmailMessage.kt      # Room Entity (matches AGENTS.md schema, v3 columns)
│   │   ├── EmailDao.kt          # CRUD + paged triage/OTP queries + lifecycle sweeps
│   │   └── MailDatabase.kt      # RoomDatabase (v3) with migrations
│   ├── remote/              # Gmail REST API: Retrofit service, DTOs, interceptors
│   │   ├── GmailApiService.kt   # listMessages, getMessage, modifyMessage
│   │   ├── AuthInterceptor.kt   # dynamic OAuth2 Bearer injection
│   │   └── dto/
│   │       └── GmailDtos.kt     # message list/detail + modify request DTOs
│   ├── repository/          # Single source of truth; bridges local + remote
│   │   └── EmailRepositoryImpl.kt  # Pager wiring, server sync, optimistic mutations
│   └── (data is referenced by domain, not UI directly)
├── domain/
│   ├── model/               # Pure domain models (no Room/Retrofit annotations)
│   │   └── (empty)              # e.g. Email; BundleType currently co-located in util/AutoBundler
│   └── repository/          # Domain-facing repository interfaces
│       └── EmailRepository.kt   # email flows the UI depends on (interface only)
├── di/                       # Hilt modules
│   ├── DatabaseModule.kt        # Room + SQLCipher SupportFactory + DAO providers
│   └── NetworkModule.kt         # AuthManager → AuthInterceptor → OkHttp → Retrofit
├── ui/
│   ├── theme/               # Compose Material 3 theme (Nothing OS monochrome)
│   │   ├── Color.kt             # OLED black, grays, StarkRed accent
│   │   ├── Type.kt              # NDot (dot-matrix) + Geist (sans-serif)
│   │   └── Theme.kt             # NothingTheme (fixed dark, dynamic color disabled)
│   ├── components/          # Reusable composables
│   │   ├── FloatingIsland.kt    # translucent pill-shaped action island
│   │   ├── OtpCard.kt           # dashed-border ephemeral OTP widget
│   │   ├── ActionCard.kt        # extracted date/tracking/link cards
│   │   └── BiometricGate.kt     # biometric/device-credential auth gate
│   └── screens/             # Feature screens / navigation destinations
│       ├── triage/              # Card-deck triage queue
│       │   ├── TriageScreen.kt      # swipe gestures: delete/archive/snooze
│       │   └── TriageViewModel.kt
│       ├── reader/              # Room-backed reader view
│       │   ├── ReaderScreen.kt
│       │   └── ReaderViewModel.kt
│       └── auth/                # Sign-in
│           └── SignInScreen.kt
├── util/                     # Stateless helpers (regex parsers, tracker stripper)
│   ├── AuthManager.kt           # Credential Manager + secure token storage
│   ├── AutoBundler.kt           # bundle classification; BundleType enum lives here
│   ├── HtmlStripper.kt          # Reader-mode HTML → plain text
│   ├── TrackerStripper.kt       # 1×1 tracking pixel neutralization
│   ├── GeminiProcessor.kt       # on-device summaries / extraction (LiteRT)
│   └── security/                # Keystore-backed cryptography
│       ├── CryptoManager.kt     # Keystore key + SQLCipher SupportFactory
│       └── (BiometricPrompt usage stays in ui/, never here)
└── worker/                   # WorkManager background work
    └── SyncWorker.kt            # @HiltWorker periodic sync + OTP/snooze sweeps
```

## Resources (as-built)

```
app/src/main/res/
├── drawable/
│   └── ic_logo.xml               # white-G adaptive icon foreground (512 viewport)
├── font/                         # geist_regular/medium/bold.ttf, ndot.otf
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml           # black background + @drawable/ic_logo
│   └── ic_launcher_round.xml
├── values/
│   ├── colors.xml                # ic_launcher_background (#000000), palette
│   ├── strings.xml
│   └── themes.xml                # Theme.AppCompat.NoActionBar parent
└── xml/
    └── network_security_config.xml  # cleartextTrafficPermitted="false"
```

## Project-level files
```
settings.gradle.kts              # module declaration
build.gradle.kts                 # root build script
app/build.gradle.kts             # app deps, release R8 + resource shrinking config
app/proguard-rules.pro           # Log stripping + Room/Hilt/SQLCipher keep rules
gradle.properties                # Gradle/JVM settings
app/schemas/                     # Room exported schemas (migrations)
AGENTS.md                        # System prompt: hard constraints to follow
STRUCTURE.md                     # this file: layout reference
FONTS.md                         # bundled font licensing
CHANGELOG.md                     # keep-a-changelog format
```

## Placement rules (enforced by AGENTS.md)
- **Room/DB code** → `data/local/`
- **Gmail REST API code** → `data/remote/`
- **Repository implementations** → `data/repository/`
- **Repository interfaces** → `domain/repository/`
- **Pure models** → `domain/model/`
- **Composables** → `ui/`
- **Stateless parsing/util (regex, tracker stripping)** → `util/`
- **Keystore-backed crypto (keys, AES/encryption ops)** → `util/security/`
- **Biometric/navigation gates (Compose wrappers)** → `ui/components/`
- **Background work (WorkManager workers)** → `worker/`

## Implementation status (security roadmap — all shipped)

### Phase 1 (Storage Encryption) — done
- Added `net.zetetic:android-database-sqlcipher:4.5.4` + `androidx.biometric:biometric` to `app/build.gradle.kts`.
- `util/security/CryptoManager.kt`: generates a hardware-backed AES-256 key in the Android Keystore (`AndroidKeyStore`, GCM). **Deliberately NOT biometric-bound** (`setUserAuthenticationRequired(false)`) so enrolling a new fingerprint never bricks the inbox; on DB open failure the corrupted file + sidecars are deleted and a fresh encrypted store is recreated.
- `di/DatabaseModule.kt`: Room `databaseBuilder()` passes the `SupportFactory` from `CryptoManager.getOrCreateSupportFactory(context)` — the database is encrypted from first creation; no plaintext migration path exists.

### Phase 2 (UI Hardening) — done
- `MainActivity.kt` sets `FLAG_SECURE` **before** `setContent(...)` — blocks OS background snapshots and screen recording.
- `ui/components/BiometricGate.kt`: Compose wrapper around `BiometricPrompt` wrapping the main `NavHost`, gated on `ON_RESUME` (cold launch **and** every resume-from-background re-locks the UI). Allows `DEVICE_CREDENTIAL` fallback (PIN/pattern/password) so a device without biometrics is never silently bypassed; `isPromptShowing` guard prevents double-`authenticate()` races.
- Gate sits ABOVE the NavHost in the composition tree — no email content renders before authentication succeeds.
- Requires `AppCompatActivity` (MainActivity) + `Theme.AppCompat.NoActionBar`.

### Phase 3 (Network & Build Hardening) — done
- `res/xml/network_security_config.xml` with `<base-config cleartextTrafficPermitted="false">`; referenced via `android:networkSecurityConfig` on the manifest application tag.
- `app/build.gradle.kts` release buildType: `isMinifyEnabled = true` **and** `isShrinkResources = true` (R8).
- `app/proguard-rules.pro`:
  - `-assumenosideeffects class android.util.Log { ... }` strips all log calls (verified: zero `android.util.Log` refs in release dex).
  - Keep rules: Room (`RoomDatabase` subclasses), Hilt (`@HiltViewModel` constructors + `dagger.hilt.**`), SQLCipher (`net.sqlcipher.**` / `net.zetetic.**` JNI-bound classes).
- Zero secrets compile into release binaries — OAuth client IDs / tokens resolve at runtime via `AuthManager` (EncryptedSharedPreferences + Keystore).

## Dependencies on this structure
- Fight the urge to put everything under `ui/` or one mega file.
- Keep `data/` free of Android UI types; keep `domain/` free of Room/Retrofit annotations.
- Paging wiring lives in `data/repository/`, not in ViewModels.
- **Security wiring convention:** cryptography lives in `util/security/`, database encryption wiring lives in `di/`, UI gating lives in `ui/components/`. Do not scatter Keystore or biometric calls across screens.
- Worker construction flows exclusively through Hilt (`MailApp` → `HiltWorkerFactory`) — never `WorkerFactory` manually.