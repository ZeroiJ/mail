package com.example.mail.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mail.ui.theme.BorderGray
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite

/**
 * Minimalist sign-in gate shown when no valid Gmail token exists yet.
 * Pure OLED black, dot-matrix typography, typographic pill CTA.
 */
@Composable
fun SignInScreen(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MAIL",
                fontFamily = NDot,
                fontSize = 48.sp,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OFFLINE-FIRST INBOX",
                fontFamily = NDot,
                fontSize = 12.sp,
                color = MutedGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onSignIn)
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "SIGN IN WITH GOOGLE",
                    fontFamily = NDot,
                    fontSize = 13.sp,
                    color = PureWhite
                )
            }
        }
    }
}

@Preview("Sign In", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SignInScreenPreview() {
    NothingTheme {
        SignInScreen(onSignIn = {})
    }
}