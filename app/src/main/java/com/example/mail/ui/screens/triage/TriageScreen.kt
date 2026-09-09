package com.example.mail.ui.screens.triage

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mail.data.local.EmailMessage
import com.example.mail.ui.components.FloatingIsland
import com.example.mail.ui.components.FloatingIslandState
import com.example.mail.ui.components.OtpCard
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.StarkRed
import com.example.mail.ui.theme.SurfaceDark
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Swipe thresholds — minimum px drag to trigger an action.
// ---------------------------------------------------------------------------
private const val SWIPE_THRESHOLD = 120f

/**
 * Finite triage queue screen. Implements the card-deck swipe view for
 * "Inbox Zero" in < 2 minutes:
 *   swipe left  → delete (StarkRed feedback)
 *   swipe right → archive
 *   swipe up    → snooze
 *
 * Top: OTP widget. Middle: paginated triage deck. Bottom: FloatingIsland.
 */
@Composable
fun TriageScreen(
    viewModel: TriageViewModel = hiltViewModel()
) {
    val triageItems = viewModel.triageEmails.collectAsLazyPagingItems()
    val otpItems = viewModel.otpEmails.collectAsLazyPagingItems()

    // Cleanup expired OTPs on first composition.
    LaunchedEffect(Unit) {
        viewModel.cleanupExpiredOtps()
    }

    val clipboardManager = LocalClipboardManager.current
    val islandState = if (triageItems.itemCount > 0)
        FloatingIslandState.ThreadSelected else FloatingIslandState.Idle

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Header ──────────────────────────────────────────────────────
            TriageHeader(triageCount = triageItems.itemCount)

            // ── OTP Widget Section ──────────────────────────────────────────
            if (otpItems.itemCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val firstOtp = otpItems[0]
                    if (firstOtp != null) {
                        val remainingMs = firstOtp.expiresAt - System.currentTimeMillis()
                        val remainingSec = (remainingMs / 1000).coerceAtLeast(0)
                        OtpCard(
                            sender = firstOtp.sender,
                            otp = extractOtpCode(firstOtp.snippet),
                            expiresInSeconds = remainingSec,
                            tick = true,
                            onCopy = { code ->
                                clipboardManager.setText(AnnotatedString(code))
                            }
                        )
                    }
                }
            }

            // ── Triage Deck ─────────────────────────────────────────────────
            TriageDeck(
                emails = triageItems,
                onDelete = { viewModel.delete(it) },
                onArchive = { viewModel.archive(it) },
                onSnooze = { viewModel.snooze(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
            )
        }

        // ── Bottom Floating Island (overlaid) ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FloatingIsland(state = islandState)
        }
    }
}

// ===========================================================================
// Header
// ===========================================================================

@Composable
private fun TriageHeader(triageCount: Int) {
    val dateLabel = SimpleDateFormat("MMM d", Locale.US).format(Date())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TRIAGE",
            fontFamily = NDot,
            fontSize = 28.sp,
            color = PureWhite
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel.uppercase(),
                fontFamily = NDot,
                fontSize = 12.sp,
                color = MutedGray
            )
            Text(
                text = "$triageCount",
                fontFamily = NDot,
                fontSize = 14.sp,
                color = PureWhite
            )
        }
    }
}

// ===========================================================================
// Triage Deck — paginated list with directional swipe gestures
// ===========================================================================

@Composable
private fun TriageDeck(
    emails: androidx.paging.compose.LazyPagingItems<EmailMessage>,
    onDelete: (String) -> Unit,
    onArchive: (String) -> Unit,
    onSnooze: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = emails.itemCount,
            key = { emails[it]?.id ?: "item-$it" }
        ) { index ->
            val email = emails[index] ?: return@items
            SwipeableEmailCard(
                email = email,
                onDelete = { onDelete(email.id) },
                onArchive = { onArchive(email.id) },
                onSnooze = { onSnooze(email.id) }
            )
        }
    }
}

// ===========================================================================
// Single swipable email card
// ===========================================================================

