package com.example.mail.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.mail.ui.theme.MutedGray
import com.example.mail.ui.theme.NDot
import com.example.mail.ui.theme.OLEDBlack
import com.example.mail.ui.theme.PureWhite
import java.util.concurrent.Executor

/**
 * Locks all email content behind a [BiometricPrompt]. Shows a pure-black
 * Nothing-style lock screen until the user authenticates. The prompt fires on
 * cold launch and again on every resume-from-background (ON_RESUME),
 * re-locking on each return.
 *
 * [BiometricManager.Authenticators.DEVICE_CREDENTIAL] is always allowed so a
 * device PIN/pattern/password remains a valid fallback — never a silent bypass.
 */
@Composable
fun BiometricGate(
    activity: FragmentActivity,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isPromptShowing by remember { mutableStateOf(false) }

    val executor: Executor = remember(activity) { ContextCompat.getMainExecutor(activity) }
    val prompt = remember(activity) {
        BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isPromptShowing = false
                    isAuthenticated = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isPromptShowing = false
                }
            }
        )
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Nothing Mail")
            .setSubtitle("Verify identity to view emails")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    fun requestAuthentication() {
        if (!isAuthenticated && !isPromptShowing) {
            isPromptShowing = true
            prompt.authenticate(promptInfo)
        }
    }

    DisposableEffect(activity) {
        // BiometricPrompt.authenticate() requires the host to be RESUMED.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestAuthentication()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
            prompt.cancelAuthentication()
        }
    }

    if (isAuthenticated) {
        content()
    } else {
        NothingLockScreen(
            onRetry = ::requestAuthentication,
            modifier = modifier
        )
    }
}

@Composable
private fun NothingLockScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .clickable(onClick = onRetry),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = "Locked",
                tint = MutedGray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MAIL",
                fontFamily = NDot,
                fontSize = 26.sp,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TAP TO UNLOCK",
                fontFamily = NDot,
                fontSize = 11.sp,
                color = MutedGray
            )
        }
    }
}