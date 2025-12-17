// File Path: app/src/main/java/com/example/edutrack/ui/teacher/StudentAttendanceAdapter.kt

package com.example.edutrack.ui.teacher

import android.util.Log
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
    private var students: List<StudentAttendanceItem>,
    var onStudentLongClick: ((Student) -> Unit)? = null
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "StudentAttendanceAdapter"
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
        val chipGroup: ChipGroup = view.findViewById(R.id.chipGroup)
        val chipPresent: Chip = view.findViewById(R.id.chipPresent)
        val chipLate: Chip = view.findViewById(R.id.chipLate)
        val chipAbsent: Chip = view.findViewById(R.id.chipAbsent)
        val chipExcused: Chip = view.findViewById(R.id.chipExcused)

        // ✅ FIX: Track if we're programmatically updating to prevent listener firing
        var isUpdatingProgrammatically = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            // Validate position
            if (position < 0 || position >= students.size) {
                Log.e(TAG, "Invalid position: $position, size: ${students.size}")
                return
            }

            val item = students[position]

            // Set student name
            holder.tvStudentName.text = item.student.name

            // ✅ FIX: Set flag BEFORE updating chips
            holder.isUpdatingProgrammatically = true

            // Update status indicator color
            val statusColor = when (item.status) {
                AttendanceStatus.PRESENT -> R.color.status_present
                AttendanceStatus.LATE -> R.color.status_late
                AttendanceStatus.ABSENT -> R.color.status_absent
                AttendanceStatus.EXCUSED -> R.color.status_excused
            }
            holder.statusIndicator.setBackgroundResource(statusColor)

            // ✅ FIX: Remove listener BEFORE setting chip states
            holder.chipGroup.setOnCheckedStateChangeListener(null)

            // ✅ FIX: Clear all selections first
            holder.chipGroup.clearCheck()

            // ✅ FIX: Set the correct chip based on current status
            when (item.status) {
                AttendanceStatus.PRESENT -> holder.chipPresent.isChecked = true
                AttendanceStatus.LATE -> holder.chipLate.isChecked = true
                AttendanceStatus.ABSENT -> holder.chipAbsent.isChecked = true
                AttendanceStatus.EXCUSED -> holder.chipExcused.isChecked = true
            }

            // ✅ FIX: Small delay to ensure UI is updated, then reset flag
            holder.itemView.post {
                holder.isUpdatingProgrammatically = false
            }

            // ✅ FIX: Set listener AFTER chip states are set
            holder.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
                try {
                    // ✅ FIX: Ignore listener if we're updating programmatically
                    if (holder.isUpdatingProgrammatically) {
                        return@setOnCheckedStateChangeListener
                    }

                    // Validate adapter position
                    val adapterPosition = holder.adapterPosition
                    if (adapterPosition == RecyclerView.NO_POSITION ||
                        adapterPosition >= students.size) {
                        Log.w(TAG, "Invalid adapter position during chip change: $adapterPosition")
                        return@setOnCheckedStateChangeListener
                    }

                    if (checkedIds.isNotEmpty()) {
                        val newStatus = when (checkedIds[0]) {
                            R.id.chipPresent -> AttendanceStatus.PRESENT
                            R.id.chipLate -> AttendanceStatus.LATE
                            R.id.chipAbsent -> AttendanceStatus.ABSENT
                            R.id.chipExcused -> AttendanceStatus.EXCUSED
                            else -> {
                                Log.w(TAG, "Unknown chip ID: ${checkedIds[0]}")
                                return@setOnCheckedStateChangeListener
                            }
                        }

                        // Update the status in the data
                        students[adapterPosition].status = newStatus

                        // Update the status indicator color
                        val newStatusColor = when (newStatus) {
                            AttendanceStatus.PRESENT -> R.color.status_present
                            AttendanceStatus.LATE -> R.color.status_late
                            AttendanceStatus.ABSENT -> R.color.status_absent
                            AttendanceStatus.EXCUSED -> R.color.status_excused
                        }
                        holder.statusIndicator.setBackgroundResource(newStatusColor)

                        Log.d(TAG, "✅ Status updated for ${students[adapterPosition].student.name}: $newStatus")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling chip selection: ${e.message}", e)
                }
            }

            // Long-click to open student detail
            holder.itemView.setOnLongClickListener {
                try {
                    val adapterPosition = holder.adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION &&
                        adapterPosition < students.size) {
                        onStudentLongClick?.invoke(students[adapterPosition].student)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling long click: ${e.message}", e)
                }
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error binding view holder at position $position: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateStudents(newStudents: List<StudentAttendanceItem>) {
        try {
            students = newStudents.toList() // Create a copy to prevent external modifications
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating students: ${e.message}", e)
        }
    }

    fun getAttendanceData(): List<StudentAttendanceItem> {
        return try {
            students.toList() // Return a copy to prevent external modifications
        } catch (e: Exception) {
            Log.e(TAG, "Error getting attendance data: ${e.message}", e)
            emptyList()
        }
    }

    override fun getItemId(position: Int): Long {
        return try {
            if (position >= 0 && position < students.size) {
                students[position].student.studentId.hashCode().toLong()
            } else {
                RecyclerView.NO_ID
            }
        } catch (e: Exception) {
            RecyclerView.NO_ID
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}