@Composable
private fun SwipeableEmailCard(
    email: EmailMessage,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onSnooze: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }

    // Dominant swipe direction based on drag distance.
    val isHorizontalSwipe = kotlin.math.abs(offsetX) > kotlin.math.abs(offsetY)

    // Visual feedback: red tint on delete, green tint on archive.
    val backgroundColor = when {
        dismissed -> Color.Transparent
        isHorizontalSwipe && offsetX < -SWIPE_THRESHOLD / 2 ->
            StarkRed.copy(alpha = 0.3f * (kotlin.math.abs(offsetX) / 300f).coerceIn(0f, 1f))
        isHorizontalSwipe && offsetX > SWIPE_THRESHOLD / 2 ->
            Color(0xFF1B5E20).copy(alpha = 0.3f * (kotlin.math.abs(offsetX) / 300f).coerceIn(0f, 1f))
        !isHorizontalSwipe && offsetY < -SWIPE_THRESHOLD / 2 ->
            Color(0xFF0D47A1).copy(alpha = 0.3f * (kotlin.math.abs(offsetY) / 300f).coerceIn(0f, 1f))
        else -> SurfaceDark
    }

    // Label shown during swipe.
    val swipeLabel = when {
        dismissed -> ""
        isHorizontalSwipe && offsetX < -SWIPE_THRESHOLD / 2 -> "DELETE"
        isHorizontalSwipe && offsetX > SWIPE_THRESHOLD / 2 -> "ARCHIVE"
        !isHorizontalSwipe && offsetY < -SWIPE_THRESHOLD / 2 -> "SNOOZE"
        else -> ""
    }

    if (!dismissed) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    // Slight rotation on horizontal swipe.
                    rotationZ = if (isHorizontalSwipe) (offsetX / 50f).coerceIn(-8f, 8f) else 0f
                    // Scale down slightly on vertical swipe.
                    scaleX = if (!isHorizontalSwipe) 1f - (kotlin.math.abs(offsetY) / 1500f).coerceIn(0f, 0.1f) else 1f
                    scaleY = scaleX
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                isHorizontalSwipe && offsetX < -SWIPE_THRESHOLD -> {
                                    dismissed = true; onDelete()
                                }
                                isHorizontalSwipe && offsetX > SWIPE_THRESHOLD -> {
                                    dismissed = true; onArchive()
                                }
                                !isHorizontalSwipe && offsetY < -SWIPE_THRESHOLD -> {
                                    dismissed = true; onSnooze()
                                }
                                else -> {
                                    // Snap back.
                                    offsetX = 0f; offsetY = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f; offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Swipe feedback label.
                if (swipeLabel.isNotEmpty()) {
                    Text(
                        text = swipeLabel,
                        fontFamily = NDot,
                        fontSize = 10.sp,
                        color = when (swipeLabel) {
                            "DELETE" -> StarkRed
                            "ARCHIVE" -> Color(0xFF4CAF50)
                            else -> Color(0xFF42A5F5)
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.sender,
                        fontFamily = NDot,
                        fontSize = 13.sp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email.subject,
                        fontFamily = NDot,
                        fontSize = 15.sp,
                        color = PureWhite,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email.snippet,
                        fontSize = 13.sp,
                        color = MutedGray,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ===========================================================================
// Helpers
// ===========================================================================

/** Extract the first OTP code from an email snippet (4–8 digits, possibly spaced). */
private fun extractOtpCode(snippet: String): String {
    val regex = Regex("""\b(\d{4,8})\b""")
    return regex.find(snippet)?.value.orEmpty().let { raw ->
        // Format as spaced digits for display: "4 8 2 1"
        raw.chunked(1).joinToString(" ")
    }
}

// ===========================================================================
// Preview
// ===========================================================================

@Preview("Triage Screen", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TriageScreenPreview() {
    NothingTheme {
        // Static preview — shows header + empty deck. Full preview needs
        // Hilt + ViewModel; use Compose Preview instrumented test instead.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OLEDBlack)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TriageHeader(triageCount = 0)
            }
        }
    }
}
