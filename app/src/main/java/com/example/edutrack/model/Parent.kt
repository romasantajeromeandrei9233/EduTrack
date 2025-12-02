package com.example.edutrack.model

data class Parent(
    val parentId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val linkedStudentIds: List<String> = emptyList(),
    val fcmToken: String = ""
)