package com.example.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.SurfaceDark

/**
 * The category of an extracted action card. Determines the surface label and
 * (optionally) the leading icon.
 */
enum class ActionCardType {
    DATE, TRACKING, LINK
}

/**
 * Compact, boxy Nothing OS widget to surface an extracted action — a meeting
 * date, a package tracking ID, or a link — pinned to the top of a thread.
 *
 * Uses a dot-matrix header + dashed border like a Nothing home-screen widget.
 * The value is prominent, and the whole card is tappable to act on it.
 *
 * @param type   how the card is presented (label + icon)
 * @param label  short contextual label, e.g. "FLIGHT" or "TRACKING"
 * @param value  the extracted data, e.g. "UA 1780", "1Z999AA10123456784"
 * @param onClick invoked when the card is tapped
 */
@Composable
fun ActionCard(
    type: ActionCardType,
    label: String,
    value: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ActionCardType.DATE -> Icons.Outlined.Schedule
        ActionCardType.TRACKING -> Icons.Outlined.LocalShipping
        ActionCardType.LINK -> Icons.Outlined.Link
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon in a bordered square (boxy widget treatment).
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PureWhite,
                modifier = Modifier.width(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Dot-matrix header line.
            Text(
                text = label.uppercase(),
                fontFamily = NDot,
                fontSize = 11.sp,
                color = MutedGray
            )
            // Prominent value.
            Text(
                text = value,
                fontFamily = NDot,
                fontSize = 16.sp,
                color = PureWhite
            )
        }
    }
}

@Preview("Action Card — Tracking", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ActionCardTrackingPreview() {
    NothingTheme {
        ActionCard(
            type = ActionCardType.TRACKING,
            label = "Tracking",
            value = "1Z999AA10123456784",
            onClick = {}
        )
    }
}

@Preview("Action Card — Date", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ActionCardDatePreview() {
    NothingTheme {
        ActionCard(
            type = ActionCardType.DATE,
            label = "Meeting",
            value = "SEP 12 · 14:30",
            onClick = {}
        )
    }
}
