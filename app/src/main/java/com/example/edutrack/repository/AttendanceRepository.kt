package com.example.edutrack.repository

import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.Attendance
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AttendanceRepository {

    private val db: FirebaseFirestore = FirestoreService.db
    private val attendanceCollection = db.collection(FirestoreService.ATTENDANCE_COLLECTION)

    suspend fun markAttendance(attendance: Attendance): Result<String> {
        return try {
            val docRef = attendanceCollection.document()
            val attendanceWithId = attendance.copy(attendanceId = docRef.id)
            docRef.set(attendanceWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAttendanceBatch(attendanceList: List<Attendance>): Result<Unit> {
        return try {
            val batch = db.batch()
            attendanceList.forEach { attendance ->
                val docRef = attendanceCollection.document()
                val attendanceWithId = attendance.copy(attendanceId = docRef.id)
                batch.set(docRef, attendanceWithId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByStudent(studentId: String): Result<List<Attendance>> {
        return try {
            val snapshot = attendanceCollection
                .whereEqualTo("studentId", studentId)
                // REMOVED: .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            // Sort manually in code instead
            val attendance = snapshot.toObjects(Attendance::class.java)
                .sortedByDescending { it.date.seconds }

            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByStudentAndDate(
        studentId: String,
        date: Timestamp
    ): Result<Attendance?> {
        return try {
            val startOfDay = getStartOfDay(date)
            val endOfDay = getEndOfDay(date)

            val snapshot = attendanceCollection
                .whereEqualTo("studentId", studentId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .await()

            val attendance = snapshot.toObjects(Attendance::class.java).firstOrNull()
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByDate(date: Timestamp): Result<List<Attendance>> {
        return try {
            val startOfDay = getStartOfDay(date)
            val endOfDay = getEndOfDay(date)

            val snapshot = attendanceCollection
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .await()

            val attendance = snapshot.toObjects(Attendance::class.java)
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAttendance(attendance: Attendance): Result<Unit> {
        return try {
            attendanceCollection.document(attendance.attendanceId).set(attendance).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(attendanceId: String): Result<Unit> {
        return try {
            attendanceCollection.document(attendanceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions to get start and end of day
    private fun getStartOfDay(timestamp: Timestamp): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp.toDate()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    private fun getEndOfDay(timestamp: Timestamp): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp.toDate()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return Timestamp(calendar.time)
    }
    /**
     * Mark attendance with offline support
     * Sets synced = false if offline, true if online
     */
    suspend fun markAttendanceOffline(
        attendance: Attendance,
        isOnline: Boolean
    ): Result<String> {
        return try {
            val docRef = attendanceCollection.document()
            val attendanceWithId = attendance.copy(
                attendanceId = docRef.id,
                synced = isOnline
            )

            docRef.set(attendanceWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark attendance batch with offline support
     */
    suspend fun markAttendanceBatchOffline(
        attendanceList: List<Attendance>,
        isOnline: Boolean
    ): Result<Unit> {
        return try {
            val batch = db.batch()
            attendanceList.forEach { attendance ->
                val docRef = attendanceCollection.document()
                val attendanceWithId = attendance.copy(
                    attendanceId = docRef.id,
                    synced = isOnline
                )
                batch.set(docRef, attendanceWithId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of unsynced attendance records
     */
    suspend fun getUnsyncedCount(): Result<Int> {
        return try {
            val snapshot = attendanceCollection
                .whereEqualTo("synced", false)
                .get()
                .await()

            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}