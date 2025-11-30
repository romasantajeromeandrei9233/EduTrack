package com.example.edutrack.ui.parent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.edutrack.R

class ParentDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        findViewById<TextView>(R.id.tvWelcome).text = "Parent Dashboard"
    }
}