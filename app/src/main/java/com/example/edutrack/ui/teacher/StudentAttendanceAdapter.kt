package com.example.edutrack.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.AttendanceStatus
import com.example.edutrack.model.Student
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

data class StudentAttendanceItem(
    val student: Student,
    var status: AttendanceStatus = AttendanceStatus.PRESENT
)

class StudentAttendanceAdapter(
    private var students: List<StudentAttendanceItem>
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
        val chipGroup: ChipGroup = view.findViewById(R.id.chipGroup)
        val chipPresent: Chip = view.findViewById(R.id.chipPresent)
        val chipLate: Chip = view.findViewById(R.id.chipLate)
        val chipAbsent: Chip = view.findViewById(R.id.chipAbsent)
        val chipExcused: Chip = view.findViewById(R.id.chipExcused)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = students[position]

        holder.tvStudentName.text = item.student.name

        // Update status indicator color
        val statusColor = when (item.status) {
            AttendanceStatus.PRESENT -> R.color.status_present
            AttendanceStatus.LATE -> R.color.status_late
            AttendanceStatus.ABSENT -> R.color.status_absent
            AttendanceStatus.EXCUSED -> R.color.status_excused
        }
        holder.statusIndicator.setBackgroundResource(statusColor)

        // Set selected chip based on status
        when (item.status) {
            AttendanceStatus.PRESENT -> holder.chipPresent.isChecked = true
            AttendanceStatus.LATE -> holder.chipLate.isChecked = true
            AttendanceStatus.ABSENT -> holder.chipAbsent.isChecked = true
            AttendanceStatus.EXCUSED -> holder.chipExcused.isChecked = true
        }

        // Setup chip listeners
        holder.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val newStatus = when (checkedIds[0]) {
                    R.id.chipPresent -> AttendanceStatus.PRESENT
                    R.id.chipLate -> AttendanceStatus.LATE
                    R.id.chipAbsent -> AttendanceStatus.ABSENT
                    R.id.chipExcused -> AttendanceStatus.EXCUSED
                    else -> AttendanceStatus.PRESENT
                }
                item.status = newStatus
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateStudents(newStudents: List<StudentAttendanceItem>) {
        students = newStudents
        notifyDataSetChanged()
    }

    fun getAttendanceData(): List<StudentAttendanceItem> = students
}