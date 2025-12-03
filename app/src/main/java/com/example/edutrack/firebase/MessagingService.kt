package com.example.edutrack.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.edutrack.R
import com.example.edutrack.ui.parent.StudentAttendanceActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "attendance_notifications"
        private const val CHANNEL_NAME = "Attendance Updates"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📬 Message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(
                title = it.title ?: "EduTrack",
                body = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM token: $token")

        // Save token to Firestore
        saveTokenToFirestore(token)
    }

    /**
     * Handle data payload from FCM
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val studentId = data["studentId"]
        val studentName = data["studentName"]
        val status = data["status"]
        val date = data["date"]

        when (type) {
            "attendance_update" -> {
                // Show notification for attendance update
                showNotification(
                    title = "Attendance Update",
                    body = "$studentName marked $status on $date",
                    data = data
                )
            }
            "excuse" -> {
                // Show notification for excuse letter
                showNotification(
                    title = "Excuse Letter",
                    body = "$studentName submitted an excuse letter",
                    data = data
                )
            }
        }
    }

    /**
     * Display local notification
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()

        // Create intent for notification click
        val intent = Intent(this, StudentAttendanceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("STUDENT_ID", data["studentId"])
            putExtra("STUDENT_NAME", data["studentName"])
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

        Log.d(TAG, "🔔 Notification displayed: $title")
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for student attendance updates"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Save FCM token to Firestore
     */
    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Check user role and update appropriate collection
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")

                val collection = when (role) {
                    "TEACHER" -> "teachers"
                    "PARENT" -> "parents"
                    else -> return@addOnSuccessListener
                }

                db.collection(collection)
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ FCM token saved to Firestore ($collection)")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to save FCM token: ${e.message}")
                    }
            }
    }
}