package com.example.mail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mail.ui.screens.triage.TriageScreen
import com.example.mail.ui.theme.NothingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the mail application. Annotated with @AndroidEntryPoint
 * to enable Hilt injection into Compose screens and ViewModels.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NothingTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "triage",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("triage") {
                        TriageScreen()
                    }
                    // Future destinations:
                    // composable("inbox") { InboxScreen() }
                    // composable("email/{id}") { backStackEntry ->
                    //     EmailDetailScreen(emailId = backStackEntry.arguments?.getString("id"))
                    // }
                }
            }
        }
    }
}
