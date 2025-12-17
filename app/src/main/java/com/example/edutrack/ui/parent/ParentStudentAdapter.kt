package com.example.edutrack.ui.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Student
// REMOVE THIS IMPORT: import com.google.android.material.button.MaterialButton // Removed

class ParentStudentAdapter(
    private var students: List<Student>,
    private val onStudentClick: (Student) -> Unit
) : RecyclerView.Adapter<ParentStudentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvClassName: TextView = view.findViewById(R.id.tvClassName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        // REMOVE THIS: val btnViewAttendance: MaterialButton = view.findViewById(R.id.btnViewAttendance) // Removed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parent_student, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]

        holder.tvStudentName.text = student.name
        holder.tvClassName.text = student.grade

        // Default status - will be updated with actual attendance data in future
        holder.tvStatus.text = "View Status"
        holder.tvStatus.setBackgroundResource(R.drawable.status_badge_present)

        // REMOVE THIS BLOCK (Redundant with itemView.setOnClickListener):
        /*
        holder.btnViewAttendance.setOnClickListener {
            onStudentClick(student)
        }
        */

        // KEEP THIS: This handles the click for the entire card item
        holder.itemView.setOnClickListener {
            onStudentClick(student)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateStudents(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }
}