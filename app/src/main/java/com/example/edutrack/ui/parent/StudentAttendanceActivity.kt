package com.example.edutrack.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Attendance
import com.example.edutrack.repository.AttendanceRepository
import com.example.edutrack.repository.StudentRepository
import com.example.edutrack.utils.AttendanceUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var studentId: String
    private lateinit var studentName: String

    private lateinit var tvStudentName: TextView
    private lateinit var tvClassName: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var rvAttendanceHistory: RecyclerView
    private lateinit var btnExcuseStudent: MaterialButton

    private lateinit var historyAdapter: AttendanceHistoryAdapter
    private val attendanceRepository = AttendanceRepository()
    private val studentRepository = StudentRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_attendance)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: ""

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadStudentData()
        loadAttendanceHistory()
    }

    private fun initializeViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        tvClassName = findViewById(R.id.tvClassName)
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        rvAttendanceHistory = findViewById(R.id.rvAttendanceHistory)
        btnExcuseStudent = findViewById(R.id.btnExcuseStudent)

        tvStudentName.text = studentName

        // Set current date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        historyAdapter = AttendanceHistoryAdapter(emptyList())
        rvAttendanceHistory.layoutManager = LinearLayoutManager(this)
        rvAttendanceHistory.adapter = historyAdapter
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnExcuseStudent.setOnClickListener {
            openExcuseLetter()
        }
    }

    private fun loadStudentData() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = studentRepository.getStudent(studentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { student ->
                        tvClassName.text = "Grade ${student.grade}"
                    },
                    onFailure = { }
                )
            }
        }
    }

    private fun loadAttendanceHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = attendanceRepository.getAttendanceByStudent(studentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { attendanceList ->
                        if (attendanceList.isNotEmpty()) {
                            // Show most recent attendance as current status
                            val latest = attendanceList.first()
                            tvCurrentStatus.text = AttendanceUtils.getStatusText(latest.status)
                            val statusColor = AttendanceUtils.getStatusColor(
                                this@StudentAttendanceActivity,
                                latest.status
                            )
                            tvCurrentStatus.setTextColor(statusColor)

                            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            tvCurrentDate.text = dateFormat.format(latest.date.toDate())
                        }

                        historyAdapter.updateAttendance(attendanceList)
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            this@StudentAttendanceActivity,
                            "Failed to load history: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun openExcuseLetter() {
        val intent = Intent(this, ExcuseLetterActivity::class.java)
        intent.putExtra("STUDENT_ID", studentId)
        intent.putExtra("STUDENT_NAME", studentName)
        startActivity(intent)
    }
}