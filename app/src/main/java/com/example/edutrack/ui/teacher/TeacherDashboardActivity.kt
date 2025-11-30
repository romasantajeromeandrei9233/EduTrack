package com.example.edutrack.ui.teacher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.edutrack.R

class TeacherDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        findViewById<TextView>(R.id.tvWelcome).text = "Teacher Dashboard"
    }
}