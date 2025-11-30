package com.example.edutrack.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.ClassRoom
import com.google.android.material.button.MaterialButton

class ClassAdapter(
    private var classes: List<ClassRoom>,
    private val onClassClick: (ClassRoom) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClassName: TextView = view.findViewById(R.id.tvClassName)
        val tvStudentCount: TextView = view.findViewById(R.id.tvStudentCount)
        val btnViewClass: MaterialButton = view.findViewById(R.id.btnViewClass)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classRoom = classes[position]

        holder.tvClassName.text = classRoom.className
        holder.tvStudentCount.text = "${classRoom.studentIds.size} Students"

        holder.btnViewClass.setOnClickListener {
            onClassClick(classRoom)
        }

        holder.itemView.setOnClickListener {
            onClassClick(classRoom)
        }
    }

    override fun getItemCount(): Int = classes.size

    fun updateClasses(newClasses: List<ClassRoom>) {
        classes = newClasses
        notifyDataSetChanged()
    }
}