package com.example.edutrack.ui.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Student
import com.google.android.material.button.MaterialButton

class ParentStudentAdapter(
    private var students: List<Student>,
    private val onStudentClick: (Student) -> Unit
) : RecyclerView.Adapter<ParentStudentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvClassName: TextView = view.findViewById(R.id.tvClassName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnViewAttendance: MaterialButton = view.findViewById(R.id.btnViewAttendance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parent_student, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]

        holder.tvStudentName.text = student.name
        holder.tvClassName.text = "Grade ${student.grade}"
        holder.tvStatus.text = "View Status"

        holder.btnViewAttendance.setOnClickListener {
            onStudentClick(student)
        }

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