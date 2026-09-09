# Database Schema & Local Caching Strategy

## Smart Auto-Bundles
*   **Local Grouping:** Automatically group receipts, logistics tracking, and newsletters into smart local collections without requiring manual Gmail server labels[cite: 1].
*   **Implementation:** Run a lightweight regex or string-matching parser on incoming email sender domains and subjects before saving them to the Room database. Tag the local database entry with a `bundle_type` (e.g., `RECEIPT`, `NEWSLETTER`).

## Room Database Structure (Conceptual)
*   **Entity `EmailMessage`:**
    *   `id` (String, Primary Key - matches Gmail Message ID)
    *   `threadId` (String)
    *   `sender` (String)
    *   `subject` (String)
    *   `snippet` (String)
    *   `bodyHtml` (String - original HTML)
    *   `bodyMarkdown` (String - parsed clean text for Reader Mode)
    *   `timestamp` (Long)
    *   `isOTP` (Boolean - flags for ephemeral widget rendering)
    *   `expiresAt` (Long - timestamp for auto-deletion)
