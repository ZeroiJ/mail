package com.example.mail

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mail.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point. Annotated with @HiltAndroidApp to enable
 * dependency injection across the entire app lifecycle. Implements
 * Configuration.Provider so WorkManager workers (see SyncWorker) are
 * built with Hilt's WorkerFactory.
 */
@HiltAndroidApp
class MailApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundSync()
    }

    /**
     * Enqueues the quiet background poller. KEEP policy means the
     * schedule is created once and is never duplicated across launches.
     */
    private fun scheduleBackgroundSync() {
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UNIQUE_BACKGROUND_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private companion object {
        const val UNIQUE_BACKGROUND_SYNC = "background-email-sync"
    }
}