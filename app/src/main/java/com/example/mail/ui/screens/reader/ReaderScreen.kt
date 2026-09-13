package com.example.mail.ui.screens.reader

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mail.data.local.EmailMessage
import com.example.mail.ui.components.ActionCard
import com.example.mail.ui.components.ActionCardType
import com.example.mail.ui.components.OtpCard
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.util.FallbackGenerator
import com.example.mail.util.HtmlStripper

/**
 * Reader / detail view bound to the local Room flow. Renders the raw HTML
 * in a sandboxed WebView (JS/file/content access all disabled per the
 * AGENTS.md payload sandboxing rule) with tracking pixels already stripped
 * at sync time. Pins extracted OTPs and actionable data on top.
 * Never waits on the network.
 */
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onReply: (String) -> Unit = {},
    onManageLabels: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack)
    ) {
        val message = email
        if (message == null) {
            missingState(onBack = onBack)
        } else {
            readerContent(email = message, onBack = onBack, onReply = onReply, onManageLabels = onManageLabels)
        }
    }
}

@Composable
private fun missingState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        readerHeader(sender = "", subject = "MESSAGE MISSING", onBack = onBack)
    }
}

@Composable
private fun readerContent(
    email: EmailMessage,
    onBack: () -> Unit,
    onReply: (String) -> Unit,
    onManageLabels: () -> Unit
) {
    // Older rows may lack bodyMarkdown, so re-strip the HTML on the fly.
    val body = email.bodyMarkdown.ifBlank { HtmlStripper.strip(email.bodyHtml) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
    ) {
        readerHeader(sender = email.sender, subject = email.subject, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderGray)
        )

        ReplyActionRow(
            onReply = { onReply("reply") },
            onReplyAll = { onReply("replyAll") },
            onForward = { onReply("forward") }
        )

        LabelSection(
            messageId = email.id,
            onManageLabels = onManageLabels
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (email.isOTP) {
                val code = FallbackGenerator.extractOtp(body)
                if (code.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val expiresInSeconds =
                        ((email.expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                    OtpCard(
                        sender = email.sender,
                        otp = code.chunked(1).joinToString(" "),
                        expiresInSeconds = expiresInSeconds,
                        tick = true,
                        onCopy = { otp -> clipboardManager.setText(AnnotatedString(otp)) }
                    )
                }
            }

            val findings = remember(body) { FallbackGenerator.extractActionableData(body) }
            val actionCards = remember(findings) {
                findings.split("; ")
                    .mapNotNull { segment ->
                        when {
                            segment.startsWith("TRACKING:") ->
                                ActionCardType.TRACKING to segment.removePrefix("TRACKING:").trim()
                            segment.startsWith("DATE:") ->
                                ActionCardType.DATE to segment.removePrefix("DATE:").trim()
                            else -> null
                        }
                    }
                    .distinctBy { it.second }
            }
            actionCards.forEach { (type, value) ->
                Spacer(modifier = Modifier.height(8.dp))
                ActionCard(
                    type = type,
                    label = if (type == ActionCardType.DATE) "DATE" else "TRACKING",
                    value = value
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SandboxedHtmlView(
                html = email.bodyHtml.ifBlank { email.bodyMarkdown },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SandboxedHtmlView(
    html: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            android.webkit.WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: android.webkit.WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            view?.context?.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(url)
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            return true
                        }
                        return false
                    }
                }
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(null, mobileWrap(html), "text/html", "UTF-8", null)
            }
        }
    )
}

private fun mobileWrap(html: String): String {
    val viewport = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
    val style = "<style>" +
        "img{max-width:100%!important;height:auto!important}" +
        "table{max-width:100%!important}" +
        "body{margin:0;padding:8px;word-wrap:break-word}" +
        "</style>"
    val headIndex = html.indexOf("<head", ignoreCase = true)
    if (headIndex < 0) return viewport + style + html
    val headClose = html.indexOf('>', headIndex)
    if (headClose < 0) return viewport + style + html
    return html.substring(0, headClose + 1) + viewport + style + html.substring(headClose + 1)
}

@Composable
private fun ReplyActionRow(
    onReply: () -> Unit,
    onReplyAll: () -> Unit,
    onForward: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReplyPill(label = "REPLY", primary = true, onClick = onReply)
        ReplyPill(label = "REPLY ALL", primary = false, onClick = onReplyAll)
        ReplyPill(label = "FORWARD", primary = false, onClick = onForward)
    }
}

@Composable
private fun ReplyPill(
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val pillModifier = if (primary) {
        Modifier
            .clip(RoundedCornerShape(50))
            .background(PureWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    }
    Box(
        modifier = pillModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = NDot,
            fontSize = 12.sp,
            color = if (primary) OLEDBlack else PureWhite
        )
    }
}

@Composable
private fun readerHeader(
    sender: String,
    subject: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, BorderGray, RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PureWhite,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = sender.uppercase(),
                fontFamily = NDot,
                fontSize = 12.sp,
                color = MutedGray
            )
            Text(
                text = subject,
                fontFamily = NDot,
                fontSize = 20.sp,
                color = PureWhite
            )
        }
    }
}