package com.example.edutrack.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.example.edutrack.model.Attendance
import com.example.edutrack.model.AttendanceStatus
import com.example.edutrack.model.Student
import com.example.edutrack.repository.AttendanceRepository
import com.example.edutrack.repository.StudentRepository
import com.example.edutrack.utils.FCMNotificationSender
import com.example.edutrack.utils.AttendanceUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var classId: String
    private lateinit var className: String

    private lateinit var tvClassName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsentCount: TextView
    private lateinit var rvStudents: RecyclerView
    private lateinit var btnSaveAttendance: MaterialButton
    private lateinit var fabAddStudent: FloatingActionButton

    private lateinit var studentAdapter: StudentAttendanceAdapter
    private val studentRepository = StudentRepository()
    private val attendanceRepository = AttendanceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)

        classId = intent.getStringExtra("CLASS_ID") ?: ""
        className = intent.getStringExtra("CLASS_NAME") ?: ""

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadStudents()
    }

    private fun initializeViews() {
        tvClassName = findViewById(R.id.tvClassName)
        tvDate = findViewById(R.id.tvDate)
        tvPresentCount = findViewById(R.id.tvPresentCount)
        tvAbsentCount = findViewById(R.id.tvAbsentCount)
        rvStudents = findViewById(R.id.rvStudents)
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance)
        fabAddStudent = findViewById(R.id.fabAddStudent)

        tvClassName.text = className

        // Set current date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        tvDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAttendanceAdapter(emptyList())
        studentAdapter.onStudentLongClick = { student ->
            openStudentDetail(student)
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = studentAdapter
    }

    private fun openStudentDetail(student: Student) {
        val intent = Intent(this, StudentDetailActivity::class.java)
        intent.putExtra("STUDENT_ID", student.studentId)
        intent.putExtra("STUDENT_NAME", student.name)
        intent.putExtra("CLASS_ID", classId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload student data when returning from student detail
        loadStudents()
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnSaveAttendance.setOnClickListener {
            saveAttendance()
        }

        fabAddStudent.setOnClickListener {
            showAddStudentDialog()
        }
    }

    private fun loadStudents() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = studentRepository.getStudentsByClass(classId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { students ->
                        val attendanceItems = students.map { student ->
                            StudentAttendanceItem(student, AttendanceStatus.PRESENT)
                        }
                        studentAdapter.updateStudents(attendanceItems)
                        updateAttendanceStats()
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed to load students: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun updateAttendanceStats() {
        val attendanceData = studentAdapter.getAttendanceData()
        val presentCount = attendanceData.count {
            it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE
        }
        val absentCount = attendanceData.count {
            it.status == AttendanceStatus.ABSENT
        }

        tvPresentCount.text = presentCount.toString()
        tvAbsentCount.text = absentCount.toString()
    }

    private fun saveAttendance() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val attendanceData = studentAdapter.getAttendanceData()

        if (attendanceData.isEmpty()) {
            Toast.makeText(this, "No students to mark attendance", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        btnSaveAttendance.isEnabled = false
        btnSaveAttendance.text = "Saving..."

        CoroutineScope(Dispatchers.IO).launch {
            val attendanceList = attendanceData.map { item ->
                Attendance(
                    studentId = item.student.studentId,
                    date = Timestamp.now(),
                    status = item.status,
                    teacherId = teacherId,
                    synced = true
                )
            }

            val result = attendanceRepository.markAttendanceBatch(attendanceList)

            result.fold(
                onSuccess = {
                    // Send notifications to parents
                    attendanceData.forEach { item ->
                        CoroutineScope(Dispatchers.IO).launch {
                            FCMNotificationSender.sendAttendanceNotification(
                                studentId = item.student.studentId,
                                studentName = item.student.name,
                                status = AttendanceUtils.getStatusText(item.status),
                                date = Date()
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        btnSaveAttendance.isEnabled = true
                        btnSaveAttendance.text = "Save Attendance"

                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Attendance saved & notifications sent!",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateAttendanceStats()
                    }
                },
                onFailure = { exception ->
                    withContext(Dispatchers.Main) {
                        btnSaveAttendance.isEnabled = true
                        btnSaveAttendance.text = "Save Attendance"

                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed to save: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
    }

    private fun showAddStudentDialog() {
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = android.widget.EditText(this)
        inputName.hint = "Student Name"
        layout.addView(inputName)

        val inputGrade = android.widget.EditText(this)
        inputGrade.hint = "Grade"
        layout.addView(inputGrade)

        AlertDialog.Builder(this)
            .setTitle("Add Student")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString()
                val grade = inputGrade.text.toString()
                if (name.isNotBlank()) {
                    addStudent(name, grade)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addStudent(name: String, grade: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val student = Student(
                name = name,
                classId = classId,
                grade = grade,
                parentId = ""
            )

            val result = studentRepository.createStudent(student)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Student added!",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadStudents()
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }
}