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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = authRepository.currentUser

        if (currentUser != null) {
            // User is logged in, check role and navigate
            lifecycleScope.launch {
                val roleResult = authRepository.getUserRole(currentUser.uid)
                roleResult.fold(
                    onSuccess = { role ->
                        navigateBasedOnRole(role)
                    },
                    onFailure = {
                        // Error getting role, show login
                        showLoginScreen()
                    }
                )
            }
        } else {
            // No user, show login
            showLoginScreen()
        }
    }

    private fun showLoginScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }

    private fun navigateBasedOnRole(role: UserRole) {
        val intent = when (role) {
            UserRole.TEACHER -> Intent(this, TeacherDashboardActivity::class.java)
            UserRole.PARENT -> Intent(this, ParentDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}