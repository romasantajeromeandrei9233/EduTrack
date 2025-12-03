package com.example.edutrack

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.edutrack.model.UserRole
import com.example.edutrack.repository.AuthRepository
import com.example.edutrack.ui.login.LoginFragment
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in - navigate based on role
            lifecycleScope.launch {
                val roleResult = AuthRepository().getUserRole(currentUser.uid)
                roleResult.fold(
                    onSuccess = { role ->
                        val intent = when (role) {
                            UserRole.TEACHER -> Intent(this@MainActivity, TeacherDashboardActivity::class.java)
                            UserRole.PARENT -> Intent(this@MainActivity, ParentDashboardActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onFailure = {
                        // Error getting role - show login
                        android.util.Log.e("MainActivity", "Failed to get user role: ${it.message}")
                        showLoginFragment()
                    }
                )
            }
        } else {
            // No user signed in - show login fragment
            showLoginFragment()
        }
    }

    private fun showLoginFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }
}