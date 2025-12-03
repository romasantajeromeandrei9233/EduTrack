package com.example.edutrack

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.edutrack.repository.AuthRepository
import com.example.edutrack.ui.login.LoginFragment
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in - navigate based on role
            navigateBasedOnRole(currentUser.uid)
        } else {
            // No user signed in - show login
            showLoginScreen()
        }
    }

    private fun navigateBasedOnRole(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                authRepository.getUserRole(uid)
            }

            result.fold(
                onSuccess = { role ->
                    val intent = when (role.name) {
                        "TEACHER" -> Intent(this@MainActivity, TeacherDashboardActivity::class.java)
                        "PARENT" -> Intent(this@MainActivity, ParentDashboardActivity::class.java)
                        else -> null
                    }

                    intent?.let {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(it)
                        finish()
                    } ?: showLoginScreen()
                },
                onFailure = {
                    showLoginScreen()
                }
            )
        }
    }

    private fun showLoginScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }
}