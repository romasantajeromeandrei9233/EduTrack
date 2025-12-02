package com.example.edutrack.model

import com.google.firebase.Timestamp

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED
}

data class Attendance(
    val attendanceId: String = "",
    val studentId: String = "",
    val date: Timestamp = Timestamp.now(),
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val teacherId: String = "",
    val notes: String = "",
    val synced: Boolean = true // For offline tracking
)