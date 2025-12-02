package com.example.edutrack.ui.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Attendance
import com.example.edutrack.model.AttendanceStatus
import com.example.edutrack.utils.AttendanceUtils
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceHistoryAdapter(
    private var attendanceList: List<Attendance>
) : RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendance = attendanceList[position]

        // Format date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(attendance.date.toDate())

        // Set status text
        holder.tvStatus.text = AttendanceUtils.getStatusText(attendance.status).uppercase()

        // Set background based on status
        val backgroundRes = when (attendance.status) {
            AttendanceStatus.PRESENT -> R.drawable.status_badge_present
            AttendanceStatus.LATE -> R.drawable.status_badge_late
            AttendanceStatus.ABSENT -> R.drawable.status_badge_absent
            AttendanceStatus.EXCUSED -> R.drawable.status_badge_excused
        }
        holder.tvStatus.setBackgroundResource(backgroundRes)
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateAttendance(newList: List<Attendance>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}