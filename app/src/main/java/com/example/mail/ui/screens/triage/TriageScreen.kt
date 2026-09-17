package com.example.mail.ui.screens.triage

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import com.example.mail.data.local.ConversationItem
import com.example.mail.data.local.EmailMessage
import com.example.mail.ui.components.DockTab
import com.example.mail.ui.components.DrawerContent
import com.example.mail.ui.components.NothingDock
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
 * Edge-to-edge overlay layout: the deck scrolls full-screen behind a
 * translucent top header and the bottom NothingDock, clearing both via
 * LazyColumn contentPadding derived from WindowInsets.systemBars.
 */
@Composable
fun TriageScreen(
    viewModel: TriageViewModel = hiltViewModel(),
    onEmailClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onComposeClick: () -> Unit = {},
    onLabelsClick: () -> Unit = {}
) {
    val conversationItems = viewModel.conversations.collectAsLazyPagingItems()
    val otpItems = viewModel.otpEmails.collectAsLazyPagingItems()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val bundleFilter by viewModel.bundleFilter.collectAsState()
    val accountEmail by viewModel.accountEmail.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.cleanupExpiredOtps()
        viewModel.sync()
    }

    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val statusBarTop = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }
    val navBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = OLEDBlack,
                    drawerContentColor = PureWhite
                ) {
                    DrawerContent(
                        accountEmail = accountEmail,
                        activeBundle = bundleFilter,
                        onInbox = {
                            drawerScope.launch { drawerState.close() }
                            viewModel.setBundleFilter(null)
                        },
                        onBundle = { bundle ->
                            drawerScope.launch { drawerState.close() }
                            viewModel.setBundleFilter(bundle)
                        },
                        onLabels = {
                            drawerScope.launch { drawerState.close() }
                            onLabelsClick()
                        }
                    )
                }
            }
        ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
        // ── Deck (full-screen, scrolls behind overlays) ──────────────────
        TriageDeck(
            conversations = conversationItems,
            onEmailClick = onEmailClick,
            onDelete = { viewModel.delete(it) },
            onArchive = { viewModel.archive(it) },
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            onLoadMore = { viewModel.loadMore() },
            contentPadding = PaddingValues(
                top = statusBarTop + 88.dp,
                bottom = navBarBottom + 108.dp
            ),
            otpHeader = {
                if (otpItems.itemCount > 0) {
                    val firstOtp = otpItems[0]
                    if (firstOtp != null) {
                        val code = FallbackGenerator.extractOtp(
                            firstOtp.bodyMarkdown.ifBlank { firstOtp.snippet }
                        )
                        if (code.isNotEmpty()) {
                            val remainingMs = firstOtp.expiresAt - System.currentTimeMillis()
                            val remainingSec = (remainingMs / 1000).coerceAtLeast(0)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
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
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top fade scrim (content dissolves underneath, ChatGPT-style) ─
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(statusBarTop + 120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            OLEDBlack,
                            OLEDBlack.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── Top header (overlaid, fully transparent) ─────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = statusBarTop)
        ) {
            TriageHeader(
                triageCount = conversationItems.itemCount,
                isSyncing = isSyncing,
                onSync = { viewModel.sync() },
                onComposeClick = onComposeClick,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                activeFilter = bundleFilter,
                onClearFilter = { viewModel.clearBundleFilter() }
            )
        }

        // ── Bottom dock (overlaid, clears navigation bar) ────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBarBottom + 12.dp),
            contentAlignment = Alignment.Center
        ) {
            NothingDock(
                activeTab = if (bundleFilter == null) DockTab.INBOX else DockTab.BUNDLES,
                onInbox = { viewModel.clearBundleFilter() },
                onBundles = { viewModel.cycleBundleFilter() },
                onSearch = onSearchClick
            )
        }
        }
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
    onComposeClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    activeFilter: String? = null,
    onClearFilter: () -> Unit = {}
) {
    val dateLabel = SimpleDateFormat("MMM d", Locale.US).format(Date())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onMenuClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Menu",
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ComposeButton(onCompose = onComposeClick)
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
        if (activeFilter != null) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onClearFilter)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FILTER: ${activeFilter.uppercase()}",
                    fontFamily = NDot,
                    fontSize = 11.sp,
                    color = PureWhite
                )
                Text(
                    text = "✕",
                    fontFamily = NDot,
                    fontSize = 11.sp,
                    color = MutedGray
                )
            }
        }
    }
}

@Composable
private fun ComposeButton(onCompose: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PureWhite)
            .clickable(onClick = onCompose)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Compose",
            tint = OLEDBlack,
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
    conversations: androidx.paging.compose.LazyPagingItems<ConversationItem>,
    onEmailClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onArchive: (String) -> Unit,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues,
    otpHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "otp-header") {
            otpHeader()
        }
        items(
            count = conversations.itemCount,
            key = { conversations[it]?.message?.id ?: "item-$it" }
        ) { index ->
            val item = conversations[index] ?: return@items
            SwipeableEmailCard(
                email = item.message,
                unreadCount = item.unreadCount,
                onOpen = { onEmailClick(item.message.id) },
                onDelete = { onDelete(item.message.threadId) },
                onArchive = { onArchive(item.message.threadId) }
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
    unreadCount: Int = 0,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }

    // Visual feedback: red tint on delete, white tint on archive.
    val backgroundColor = when {
        dismissed -> Color.Transparent
        offsetX < -SWIPE_THRESHOLD / 2 ->
            StarkRed.copy(alpha = 0.3f * (kotlin.math.abs(offsetX) / 300f).coerceIn(0f, 1f))
        offsetX > SWIPE_THRESHOLD / 2 ->
            PureWhite.copy(alpha = 0.3f * (offsetX / 300f).coerceIn(0f, 1f))
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
                            else -> PureWhite
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email.sender,
                            fontFamily = NDot,
                            fontSize = 13.sp,
                            color = if (unreadCount > 0) PureWhite else MutedGray,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount",
                                fontFamily = NDot,
                                fontSize = 11.sp,
                                color = PureWhite,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
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
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TriageHeader(triageCount = 0, isSyncing = false, onSync = {})
            }
        }
    }
}
