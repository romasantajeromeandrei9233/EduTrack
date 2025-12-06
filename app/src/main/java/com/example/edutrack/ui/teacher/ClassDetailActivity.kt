package com.example.edutrack.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.edutrack.repository.ClassRepository
import com.example.edutrack.repository.StudentRepository
import com.example.edutrack.utils.FCMNotificationSender
import com.example.edutrack.utils.AttendanceUtils
import com.example.edutrack.utils.OfflineAttendanceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ClassDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ClassDetailActivity"
    }

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

    // Use a dedicated scope for this activity
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Prevent multiple simultaneous saves
    private val isSaving = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_class_detail)

            classId = intent.getStringExtra("CLASS_ID") ?: run {
                Log.e(TAG, "Missing CLASS_ID in intent")
                Toast.makeText(this, "Error: Missing class information", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            className = intent.getStringExtra("CLASS_NAME") ?: "Unknown Class"

            initializeViews()
            setupRecyclerView()
            setupClickListeners()
            loadStudents()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing class details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            studentAdapter = StudentAttendanceAdapter(emptyList())
            studentAdapter.onStudentLongClick = { student ->
                openStudentDetail(student)
            }
            rvStudents.layoutManager = LinearLayoutManager(this)
            rvStudents.adapter = studentAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}", e)
        }
    }

    private fun openStudentDetail(student: Student) {
        try {
            val intent = Intent(this, StudentDetailActivity::class.java)
            intent.putExtra("STUDENT_ID", student.studentId)
            intent.putExtra("STUDENT_NAME", student.name)
            intent.putExtra("CLASS_ID", classId)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening student detail: ${e.message}", e)
            Toast.makeText(this, "Failed to open student details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload student data when returning from student detail
        loadStudents()
    }

    private fun setupClickListeners() {
        try {
            findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
                finish()
            }

            btnSaveAttendance.setOnClickListener {
                saveAttendance()
            }

            fabAddStudent.setOnClickListener {
                showAddStudentDialog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun loadStudents() {
        activityScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    studentRepository.getStudentsByClass(classId)
                }

                result.fold(
                    onSuccess = { students ->
                        try {
                            val attendanceItems = students.map { student ->
                                StudentAttendanceItem(student, AttendanceStatus.PRESENT)
                            }
                            studentAdapter.updateStudents(attendanceItems)
                            updateAttendanceStats()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating UI with students: ${e.message}", e)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load students: ${exception.message}", exception)
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed to load students: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadStudents: ${e.message}", e)
            }
        }
    }

    private fun updateAttendanceStats() {
        try {
            val attendanceData = studentAdapter.getAttendanceData()
            val presentCount = attendanceData.count {
                it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE
            }
            val absentCount = attendanceData.count {
                it.status == AttendanceStatus.ABSENT
            }

            tvPresentCount.text = presentCount.toString()
            tvAbsentCount.text = absentCount.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating attendance stats: ${e.message}", e)
        }
    }

    private fun saveAttendance() {
        // Prevent multiple simultaneous saves
        if (!isSaving.compareAndSet(false, true)) {
            Toast.makeText(this, "Please wait, saving in progress...", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherId = FirebaseAuth.getInstance().currentUser?.uid
        if (teacherId == null) {
            isSaving.set(false)
            Toast.makeText(this, "Error: Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val attendanceData = try {
            studentAdapter.getAttendanceData()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting attendance data: ${e.message}", e)
            isSaving.set(false)
            Toast.makeText(this, "Error reading attendance data", Toast.LENGTH_SHORT).show()
            return
        }

        if (attendanceData.isEmpty()) {
            isSaving.set(false)
            Toast.makeText(this, "No students to mark attendance", Toast.LENGTH_SHORT).show()
            return
        }

        // Check online status
        val isOnline = try {
            OfflineAttendanceManager.isOnline(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking online status: ${e.message}", e)
            false
        }

        // Update UI
        runOnUiThread {
            btnSaveAttendance.isEnabled = false
            btnSaveAttendance.text = if (isOnline) "Saving..." else "Saving Offline..."
        }

        activityScope.launch {
            try {
                // Create attendance list with proper error handling
                val attendanceList = attendanceData.mapNotNull { item ->
                    try {
                        Attendance(
                            studentId = item.student.studentId,
                            date = Timestamp.now(),
                            status = item.status,
                            teacherId = teacherId,
                            synced = isOnline
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating attendance for ${item.student.name}: ${e.message}", e)
                        null
                    }
                }

                if (attendanceList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        btnSaveAttendance.isEnabled = true
                        btnSaveAttendance.text = "Save Attendance"
                        isSaving.set(false)
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Error: No valid attendance data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Save to Firestore with timeout
                val result = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(30000) { // 30 second timeout
                        attendanceRepository.markAttendanceBatchOffline(attendanceList, isOnline)
                    } ?: Result.failure(Exception("Save operation timed out"))
                }

                result.fold(
                    onSuccess = {
                        // Send notifications if online (don't block UI)
                        if (isOnline) {
                            launch(Dispatchers.IO) {
                                attendanceData.forEach { item ->
                                    try {
                                        FCMNotificationSender.sendAttendanceNotification(
                                            context = this@ClassDetailActivity,
                                            studentId = item.student.studentId,
                                            studentName = item.student.name,
                                            status = AttendanceUtils.getStatusText(item.status),
                                            date = Date()
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to send notification for ${item.student.name}: ${e.message}", e)
                                        // Don't fail the whole operation if notification fails
                                    }
                                }
                            }
                        } else {
                            // Schedule sync for later
                            try {
                                OfflineAttendanceManager.scheduleSyncWork(this@ClassDetailActivity)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to schedule sync: ${e.message}", e)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            btnSaveAttendance.isEnabled = true
                            btnSaveAttendance.text = "Save Attendance"

                            val message = if (isOnline) {
                                "Attendance saved & notifications sent!"
                            } else {
                                "Attendance saved offline. Will sync when online."
                            }

                            Toast.makeText(
                                this@ClassDetailActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            updateAttendanceStats()
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to save attendance: ${exception.message}", exception)
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
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveAttendance: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    btnSaveAttendance.isEnabled = true
                    btnSaveAttendance.text = "Save Attendance"
                    Toast.makeText(
                        this@ClassDetailActivity,
                        "An error occurred. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isSaving.set(false)
            }
        }
    }

    private fun showAddStudentDialog() {
        try {
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
                    val name = inputName.text.toString().trim()
                    val grade = inputGrade.text.toString().trim()
                    if (name.isNotBlank()) {
                        addStudent(name, grade)
                    } else {
                        Toast.makeText(this, "Student name is required", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing add student dialog: ${e.message}", e)
            Toast.makeText(this, "Error opening dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addStudent(name: String, grade: String) {
        activityScope.launch {
            try {
                val student = Student(
                    name = name,
                    classId = classId,
                    grade = grade,
                    parentId = ""
                )

                val result = withContext(Dispatchers.IO) {
                    studentRepository.createStudent(student)
                }

                result.fold(
                    onSuccess = { studentId ->
                        withContext(Dispatchers.IO) {
                            val classRepo = ClassRepository()
                            classRepo.addStudentToClass(classId, studentId)
                        }

                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Student added!",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadStudents()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to add student: ${exception.message}", exception)
                        Toast.makeText(
                            this@ClassDetailActivity,
                            "Failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in addStudent: ${e.message}", e)
                Toast.makeText(
                    this@ClassDetailActivity,
                    "An error occurred",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activityScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling coroutine scope: ${e.message}", e)
        }
    }
}