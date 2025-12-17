package com.example.edutrack.ui.parent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Attendance
import com.example.edutrack.repository.StudentRepository
import com.example.edutrack.utils.AttendanceUtils
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAttendanceActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StudentAttendance"
    }

    private lateinit var studentId: String
    private lateinit var studentName: String

    private lateinit var tvStudentName: TextView
    private lateinit var tvClassName: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var rvAttendanceHistory: RecyclerView
    private lateinit var historyAdapter: AttendanceHistoryAdapter
    private val studentRepository = StudentRepository()
    private val db = FirebaseFirestore.getInstance()

    // Real-time listener
    private var attendanceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_attendance)

        studentId = intent.getStringExtra("STUDENT_ID") ?: run {
            Toast.makeText(this, "Error: Missing student information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        studentName = intent.getStringExtra("STUDENT_NAME") ?: "Unknown Student"

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadStudentData()
        setupRealtimeAttendanceListener()
    }

    private fun initializeViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        tvClassName = findViewById(R.id.tvClassName)
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        rvAttendanceHistory = findViewById(R.id.rvAttendanceHistory)

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
    }

    private fun loadStudentData() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = studentRepository.getStudent(studentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { student ->
                        tvClassName.text = "Grade ${student.grade}"
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to load student data")
                    }
                )
            }
        }
    }

    /**
     * Setup real-time listener for attendance changes
     */
    private fun setupRealtimeAttendanceListener() {
        Log.d(TAG, "Setting up real-time listener for student: $studentId")

        attendanceListener = db.collection("attendance")
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        // Convert to Attendance objects and sort by date descending
                        val attendanceList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Attendance::class.java)
                        }.sortedByDescending { it.date.seconds }

                        Log.d(TAG, "✅ Real-time update: ${attendanceList.size} attendance records")

                        // Update UI with latest attendance
                        if (attendanceList.isNotEmpty()) {
                            val latest = attendanceList.first()
                            tvCurrentStatus.text = AttendanceUtils.getStatusText(latest.status)
                            val statusColor = AttendanceUtils.getStatusColor(
                                this@StudentAttendanceActivity,
                                latest.status
                            )
                            tvCurrentStatus.setTextColor(statusColor)

                            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            tvCurrentDate.text = dateFormat.format(latest.date.toDate())

                            // Update history adapter
                            historyAdapter.updateAttendance(attendanceList)
                        } else {
                            tvCurrentStatus.text = "No Records"
                            tvCurrentStatus.setTextColor(getColor(R.color.text_secondary))
                            historyAdapter.updateAttendance(emptyList())
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing attendance data: ${e.message}", e)
                    }
                } else {
                    Log.d(TAG, "No attendance records found")
                    tvCurrentStatus.text = "No Records"
                    tvCurrentStatus.setTextColor(getColor(R.color.text_secondary))
                    historyAdapter.updateAttendance(emptyList())
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener when activity is destroyed
        attendanceListener?.remove()
        Log.d(TAG, "Real-time listener removed")
    }
}