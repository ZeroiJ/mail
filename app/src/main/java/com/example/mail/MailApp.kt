package com.example.mail

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated with @HiltAndroidApp to enable
 * dependency injection across the entire app lifecycle.
 */
@HiltAndroidApp
class MailApp : Application()
