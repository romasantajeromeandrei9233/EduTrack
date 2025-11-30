package com.example.edutrack.model

enum class UserRole {
    TEACHER,
    PARENT
}

data class User(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.PARENT,
    val name: String = ""
)