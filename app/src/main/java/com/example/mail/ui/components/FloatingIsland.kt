package com.example.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.SurfaceDarkElevated
import com.example.mail.ui.theme.StarkRed

/**
 * Contextual Action Floating Island — a translucent, pill-shaped glassmorphic
 * bar hovering at the bottom of the screen.
 *
 * - [FloatingIslandState.Idle]: shown during normal inbox navigation (a subtle
 *   chevron/compose affordance). Minimal, unobtrusive.
 * - [FloatingIslandState.ThreadSelected]: reveals Reply / Archive / Star /
 *   Delete actions for the selected thread.
 *
 * The island reveals actions only on thread selection or scroll (callers drive
 * the [FloatingIslandState]).
 */
enum class FloatingIslandState {
    /** Normal navigation — idle, minimal island. */
    Idle,

    /** A thread is selected — reply/archive/star/delete actions revealed. */
    ThreadSelected
}

/** Single action button rendered inside the island. */
data class FloatingIslandAction(
    val icon: ImageVector,
    val label: String,
    val tint: Color = PureWhite,
    val onClick: () -> Unit = {}
)

/**
 * Pill-shaped glassmorphic action island.
 *
 * @param state       controls which actions are shown
 * @param onReply     reply action (thread-selected only)
 * @param onArchive   archive action (thread-selected only)
 * @param onStar      star action (thread-selected only)
 * @param onDelete    delete action — destructive, uses StarkRed
 * @param modifier    applied to the island container
 */
@Composable
fun FloatingIsland(
    state: FloatingIslandState,
    onReply: () -> Unit = {},
    onArchive: () -> Unit = {},
    onStar: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCompose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val actions = when (state) {
        FloatingIslandState.Idle -> listOf(
            FloatingIslandAction(
                icon = Icons.Outlined.MailOutline,
                label = "Compose",
                tint = PureWhite,
                onClick = onCompose
            )
        )
        FloatingIslandState.ThreadSelected -> listOf(
            FloatingIslandAction(Icons.Outlined.Reply, "Reply", onClick = onReply),
            FloatingIslandAction(Icons.Outlined.Archive, "Archive", onClick = onArchive),
            FloatingIslandAction(Icons.Outlined.StarBorder, "Star", onClick = onStar),
            FloatingIslandAction(
                Icons.Outlined.Delete,
                "Delete",
                tint = StarkRed,
                onClick = onDelete
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3A3A3C).copy(alpha = 0.78f),
                            Color(0xFF1C1C1E).copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PureWhite.copy(alpha = 0.22f),
                            BorderGray.copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                FloatingIslandIconButton(action)
            }
        }
    }
}

@Composable
private fun FloatingIslandIconButton(action: FloatingIslandAction) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(onClick = action.onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = action.tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Preview("Floating Island — Idle", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FloatingIslandIdlePreview() {
    NothingTheme {
        FloatingIsland(state = FloatingIslandState.Idle)
    }
}

@Preview(
    "Floating Island — Thread Selected",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun FloatingIslandThreadSelectedPreview() {
    NothingTheme {
        FloatingIsland(
            state = FloatingIslandState.ThreadSelected,
            onReply = {},
            onArchive = {},
            onStar = {},
            onDelete = {}
        )
    }
}
