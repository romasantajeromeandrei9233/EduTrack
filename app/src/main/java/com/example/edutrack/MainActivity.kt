package com.example.edutrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in - navigate based on role (will implement in M1)
            // For now, just log
            android.util.Log.d("MainActivity", "User signed in: ${currentUser.email}")
        } else {
            // No user signed in - show login (will implement in M1)
            android.util.Log.d("MainActivity", "No user signed in")
        }
    }
}