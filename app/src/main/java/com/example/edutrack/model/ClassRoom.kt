package com.example.edutrack.model

data class ClassRoom(
    val classId: String = "",
    val className: String = "",
    val teacherId: String = "",
    val studentIds: List<String> = emptyList()
)