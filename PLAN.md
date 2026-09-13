# Nothing Mail — Feature Roadmap

Living planning document. Check off items as they ship.

---

## Phase 1 — Core Email Operations (P0)

### Compose (v1 — text + CC/BCC + local drafts)
- [x] `POST /gmail/v1/users/me/messages/send` endpoint in `GmailApiService`
- [ ] `POST /gmail/v1/users/me/drafts` (create/update/delete) endpoints — declared, local-first for now
- [x] `Draft` Room entity (`id`, `to`, `cc`, `bcc`, `subject`, `body`, `serverDraftId`, `updatedAt`)
- [x] `DraftDao` with upsert/update/delete/getById/getAll
- [x] `ComposeScreen` — To/CC/BCC (collapsible) fields, subject, body, SEND + SAVE DRAFT
- [x] `compose` route + header compose button + FloatingIsland idle entry
- [x] MailDatabase v3→v4 migration (creates `drafts` table)
- [ ] Attachment picker (fast-follow slice)
- [ ] Rich text toolbar (bold, italic, link, list) (fast-follow slice)
- [ ] Signature prepend from settings (needs SettingsScreen first)
- [ ] Auto-save draft on back press (every 30s or on text change) (fast-follow slice)

### Reply / Reply All / Forward (v1)
- [x] Parse `Message-ID`, `References`, `To`, `Cc` headers into `EmailMessage` (DB v4→v5)
- [x] Quote extraction: sender/date + `> ` prefixed body; forward header block
- [x] REPLY / REPLY ALL / FORWARD pills in `ReaderScreen` → `compose?replyTo=&mode=` route
- [x] `ComposeViewModel.prepareReply`: To/Cc prefill, `Re:`/`Fwd:` subjects, `In-Reply-To` + `References` threading, self excluded from Reply All via AuthManager
- [ ] Forward attaches original email as `.eml` (fast-follow; inline block for now)

### Label Management (v1)
- [x] `GET /gmail/v1/users/me/labels` endpoint
- [x] `POST /gmail/v1/users/me/labels` (create) endpoint
- [x] `PATCH /gmail/v1/users/me/labels/{id}` (rename) + `DELETE` endpoints
- [x] `Label` Room entity + `EmailLabelCrossRef` many-to-many (DB v5→v6)
- [x] `LabelManagerScreen` — list/create/rename/delete (system labels read-only)
- [x] Reader `LabelSection` — applied chips + ADD picker dialog with inline create
- [x] `labels` route, wired via reader MANAGE link
- [ ] Label picker bottom sheet in compose/reply (fast-follow)
- [ ] Label filter chips in search (fast-follow)

### Conversation View
- [x] Room query: `GROUP BY threadId` with `MAX(timestamp)` ordering (correlated subquery — SQLite 3.18 on minSdk 26 has no window functions)
- [x] Add `isRead` field to `EmailMessage` entity (Room migration v6→v7 + `threadId` index)
- [x] Unread count badge per thread
- [x] Expand/collapse individual messages within a thread
- [x] Mark as read on open (local + one `threads.modify` server call)

### Search (basic v1 — full operator UI later)
- [x] `searchEmails(query)` in repository (network → upsert Room → return list)
- [x] `SearchViewModel` with 400ms debounce
- [x] `SearchScreen` — search bar, results list, empty state, clear button
- [x] `search` route in NavHost + magnifier button in triage header
- [ ] `SearchHistory` Room entity (`query`, `timestamp`, `id`)
- [ ] `SearchHistoryDao` with insert/getRecent(limit)/delete
- [ ] Operator chips (From, To, Subject, Has Attachment, Date)
- [ ] Recent searches list below search bar
- [ ] Search filters sheet (label, date range, attachment type)

### Multi-Account
- [ ] `GmailAccount` Room entity (`accountId`, `email`, `displayName`, `photoUrl`, `isActive`, `token`, `syncEnabled`)
- [ ] Refactor `AuthManager` to support multiple token sets (keyed by accountId)
- [ ] Refactor `EmailRepository` to accept account parameter
- [ ] `AccountSwitcherDialog` — profile picture dropdown
- [ ] `AccountSettingsScreen` — per-account inbox type, notifications, sync toggle
- [ ] `SyncWorker` handles multiple accounts sequentially

