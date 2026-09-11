package com.example.mail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mail.ui.screens.auth.SignInScreen
import com.example.mail.ui.screens.compose.ComposeScreen
import com.example.mail.ui.screens.reader.ReaderScreen
import com.example.mail.ui.screens.search.SearchScreen
import com.example.mail.ui.screens.triage.TriageScreen
import com.example.mail.ui.theme.NothingTheme
import com.example.mail.util.AuthManager
import com.example.mail.util.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main entry point for the mail application.
 *
 * Hardening (see AGENTS.md "Security & Anti-Exploit Protocol"):
 * - FLAG_SECURE blocks OS background snapshots and screen recording; set
 *   BEFORE setContent so no frame is ever capturable.
 * - The NavHost is gated behind [AuthManager.authState]: unauthenticated
 *   users see [SignInScreen], and the triage queue only composes after
 *   sign-in succeeds. [AppCompatActivity] (a [FragmentActivity]) is required
 *   so [BiometricGate] can host a BiometricPrompt that re-locks on every
 *   resume-from-background before any email content renders.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            NothingTheme {
                val scope = rememberCoroutineScope()
                val authState by authManager.authState.collectAsState()

                if (authState is AuthState.Authenticated) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "triage",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("triage") {
                            TriageScreen(
                                onEmailClick = { emailId ->
                                    navController.navigate("reader/$emailId")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onComposeClick = {
                                    navController.navigate("compose")
                                }
                            )
                        }
                        composable("compose") {
                            ComposeScreen(
                                onBack = { navController.popBackStack() },
                                onSent = { navController.popBackStack() }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onBack = { navController.popBackStack() },
                                onEmailClick = { emailId ->
                                    navController.navigate("reader/$emailId")
                                }
                            )
                        }
                        composable(
                            route = "reader/{emailId}",
                            arguments = listOf(
                                navArgument("emailId") { type = NavType.StringType }
                            )
                        ) {
                            ReaderScreen(onBack = { navController.popBackStack() })
                        }
                    }
                } else {
                    SignInScreen(
                        onSignIn = {
                            scope.launch {
                                authManager.signIn(this@MainActivity)
                            }
                        }
                    )
                }
            }
        }
    }
}