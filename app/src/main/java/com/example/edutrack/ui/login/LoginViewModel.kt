package com.example.edutrack.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.model.UserRole
import com.example.edutrack.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: UserRole) : AuthState()
    data class Error(val message: String) : AuthState()
}

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signIn(email, password)

            result.fold(
                onSuccess = { user ->
                    // Get user role
                    val roleResult = authRepository.getUserRole(user.uid)
                    roleResult.fold(
                        onSuccess = { role ->
                            _authState.value = AuthState.Success(role)
                        },
                        onFailure = { exception ->
                            _authState.value = AuthState.Error(
                                exception.message ?: "Failed to get user role"
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Sign in failed"
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, name: String, role: UserRole) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signUp(email, password, name, role)

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.Success(role)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Sign up failed"
                    )
                }
            )
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}