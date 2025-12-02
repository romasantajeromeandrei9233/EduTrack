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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_MessagingService"
        private const val CHANNEL_ID = "attendance_notifications"
        private const val CHANNEL_NAME = "Attendance Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for student attendance updates"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data: ${remoteMessage.data}")

            val studentId = remoteMessage.data["studentId"] ?: ""
            val studentName = remoteMessage.data["studentName"] ?: "Your child"
            val status = remoteMessage.data["status"] ?: "updated"
            val date = remoteMessage.data["date"] ?: "today"

            val title = "Attendance Update"
            val message = "$studentName marked $status on $date"

            showNotification(title, message, studentId, studentName)
        }

        // Handle notification payload (if sent from Firebase Console for testing)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(
                it.title ?: "Attendance Update",
                it.body ?: "Attendance has been updated",
                "",
                ""
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save token to Firestore for current user
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Check if user is a parent
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val role = userDoc.getString("role")

                    if (role == "PARENT") {
                        FirebaseFirestore.getInstance()
                            .collection("parents")
                            .document(currentUser.uid)
                            .update("fcmToken", token)
                            .await()

                        Log.d(TAG, "FCM token saved to Firestore")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save FCM token: ${e.message}")
                }
            }
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        studentId: String,
        studentName: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open StudentAttendanceActivity
        val intent = Intent(this, StudentAttendanceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("STUDENT_ID", studentId)
            putExtra("STUDENT_NAME", studentName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification with unique ID
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}