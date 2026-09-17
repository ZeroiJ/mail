package com.example.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.util.BundleType

/**
 * Nothing-styled navigation drawer content. Monochrome panel with N-Dot
 * section dividers and Geist rows; the selected destination renders as an
 * inverted pill (white bg, black text). Gmail categories map to local
 * bundles: Purchases→RECEIPT, Promotions→NEWSLETTER, Social→SOCIAL,
 * Updates→LOGISTICS.
 */
@Composable
fun DrawerContent(
    accountEmail: String?,
    activeBundle: String?,
    onInbox: () -> Unit,
    onBundle: (String) -> Unit,
    onLabels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(OLEDBlack)
            .border(width = 1.dp, color = BorderGray)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "MAIL",
            fontFamily = NDot,
            fontSize = 22.sp,
            color = PureWhite
        )
        if (!accountEmail.isNullOrBlank()) {
            Text(
                text = accountEmail,
                fontFamily = Geist,
                fontSize = 12.sp,
                color = MutedGray
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        DrawerSection(title = "MAILBOX")
        DrawerRow(
            label = "Inbox",
            selected = activeBundle == null,
            onClick = onInbox
        )

        Spacer(modifier = Modifier.height(8.dp))
        DrawerSection(title = "SMART BUNDLES")
        DrawerRow(
            label = "Purchases",
            selected = activeBundle == BundleType.RECEIPT.label,
            onClick = { onBundle(BundleType.RECEIPT.label) }
        )
        DrawerRow(
            label = "Promotions",
            selected = activeBundle == BundleType.NEWSLETTER.label,
            onClick = { onBundle(BundleType.NEWSLETTER.label) }
        )
        DrawerRow(
            label = "Social",
            selected = activeBundle == BundleType.SOCIAL.label,
            onClick = { onBundle(BundleType.SOCIAL.label) }
        )
        DrawerRow(
            label = "Updates",
            selected = activeBundle == BundleType.LOGISTICS.label,
            onClick = { onBundle(BundleType.LOGISTICS.label) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        DrawerSection(title = "SYSTEM")
        DrawerRow(
            label = "Labels",
            selected = false,
            onClick = onLabels
        )
    }
}

@Composable
private fun DrawerSection(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontFamily = NDot,
            fontSize = 11.sp,
            color = MutedGray
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderGray)
        )
    }
}

@Composable
private fun DrawerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (selected) PureWhite else OLEDBlack)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = NDot,
            fontSize = 13.sp,
            color = if (selected) OLEDBlack else PureWhite
        )
    }
}
