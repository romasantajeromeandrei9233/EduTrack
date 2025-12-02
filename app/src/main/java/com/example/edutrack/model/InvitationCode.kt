package com.example.edutrack.model

import com.google.firebase.Timestamp

data class InvitationCode(
    val codeId: String = "",
    val code: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val classId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val isUsed: Boolean = false,
    val usedBy: String = "" // Parent ID who used the code
)