---

## Phase 2 — Notifications & Settings (P1)

### Per-Label Notifications
- [ ] `LabelNotificationSettings` entity (`labelId`, `isEnabled`, `sound`, `vibrate`, `priority`)
- [ ] Notification channels per label (Android 8+ requirement)
- [ ] `SyncWorker` checks notification settings before emitting
- [ ] Priority detection: keyword matching ("urgent", "offer", "contract") + ML fallback
- [ ] `NotificationSettingsScreen` — per-label toggles

### Swipe Customization
- [ ] `SwipeConfig` data class in DataStore (left/right/up action mapping)
- [ ] Actions enum: Archive, Delete, MarkRead, MoveToLabel, Snooze
- [ ] `SwipeActionsSettingsScreen` — action selectors per direction
- [ ] Wire config to `TriageDeck` swipe handlers

### Signature & Vacation Responder
- [ ] Store signature in DataStore (plain text)
- [ ] `SettingsScreen` — signature editor, vacation responder toggle + date range + message
- [ ] Vacation responder: check in `SyncWorker`, auto-reply via `POST /messages/send`
- [ ] `PUT /gmail/v1/users/me/settings/update` for vacation responder

### Default Reply Behavior
- [ ] DataStore preference: Reply vs Reply All
- [ ] Toggle in `SettingsScreen` under "General"
- [ ] Wire to compose screen's default `To`/`Cc` fields

### Smart Notifications
- [ ] `NotificationSettings` entity (`accountId`, `isEnabled`, `priorityOnly`, `sound`, `vibrate`)
- [ ] Per-account notification channels
- [ ] High-priority-only mode (keyword + ML classification)
- [ ] `NotificationSettingsScreen` — per-account toggles

---

## Phase 3 — Attachments & Integrations (P1)

### Attachment Download & Preview
- [ ] `GET /gmail/v1/users/me/messages/{id}/attachments/{attachmentId}` endpoint
- [ ] `Attachment` Room entity (`id`, `messageId`, `filename`, `mimeType`, `size`, `localPath`, `isDownloaded`)
- [ ] Wi-Fi-only download queue (respect user setting)
- [ ] `AttachmentPreviewScreen` — image preview, PDF viewer, file open intent
- [ ] Progress indicator during download

### Calendar Integration
- [ ] `CalendarEvent` Room entity (`messageId`, `title`, `startTime`, `endTime`, `location`, `rsvpStatus`)
- [ ] Event detection: regex + Gemini Nano for dates/times/locations
- [ ] `CalendarEventCard` pinned to thread top (like `ActionCard`)
- [ ] RSVP dialog (Yes/No/Maybe) via Google Calendar API
- [ ] "Add to Calendar" action button

### Contact Integration
- [ ] `Contact` Room entity (`email`, `displayName`, `photoUrl`, `isGoogleContact`)
- [ ] `ContactDao` with getByEmail/searchByName
- [ ] Google People API sync (`GET /v1/people/me/connections`)
- [ ] Contact autocomplete dropdown in compose To/CC/BCC
- [ ] Contact profile popup on sender tap
- [ ] Local photo caching (Coil or Glide)

---

## Phase 4 — Polish & AI (P2)

### Smart Compose
- [ ] Google AI API integration for text suggestions
- [ ] `SmartComposeOverlay` — grayed-out suggestion text
- [ ] Accept by tapping or swiping
- [ ] Personalization requires Gemini API key

### Nudges / Follow-Up Reminders
- [ ] `Nudge` Room entity (`emailId`, `type`, `timestamp`, `isDismissed`)
- [ ] Detect unanswered emails sent >24h ago
- [ ] Detect emails with "can you" / "when" / "please" patterns
- [ ] `NudgeBanner` at top of `TriageScreen`

### Undo Send
- [ ] `PendingSend` entity (`draftId`, `timestamp`, `cancelDeadline`)
- [ ] `UndoSnackbar` with countdown timer after send
- [ ] Cancel = move to Trash within deadline (5/10/20/30s configurable)

