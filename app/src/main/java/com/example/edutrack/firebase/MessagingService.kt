package com.example.edutrack.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // TODO: Handle FCM messages in M5
        // For now, just log
        android.util.Log.d("FCM", "Message received: ${remoteMessage.data}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // TODO: Send token to Firestore in M5
        android.util.Log.d("FCM", "New token: $token")
    }
}