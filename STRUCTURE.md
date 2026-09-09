# Project Structure

Layered Android architecture (data → domain → UI). Every layer is strictly separated;
follow this layout for all new files. Package root: `com.example.mail`.

```
app/src/main/java/com/example/mail/
├── data/
│   ├── local/              # Room: entities, DAOs, database + local-only transforms
│   │   ├── EmailMessage.kt     # Room Entity (matches AGENTS.md schema)
│   │   ├── EmailDao.kt         # CRUD + paged triage/OTP queries
│   │   └── MailDatabase.kt     # Database class + singleton
│   ├── remote/             # Gmail REST API: Retrofit service, DTOs, interceptors
│   │   └── (empty)             # e.g. GmailApiService, AuthInterceptor, dto/
│   ├── repository/         # Single source of truth; bridges local + remote
│   │   └── EmailRepository.kt  # Pager wiring for triage/OTP flows
│   └── (data is referenced by domain, not UI directly)
├── domain/
│   ├── model/              # Pure domain models (no Room/Retrofit annotations)
│   │   └── (empty)             # e.g. Email, BundleType enum
│   └── repository/         # Domain-facing repository interfaces
│       └── (empty)             # e.g. EmailRepository
├── di/                     # Hilt modules
│   └── DatabaseModule.kt       # Room + DAO + repository provider
├── ui/
│   ├── theme/              # Compose Material 3 theme (Nothing OS monochrome)
│   │   └── (empty)             # Color.kt, Type.kt (N-Dot + Inter), Theme.kt
│   ├── components/         # Reusable composables
│   │   ├── (empty)             # FloatingIsland.kt, OtpCard.kt, ActionCard.kt
│   │   └── BiometricGate.kt    # Wraps Compose nav hierarchy with biometric auth gate
│   └── screens/            # Feature screens / navigation destinations
│       ├── triage/             # Card-deck triage queue
│       │   └── (empty)         # TriageScreen.kt, TriageViewModel.kt
│       └── (other screens)     # InboxList, EmailDetail, Settings
└── util/                   # Stateless helpers (regex parsers, tracker stripper)
    ├── (empty)                 # AutoBundler.kt, TrackerStripper.kt
    └── security/               # Keystore-backed cryptography
        └── CryptoManager.kt    # Android Keystore key generation + AES encryption
```

## Project-level files
```
build.gradle.kts                 # Root build script (deps declared here)
app/src/main/res/                # Android resources (colors, strings, themes)
AGENTS.md                        # System prompt: hard constraints to follow
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

## Implementation Strategy

### Phase 1 (Storage Encryption)
1. Add `net.zetetic:sqlcipher-android` and `androidx.biometric:biometric` to `build.gradle.kts`.
2. Build `util/security/CryptoManager.kt`: generates a hardware-backed AES key in the Android Keystore (with `setUserAuthenticationRequired(true)` for biometric-bound keys where supported), exposes `encrypt()` / `decrypt()` primitives.
3. Wire the SQLCipher `SupportFactory` into `di/DatabaseModule.kt`: the Room `databaseBuilder()` call must pass a `SupportFactory` initialized with the Keystore-derived master key (`CryptoManager.getOrCreateMasterKey()`). The database is encrypted from first creation — no plaintext migration path exists.

### Phase 2 (UI Hardening)
1. In `MainActivity.kt`, call `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` **before** `setContent(...)` to block OS background snapshots and screen recording.
2. Create `ui/components/BiometricGate.kt`: a Compose wrapper around `BiometricPrompt` that wraps the main `NavHost`. It requires fingerprint/face auth on first launch and on every resume-from-background (`onStart` / lifecycle `ON_START` gate). If no biometrics are enrolled, fall back to device credential (PIN/pattern/password) — never a silent bypass.
3. Keep the gate ABOVE the NavHost in the composition tree so no email content renders before authentication succeeds.

### Phase 3 (Network & Build Hardening)
1. Add `res/xml/network_security_config.xml` with `cleartextTrafficPermitted="false"` at the base config; reference it via `android:networkSecurityConfig` in `AndroidManifest.xml` application tag.
2. Enable `isMinifyEnabled = true` in the release build type with R8.
3. Add ProGuard rules (`proguard-rules.pro`): `-assumenosideeffects class android.util.Log { ... }` to strip all log calls, plus `-shrinkresources` for resource shrinking.
4. Ensure zero secrets compile into release binaries — all credentials (OAuth client IDs, API keys) resolve at runtime from `BuildConfig` alternatives or secure storage.

## Dependencies on this structure
- Fight the urge to put everything under `ui/` or one mega file.
- Keep `data/` free of Android UI types; keep `domain/` free of Room/Retrofit annotations.
- Paging wiring lives in `data/repository/`, not in ViewModels.
- **Security wiring convention:** cryptography lives in `util/security/`, database encryption wiring lives in `di/`, and UI gating lives in `ui/components/`. Do not scatter Keystore or biometric calls across screens.
