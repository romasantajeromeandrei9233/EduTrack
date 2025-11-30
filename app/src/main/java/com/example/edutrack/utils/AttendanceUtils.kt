package com.example.edutrack.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.edutrack.R
import com.example.edutrack.model.AttendanceStatus

object AttendanceUtils {

    fun getStatusColor(context: Context, status: AttendanceStatus): Int {
        return when (status) {
            AttendanceStatus.PRESENT -> ContextCompat.getColor(context, R.color.status_present)
            AttendanceStatus.LATE -> ContextCompat.getColor(context, R.color.status_late)
            AttendanceStatus.ABSENT -> ContextCompat.getColor(context, R.color.status_absent)
            AttendanceStatus.EXCUSED -> ContextCompat.getColor(context, R.color.status_excused)
        }
    }

    fun getStatusColorLight(context: Context, status: AttendanceStatus): Int {
        return when (status) {
            AttendanceStatus.PRESENT -> ContextCompat.getColor(context, R.color.status_present_light)
            AttendanceStatus.LATE -> ContextCompat.getColor(context, R.color.status_late_light)
            AttendanceStatus.ABSENT -> ContextCompat.getColor(context, R.color.status_absent_light)
            AttendanceStatus.EXCUSED -> ContextCompat.getColor(context, R.color.status_excused_light)
        }
    }

    fun getStatusText(status: AttendanceStatus): String {
        return when (status) {
            AttendanceStatus.PRESENT -> "Present"
            AttendanceStatus.LATE -> "Late"
            AttendanceStatus.ABSENT -> "Absent"
            AttendanceStatus.EXCUSED -> "Excused"
        }
    }
}