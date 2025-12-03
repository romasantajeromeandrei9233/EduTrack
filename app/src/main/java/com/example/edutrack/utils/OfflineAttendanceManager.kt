package com.example.edutrack.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.edutrack.workers.AttendanceSyncWorker
import kotlinx.coroutines.guava.await
import java.util.concurrent.TimeUnit

object OfflineAttendanceManager {

    /**
     * Check if device has internet connection
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Schedule sync work when connectivity is available
     */
    fun scheduleSyncWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            AttendanceSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Cancel scheduled sync work
     */
    fun cancelSyncWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(AttendanceSyncWorker.WORK_NAME)
    }

    /**
     * Check if there are pending syncs
     */
    suspend fun hasPendingSyncs(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(AttendanceSyncWorker.WORK_NAME)
            .await()

        return workInfos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }
}