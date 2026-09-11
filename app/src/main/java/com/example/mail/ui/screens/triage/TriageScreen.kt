package com.example.mail.ui.screens.triage

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.StarkRed
import com.example.mail.ui.theme.SurfaceDark
import com.example.mail.util.FallbackGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Swipe thresholds — minimum px horizontal drag to trigger an action.
// Only horizontal swipes are handled; vertical drags pass through to the
// LazyColumn so the deck scrolls. (Up-swipe snooze was removed — it stole
// vertical scroll gestures and made older emails unreachable.)
// ---------------------------------------------------------------------------
private const val SWIPE_THRESHOLD = 120f

/**
 * Finite triage queue screen. Implements the card-deck swipe view for
 * "Inbox Zero" in < 2 minutes:
 *   swipe left  → delete (StarkRed feedback)
 *   swipe right → archive
 *
 * Top: OTP widget. Middle: paginated triage deck. Bottom: FloatingIsland.
 */
@Composable
fun TriageScreen(
    viewModel: TriageViewModel = hiltViewModel(),
    onEmailClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val emailItems = viewModel.emails.collectAsLazyPagingItems()
    val otpItems = viewModel.otpEmails.collectAsLazyPagingItems()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cleanupExpiredOtps()
        viewModel.sync()
    }

    val clipboardManager = LocalClipboardManager.current
    val islandState = if (emailItems.itemCount > 0)
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
            TriageHeader(
                triageCount = emailItems.itemCount,
                isSyncing = isSyncing,
                onSync = { viewModel.sync() },
                onSearchClick = onSearchClick
            )

            // ── OTP Widget Section ──────────────────────────────────────────
            if (otpItems.itemCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val firstOtp = otpItems[0]
                    if (firstOtp != null) {
                        val code = FallbackGenerator.extractOtp(
                            firstOtp.bodyMarkdown.ifBlank { firstOtp.snippet }
                        )
                        if (code.isNotEmpty()) {
                            val remainingMs = firstOtp.expiresAt - System.currentTimeMillis()
                            val remainingSec = (remainingMs / 1000).coerceAtLeast(0)
                            OtpCard(
                                sender = firstOtp.sender,
                                otp = code.chunked(1).joinToString(" "),
                                expiresInSeconds = remainingSec,
                                tick = true,
                                onCopy = { otpCode ->
                                    clipboardManager.setText(AnnotatedString(otpCode))
                                }
                            )
                        }
                    }
                }
            }

            // ── Triage Deck ─────────────────────────────────────────────────
            TriageDeck(
                emails = emailItems,
                onEmailClick = onEmailClick,
                onDelete = { viewModel.delete(it) },
                onArchive = { viewModel.archive(it) },
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                onLoadMore = { viewModel.loadMore() },
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
            val firstEmail = if (emailItems.itemCount > 0) emailItems[0] else null
            FloatingIsland(
                state = islandState,
                onReply = { firstEmail?.let { onEmailClick(it.id) } },
                onArchive = { firstEmail?.let { viewModel.archive(it.id) } },
                onStar = { /* TODO: add star action */ },
                onDelete = { firstEmail?.let { viewModel.delete(it.id) } }
            )
        }
    }
}

// ===========================================================================
// Header
// ===========================================================================

@Composable
private fun TriageHeader(
    triageCount: Int,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
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
            SearchButton(onSearch = onSearchClick)
            SyncButton(isSyncing = isSyncing, onSync = onSync)
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

@Composable
private fun SearchButton(onSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onSearch)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = MutedGray,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SyncButton(
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(enabled = !isSyncing, onClick = onSync)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MutedGray
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Sync",
                tint = MutedGray,
                modifier = Modifier.size(14.dp)
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
    onEmailClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onArchive: (String) -> Unit,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
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
                onOpen = { onEmailClick(email.id) },
                onDelete = { onDelete(email.id) },
                onArchive = { onArchive(email.id) }
            )
        }
        if (hasMore) {
            item(key = "load-more") {
                LoadMoreButton(
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun LoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MutedGray
            )
        } else {
            Text(
                text = "LOAD MORE",
                fontFamily = NDot,
                fontSize = 13.sp,
                color = PureWhite
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
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }

    // Visual feedback: red tint on delete, green tint on archive.
    val backgroundColor = when {
        dismissed -> Color.Transparent
        offsetX < -SWIPE_THRESHOLD / 2 ->
            StarkRed.copy(alpha = 0.3f * (kotlin.math.abs(offsetX) / 300f).coerceIn(0f, 1f))
        offsetX > SWIPE_THRESHOLD / 2 ->
            Color(0xFF1B5E20).copy(alpha = 0.3f * (offsetX / 300f).coerceIn(0f, 1f))
        else -> SurfaceDark
    }

    // Label shown during swipe.
    val swipeLabel = when {
        dismissed -> ""
        offsetX < -SWIPE_THRESHOLD / 2 -> "DELETE"
        offsetX > SWIPE_THRESHOLD / 2 -> "ARCHIVE"
        else -> ""
    }

    if (!dismissed) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onOpen)
                .graphicsLayer {
                    translationX = offsetX
                    rotationZ = (offsetX / 50f).coerceIn(-8f, 8f)
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -SWIPE_THRESHOLD -> {
                                    dismissed = true; onDelete()
                                }
                                offsetX > SWIPE_THRESHOLD -> {
                                    dismissed = true; onArchive()
                                }
                                else -> offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
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
                            else -> Color(0xFF4CAF50)
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
                        text = email.summary.ifBlank { email.snippet },
                        fontFamily = Geist,
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
                TriageHeader(triageCount = 0, isSyncing = false, onSync = {})
            }
        }
    }
}
