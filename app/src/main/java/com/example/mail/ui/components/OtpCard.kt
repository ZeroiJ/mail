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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.SurfaceDark
import com.example.mail.ui.theme.StarkRed
import kotlinx.coroutines.delay

/**
 * Widget-style card for Ephemeral / Self-Destructing OTPs.
 *
 * Rendered like a Nothing home-screen widget: dashed border, dot-matrix header,
 * a large dot-matrix verification code, a StarkRed countdown, and a pill-shaped
 * "TAP TO COPY" action. The email self-destructs server-side after 24h (see
 * AGENTS.md); this card surfaces the code before it disappears.
 *
 * @param sender           sender display name
 * @param otp              the verification code
 * @param expiresInSeconds remaining seconds (0 → the email is gone)
 * @param tick             when true, the countdown ticks down live each second
 * @param onCopy           invoked when the user taps the copy action
 */
@Composable
fun OtpCard(
    sender: String,
    otp: String,
    expiresInSeconds: Long,
    tick: Boolean = true,
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var remaining by remember { mutableLongStateOf(expiresInSeconds) }

    LaunchedEffect(tick, expiresInSeconds) {
        remaining = expiresInSeconds
        if (tick) {
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
            }
        }
    }

    val mins = remaining / 60
    val secs = remaining % 60
    val countdown = "%02d:%02d".format(mins, secs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .dashedBorder(BorderGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dot-matrix header: sender + countdown.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sender.uppercase(),
                fontFamily = NDot,
                fontSize = 12.sp,
                color = MutedGray
            )
            Text(
                text = countdown,
                fontFamily = NDot,
                fontSize = 12.sp,
                color = StarkRed
            )
        }

        // Large dot-matrix verification code.
        Text(
            text = otp,
            fontFamily = NDot,
            fontSize = 40.sp,
            color = PureWhite,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Pill-shaped "TAP TO COPY" action.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .border(1.dp, BorderGray, RoundedCornerShape(50))
                .clickable { onCopy(otp) }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TAP TO COPY",
                fontFamily = NDot,
                fontSize = 11.sp,
                color = PureWhite
            )
        }
    }
}

/**
 * Overlays a dashed rounded border via Canvas, mimicking a Nothing home-screen
 * widget. The dashed treatment is shared by OtpCard and ActionCard.
 */
private fun Modifier.dashedBorder(
    color: Color,
    radiusDp: Int = 16,
    strokeWidthDp: Float = 1f,
    onDash: Float = 8f,
    offDash: Float = 6f
): Modifier = this.then(
    Modifier.drawDashedBorder(color, radiusDp, strokeWidthDp, onDash, offDash)
)

private fun Modifier.drawDashedBorder(
    color: Color,
    radiusDp: Int,
    strokeWidthDp: Float,
    onDash: Float,
    offDash: Float
): Modifier = this.then(
    Modifier.drawWithCache {
        val stroke = strokeWidthDp.dp.toPx()
        val radius = radiusDp.dp.toPx()
        val on = onDash.dp.toPx()
        val off = offDash.dp.toPx()
        onDrawBehind {
            val inset = stroke / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(
                    width = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off))
                )
            )
        }
    }
)

@Preview("OTP Card", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OtpCardPreview() {
    NothingTheme {
        OtpCard(
            sender = "GitHub",
            otp = "4 8 2 1 9",
            expiresInSeconds = 9 * 60 + 42,
            tick = false
        )
    }
}
