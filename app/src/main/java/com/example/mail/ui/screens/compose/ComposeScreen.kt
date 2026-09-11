package com.example.mail.ui.screens.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.Geist
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import com.example.mail.ui.theme.StarkRed

@Composable
fun ComposeScreen(
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val to by viewModel.to.collectAsState()
    val cc by viewModel.cc.collectAsState()
    val bcc by viewModel.bcc.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val body by viewModel.body.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()

    var showCcBcc by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(sendResult) {
        if (sendResult == false) {
            showError = true
            viewModel.consumeResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            Text(
                text = "COMPOSE",
                fontFamily = NDot,
                fontSize = 20.sp,
                color = PureWhite
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (to.isBlank() || isSending) BorderGray
                        else PureWhite
                    )
                    .clickable(
                        enabled = to.isNotBlank() && !isSending,
                        onClick = { viewModel.send(onSent) }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = OLEDBlack
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = OLEDBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (showError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEND FAILED — TRY AGAIN",
                    fontFamily = NDot,
                    fontSize = 12.sp,
                    color = StarkRed
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MutedGray,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { showError = false }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            ComposeField(
                label = "TO",
                value = to,
                hint = "name@example.com",
                onChange = { viewModel.to.value = it }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (showCcBcc) "HIDE CC / BCC" else "CC / BCC",
                    fontFamily = NDot,
                    fontSize = 11.sp,
                    color = MutedGray,
                    modifier = Modifier.clickable { showCcBcc = !showCcBcc }
                )
            }
            if (showCcBcc) {
                ComposeField(
                    label = "CC",
                    value = cc,
                    hint = "cc@example.com",
                    onChange = { viewModel.cc.value = it }
                )
                ComposeField(
                    label = "BCC",
                    value = bcc,
                    hint = "bcc@example.com",
                    onChange = { viewModel.bcc.value = it }
                )
            }
            ComposeField(
                label = "SUBJECT",
                value = subject,
                hint = "Subject",
                onChange = { viewModel.subject.value = it }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = { viewModel.body.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 200.dp),
                    textStyle = TextStyle(
                        fontFamily = Geist,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = PureWhite
                    ),
                    decorationBox = { inner ->
                        if (body.isEmpty()) {
                            Text(
                                text = "Write your message…",
                                fontFamily = Geist,
                                fontSize = 15.sp,
                                color = MutedGray
                            )
                        }
                        inner()
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SAVE DRAFT",
                    fontFamily = NDot,
                    fontSize = 13.sp,
                    color = PureWhite,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BorderGray, RoundedCornerShape(50))
                        .clickable { viewModel.saveDraft(onBack) }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ComposeField(
    label: String,
    value: String,
    hint: String,
    onChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = NDot,
            fontSize = 11.sp,
            color = MutedGray
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Geist,
                fontSize = 15.sp,
                color = PureWhite
            ),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        fontFamily = Geist,
                        fontSize = 15.sp,
                        color = MutedGray
                    )
                }
                inner()
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(BorderGray)
                .padding(vertical = 0.5.dp)
        )
    }
}
