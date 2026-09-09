# Nothing OS Design System & UI Architecture

## Global Aesthetic
*   **Color Palette:** Strict monochrome. Pure black backgrounds (optimized for OLED), white text, and dark gray element borders. 
*   **Accent Color:** Use Nothing's stark red strictly for alerts, destructive actions, and ephemeral countdown timers.
*   **Typography:** Use a dot-matrix font (e.g., N-Dot) for UI headers, sender names, section dividers, and big numbers. Use a clean sans-serif (e.g., Inter or Roboto) for email bodies, preview text, and small button labels.

## UI Components
*   **Contextual Action Floating Island:** Keep standard top/bottom app bars minimal[cite: 1]. Build a translucent, pill-shaped glassmorphic bar hovering at the bottom. Reveal reply, forward, star, and archive options only when selecting a thread or scrolling into a message[cite: 1].
*   **Dynamic Information Density:** Provide a toggle between "Compact" (one-line subject/sender previews for quick scanning) and "Digest" (two-line summary with sender avatars)[cite: 1].
*   **Widget Cards:** Format extracted action cards and OTPs to look like native Nothing home screen widgets with dashed borders and stark dot-matrix headers.
