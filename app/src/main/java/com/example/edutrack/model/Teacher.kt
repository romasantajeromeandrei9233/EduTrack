package com.example.edutrack.model

data class Teacher(
    val teacherId: String = "",
    val name: String = "",
    val email: String = "",
    val classList: List<String> = emptyList(),
    val fcmToken: String = ""
)