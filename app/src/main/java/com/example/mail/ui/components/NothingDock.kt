package com.example.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite

enum class DockTab {
    INBOX,
    BUNDLES,
    SEARCH
}

private data class DockItem(
    val tab: DockTab,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun NothingDock(
    activeTab: DockTab,
    onInbox: () -> Unit,
    onBundles: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DockItem(DockTab.INBOX, Icons.Outlined.Inbox, "Inbox", onInbox),
        DockItem(DockTab.BUNDLES, Icons.Outlined.AllInbox, "Bundles", onBundles),
        DockItem(DockTab.SEARCH, Icons.Outlined.Search, "Search", onSearch)
    )

    Row(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth(0.72f)
            .clip(CircleShape)
            .background(Color(0xCC121212))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            DockButton(
                item = item,
                active = item.tab == activeTab
            )
        }
    }
}

@Composable
private fun DockButton(
    item: DockItem,
    active: Boolean
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (active) OLEDBlack.copy(alpha = 0.65f) else Color.Transparent)
            .clickable(onClick = item.onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (active) PureWhite else MutedGray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview("Nothing Dock", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NothingDockPreview() {
    NothingTheme {
        NothingDock(
            activeTab = DockTab.INBOX,
            onInbox = {},
            onBundles = {},
            onSearch = {}
        )
    }
}
