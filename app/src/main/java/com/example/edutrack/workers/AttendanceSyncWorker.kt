package com.example.edutrack.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.Attendance
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await


class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {


    companion object {
        const val TAG = "AttendanceSyncWorker"
        const val WORK_NAME = "attendance_sync_work"
    }


    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting attendance sync...")


        return try {
            val db = FirestoreService.db
            val attendanceCollection = db.collection(FirestoreService.ATTENDANCE_COLLECTION)


            // Query for unsynced attendance records
            val unsyncedSnapshot = attendanceCollection
                .whereEqualTo("synced", false)
                .get()
                .await()


            if (unsyncedSnapshot.isEmpty) {
                Log.d(TAG, "No unsynced attendance found")
                return Result.success()
            }


            val unsyncedAttendance = unsyncedSnapshot.toObjects(Attendance::class.java)
            Log.d(TAG, "Found ${unsyncedAttendance.size} unsynced attendance records")


            // Sync each record
            var successCount = 0
            var failCount = 0


            unsyncedAttendance.forEach { attendance ->
                try {
                    // Update attendance with synced = true
                    val syncedAttendance = attendance.copy(synced = true)


                    attendanceCollection
                        .document(attendance.attendanceId)
                        .set(syncedAttendance, SetOptions.merge())
                        .await()


                    successCount++
                    Log.d(TAG, "Synced attendance: ${attendance.attendanceId}")


                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "Failed to sync attendance ${attendance.attendanceId}: ${e.message}")
                }
            }


            Log.d(TAG, "Sync complete: $successCount success, $failCount failed")


            // Return success if at least one record was synced
            if (successCount > 0) {
                Result.success()
            } else {
                Result.retry()
            }


        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed: ${e.message}")
            Result.retry()
        }
    }
}
