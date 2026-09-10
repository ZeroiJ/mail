package com.example.mail.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mail.domain.repository.EmailRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Background poller replacing Cloud Pub/Sub. Runs every 15 minutes
 * (schedule owned by MailApp) while the device has network connectivity
 * and pulls the recent inbox into Room.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val emailRepository: EmailRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        emailRepository.syncRecentEmails()
    }.fold(
        onSuccess = {
            Log.i(TAG, "Background sync completed")
            Result.success()
        },
        onFailure = { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Background sync failed (${error.message}); scheduling retry")
            Result.retry()
        }
    )

    private companion object {
        const val TAG = "SyncWorker"
    }
}