### Auto-Fit & Auto-Advance
- [ ] `AutoFitText` composable (scale text to fit container)
- [ ] Auto-advance to next message after archive/delete in `TriageViewModel`
- [ ] DataStore preference for auto-advance behavior

### Multiple Inbox Types
- [ ] DataStore preference: Default Inbox vs Priority Inbox
- [ ] Priority Inbox query: `is:important` filter
- [ ] `InboxTypeSelector` in account settings

### Recent Searches
- [ ] Wire `SearchHistory` entity to search screen
- [ ] Show recent queries as autocomplete suggestions
- [ ] Clear history option in settings

---

## Architecture Notes

### Room Migrations Needed
- **v3→v4**: Add `drafts` table (compose local drafts) — SHIPPED
- **v4→v5**: Add `rfcMessageId`, `headerReferences`, `toRecipients`, `ccRecipients` to `email_messages` (reply threading) — SHIPPED
- **v5→v6**: Add `Label`, `EmailLabelCrossRef` tables (label management) — SHIPPED
- **v6→v7**: Add `isRead` to `EmailMessage` + `threadId` index (conversation view) — SHIPPED
- **v7→v8**: Add `GmailAccount` table (multi-account)
- **v8→v9**: Add `Attachment` table (attachment management)

### New Gmail API Endpoints Required
| Endpoint | Phase | Purpose |
|----------|-------|---------|
| `POST /messages/send` | 1 | Send email / reply / forward |
| `POST /drafts` | 1 | Create draft |
| `PUT /drafts/{id}` | 1 | Update draft |
| `DELETE /drafts/{id}` | 1 | Delete draft |
| `POST /drafts/{id}/send` | 1 | Send from draft |
| `GET /labels` | 1 | List labels |
| `POST /labels` | 1 | Create label |
| `PUT /labels/{id}` | 1 | Modify label |
| `DELETE /labels/{id}` | 1 | Delete label |
| `POST /threads/{id}/modify` | 1 | Mark thread read / archive-delete conversation (`removeLabelIds`/`addLabelIds`) |
| `GET /messages/{id}/attachments/{attId}` | 3 | Download attachment |
| `PUT /settings/update` | 2 | Vacation responder |

### New Dependencies
- `androidx.datastore:datastore-preferences` — settings storage
- `androidx.work:work-runtime-ktx` — already present
- Coil or Glide — contact photo caching
- Google Calendar API — event detection + RSVP
- Google People API — contact sync

---

## Done

### v1.0.0 (shipped)
- [x] Room entity with 13 fields
- [x] Gmail REST API (listMessages, getMessage, modifyMessage)
- [x] Triage card-deck with swipe gestures
- [x] Reader mode with HTML stripping
- [x] OTP detection + ephemeral cards
- [x] Auto-bundling (receipts, newsletters, logistics)
- [x] Floating Island with blur effect
- [x] Biometric gate + FLAG_SECURE
- [x] WorkManager background sync (15min)
- [x] On-device Gemini summaries
- [x] Tracker stripping
- [x] SQLCipher encrypted storage
- [x] Google sign-in via Credential Manager

### Post-v1.0.0 Fixes
- [x] Base64 URL-safe decoding fix (email body garble)
- [x] FloatingIsland button onClick wiring
- [x] FloatingIsland blur removed (reverted to crisp translucent look)
- [x] Google sign-in main-thread fix
- [x] GET_ACCOUNTS + USE_CREDENTIALS permissions
- [x] UserRecoverableAuthException handling
- [x] Silent token refresh (no forced re-login)
- [x] BiometricGate removed (user request — direct to triage after auth)
- [x] Full inbox sync (was 24h window, now `in:inbox`)
- [x] HTML rendering in reader (sandboxed WebView, JS/file access off)
- [x] HTML mobile mode (viewport meta + responsive CSS injection, external links open in browser)
- [x] Snooze swipe removed (was stealing vertical scroll — deck now scrolls)
- [x] Batch loading (sync 20→50 per page + LOAD MORE button with pageToken)
- [x] OTP false positives fixed (keyword-context matching, stale flags self-heal)
