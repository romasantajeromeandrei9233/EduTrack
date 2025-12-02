package com.example.edutrack.ui.parent

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edutrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExcuseLetterActivity : AppCompatActivity() {

    private lateinit var studentId: String
    private lateinit var studentName: String

    private lateinit var tvStudentName: TextView
    private lateinit var etDate: TextInputEditText
    private lateinit var etExcuseLetter: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excuse_letter)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: ""

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        etDate = findViewById(R.id.etDate)
        etExcuseLetter = findViewById(R.id.etExcuseLetter)
        btnSubmit = findViewById(R.id.btnSubmit)

        tvStudentName.text = "For: $studentName"

        // Set default date to today
        updateDateField()
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etDate.setOnClickListener {
            showDatePicker()
        }

        btnSubmit.setOnClickListener {
            submitExcuse()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateDateField()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateField() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))
    }

    private fun submitExcuse() {
        val excuseLetter = etExcuseLetter.text.toString()

        if (excuseLetter.isBlank()) {
            Toast.makeText(this, "Please write an excuse letter", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: In M5, this will send a notification to the teacher
        // For now, just show success message
        Toast.makeText(
            this,
            "Excuse letter submitted! (Feature will be fully implemented in M5)",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}