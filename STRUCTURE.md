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
│   │   └── (empty)             # FloatingIsland.kt, OtpCard.kt, ActionCard.kt
│   └── screens/            # Feature screens / navigation destinations
│       ├── triage/             # Card-deck triage queue
│       │   └── (empty)         # TriageScreen.kt, TriageViewModel.kt
│       └── (other screens)     # InboxList, EmailDetail, Settings
└── util/                   # Stateless helpers (regex parsers, tracker stripper)
    └── (empty)                 # AutoBundler.kt, TrackerStripper.kt
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

## Dependencies on this structure
- Fight the urge to put everything under `ui/` or one mega file.
- Keep `data/` free of Android UI types; keep `domain/` free of Room/Retrofit annotations.
- Paging wiring lives in `data/repository/`, not in ViewModels.
