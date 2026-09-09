package com.example.mail.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Nothing OS strict monochrome palette.
// Red is reserved EXCLUSIVELY for alerts, destructive actions, and ephemeral
// countdown timers (e.g. OTP expiry). Everything else stays black/white/gray.
// ---------------------------------------------------------------------------

// Pure OLED black for backgrounds.
val OLEDBlack = Color(0xFF000000)

// Elevated surfaces: cards, sheets, floating islands.
val SurfaceDark = Color(0xFF121212)
val SurfaceDarkElevated = Color(0xFF1E1E1E)

// 1dp borders & dividers.
val BorderGray = Color(0xFF2C2C2C)

// Text.
val PureWhite = Color(0xFFFFFFFF)
val MutedGray = Color(0xFF8E8E93)

// Nothing Stark Red — alerts, destructive actions, OTP countdowns only.
val StarkRed = Color(0xFFD71921)
val StarkRedAlt = Color(0xFFE53935)
