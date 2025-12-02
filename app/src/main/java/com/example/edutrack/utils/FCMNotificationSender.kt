package com.example.edutrack.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object FCMNotificationSender {

    private const val TAG = "FCMNotificationSender"
    private const val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"

    // IMPORTANT: This is your Firebase Server Key
    // Get it from: Firebase Console → Project Settings → Cloud Messaging → Server Key
    // TODO: Replace with your actual server key
    private const val SERVER_KEY = "YOUR_SERVER_KEY_HERE"

    /**
     * Send attendance notification to parent
     * Call this after successfully saving attendance
     */
    suspend fun sendAttendanceNotification(
        studentId: String,
        studentName: String,
        status: String,
        date: Date
    ): Result<String> {
        return try {
            val db = FirebaseFirestore.getInstance()

            // 1. Get student's parentId
            val studentDoc = db.collection("students")
                .document(studentId)
                .get()
                .await()

            val parentId = studentDoc.getString("parentId")

            if (parentId.isNullOrBlank()) {
                return Result.failure(Exception("No parent linked to this student"))
            }

            // 2. Get parent's FCM token
            val parentDoc = db.collection("parents")
                .document(parentId)
                .get()
                .await()

            val fcmToken = parentDoc.getString("fcmToken")

            if (fcmToken.isNullOrBlank()) {
                return Result.failure(Exception("Parent has no FCM token"))
            }

            // 3. Format date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(date)

            // 4. Send notification
            val result = sendNotification(
                token = fcmToken,
                title = "Attendance Update",
                body = "$studentName marked $status on $dateStr",
                studentId = studentId,
                studentName = studentName,
                status = status,
                date = dateStr
            )

            Log.d(TAG, "Notification sent successfully")
            Result.success("Notification sent")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send FCM notification using HTTP POST
     */
    private suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        studentId: String,
        studentName: String,
        status: String,
        date: String
    ): Boolean {
        return try {
            val url = URL(FCM_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "key=$SERVER_KEY")
                doOutput = true
            }

            // Build JSON payload
            val payload = JSONObject().apply {
                put("to", token)

                // Notification payload (shows in system tray)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("sound", "default")
                    put("priority", "high")
                })

                // Data payload (for app to handle)
                put("data", JSONObject().apply {
                    put("studentId", studentId)
                    put("studentName", studentName)
                    put("status", status)
                    put("date", date)
                    put("type", "attendance_update")
                })
            }

            // Send request
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            // Check response
            val responseCode = connection.responseCode

            if (responseCode == 200) {
                Log.d(TAG, "FCM request successful")
                true
            } else {
                Log.e(TAG, "FCM request failed with code: $responseCode")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM request: ${e.message}")
            false
        }
    }

    /**
     * Send excuse letter notification to teacher
     */
    suspend fun sendExcuseNotification(
        teacherId: String,
        studentName: String,
        date: String
    ): Result<String> {
        return try {
            val db = FirebaseFirestore.getInstance()

            // Get teacher's FCM token
            val teacherDoc = db.collection("teachers")
                .document(teacherId)
                .get()
                .await()

            val fcmToken = teacherDoc.getString("fcmToken")

            if (fcmToken.isNullOrBlank()) {
                return Result.failure(Exception("Teacher has no FCM token"))
            }

            // Send notification
            sendNotification(
                token = fcmToken,
                title = "Excuse Letter Received",
                body = "$studentName submitted an excuse letter for $date",
                studentId = "",
                studentName = studentName,
                status = "excuse",
                date = date
            )

            Result.success("Notification sent to teacher")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send excuse notification: ${e.message}")
            Result.failure(e)
        }
    }
}