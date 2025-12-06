package com.example.edutrack.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import java.text.SimpleDateFormat
import java.util.*

class ExcuseLetterAdapter(
    private var excuses: List<ExcuseLetter>
) : RecyclerView.Adapter<ExcuseLetterAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvExcuseDate)
        val tvLetter: TextView = view.findViewById(R.id.tvExcuseLetter)
        val tvSubmittedDate: TextView = view.findViewById(R.id.tvSubmittedDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excuse_letter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val excuse = excuses[position]
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        holder.tvDate.text = "Date: ${dateFormat.format(excuse.date.toDate())}"
        holder.tvLetter.text = excuse.letter
        holder.tvSubmittedDate.text = "Submitted: ${dateFormat.format(excuse.createdAt.toDate())}"
    }

    override fun getItemCount(): Int = excuses.size

    fun updateExcuses(newExcuses: List<ExcuseLetter>) {
        excuses = newExcuses
        notifyDataSetChanged()
    }
}