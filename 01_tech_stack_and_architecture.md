# Tech Stack & Architecture

## Core Frameworks
*   **UI Framework:** Build exclusively with Jetpack Compose[cite: 1]. Utilize Material 3 for fluid transitions, swipeable cards, and dynamic theming[cite: 1].
*   **Local Storage & Offline-First Data:** Implement a Room Database combined with Paging 3[cite: 1]. Cache email bodies and headers locally so opening emails and searching happens with zero network latency[cite: 1].

## API & Backend Communication
*   **Protocol:** Utilize the Gmail REST API over IMAP[cite: 1]. The REST API provides granular label management, batch requests, and push notifications via Google Cloud Pub/Sub[cite: 1].
*   **OAuth Authentication Strategy:** Because this is a personal app, register it in the Google Cloud Console strictly under "Internal" or "Testing" status[cite: 1]. 
*   **Verification Bypass:** Add your own Gmail address as a test user to explicitly bypass Google's mandatory, expensive third-party security verification process for restricted Gmail scopes[cite: 1].
