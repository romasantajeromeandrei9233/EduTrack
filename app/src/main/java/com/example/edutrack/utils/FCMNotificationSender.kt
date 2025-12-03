package com.example.edutrack.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object FCMNotificationSender {

    private const val TAG = "FCMNotificationSender"

    // FCM v1 API endpoint
    // Format: https://fcm.googleapis.com/v1/projects/{project-id}/messages:send
    private const val FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/edutrack-9269a/messages:send"

    /**
     * Send attendance notification to parent using FCM v1 API
     */
    suspend fun sendAttendanceNotification(
        context: Context,
        studentId: String,
        studentName: String,
        status: String,
        date: Date
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Get student's parentId
                val studentDoc = db.collection("students")
                    .document(studentId)
                    .get()
                    .await()

                val parentId = studentDoc.getString("parentId")

                if (parentId.isNullOrBlank()) {
                    Log.w(TAG, "⚠️ No parent linked to student: $studentId")
                    return@withContext Result.failure(Exception("No parent linked to this student"))
                }

                // 2. Get parent's FCM token
                val parentDoc = db.collection("parents")
                    .document(parentId)
                    .get()
                    .await()

                val fcmToken = parentDoc.getString("fcmToken")

                if (fcmToken.isNullOrBlank()) {
                    Log.w(TAG, "⚠️ Parent has no FCM token: $parentId")
                    return@withContext Result.failure(Exception("Parent has no FCM token"))
                }

                // 3. Format date
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                val dateStr = dateFormat.format(date)

                // 4. Send notification using FCM v1
                val success = sendFCMv1Notification(
                    context = context,
                    token = fcmToken,
                    title = "Attendance Update",
                    body = "$studentName marked $status on $dateStr",
                    studentId = studentId,
                    studentName = studentName,
                    status = status,
                    date = dateStr
                )

                if (success) {
                    Log.d(TAG, "✅ Notification sent successfully to parent: $parentId")
                    Result.success("Notification sent")
                } else {
                    Log.e(TAG, "❌ Failed to send notification")
                    Result.failure(Exception("FCM request failed"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send notification: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Send FCM v1 notification using HTTP POST with OAuth 2.0
     */
    private suspend fun sendFCMv1Notification(
        context: Context,
        token: String,
        title: String,
        body: String,
        studentId: String,
        studentName: String,
        status: String,
        date: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get OAuth 2.0 Access Token
                val accessToken = GoogleAuthHelper.getAccessToken(context)

                if (accessToken == null) {
                    Log.e(TAG, "❌ Failed to obtain access token")
                    return@withContext false
                }

                // 2. Build FCM v1 payload
                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", token)

                        // Notification payload
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })

                        // Data payload
                        put("data", JSONObject().apply {
                            put("studentId", studentId)
                            put("studentName", studentName)
                            put("status", status)
                            put("date", date)
                            put("type", "attendance_update")
                            put("click_action", "OPEN_ATTENDANCE_ACTIVITY")
                        })

                        // Android-specific config
                        put("android", JSONObject().apply {
                            put("priority", "high")
                            put("notification", JSONObject().apply {
                                put("sound", "default")
                                put("click_action", "OPEN_ATTENDANCE_ACTIVITY")
                            })
                        })
                    })
                }

                // 3. Send HTTP POST request
                val url = URL(FCM_V1_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                // Write payload
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(message.toString())
                writer.flush()
                writer.close()

                // Check response
                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d(TAG, "✅ FCM v1 request successful (200 OK)")
                    Log.d(TAG, "Response: $response")
                    true
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                    Log.e(TAG, "❌ FCM v1 request failed with code: $responseCode")
                    Log.e(TAG, "Error response: $errorResponse")
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending FCM v1 request: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Send excuse letter notification to teacher using FCM v1
     */
    suspend fun sendExcuseNotification(
        context: Context,
        teacherId: String,
        studentName: String,
        date: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // Get teacher's FCM token
                val teacherDoc = db.collection("teachers")
                    .document(teacherId)
                    .get()
                    .await()

                val fcmToken = teacherDoc.getString("fcmToken")

                if (fcmToken.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Teacher has no FCM token"))
                }

                // Send notification
                val success = sendFCMv1Notification(
                    context = context,
                    token = fcmToken,
                    title = "Excuse Letter Received",
                    body = "$studentName submitted an excuse letter for $date",
                    studentId = "",
                    studentName = studentName,
                    status = "excuse",
                    date = date
                )

                if (success) {
                    Result.success("Notification sent to teacher")
                } else {
                    Result.failure(Exception("Failed to send notification"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send excuse notification: ${e.message}")
                Result.failure(e)
            }
        }
    }
}