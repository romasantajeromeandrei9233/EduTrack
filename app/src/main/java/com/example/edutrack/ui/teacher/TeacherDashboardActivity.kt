package com.example.edutrack.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.MainActivity
import com.example.edutrack.R
import com.example.edutrack.model.ClassRoom
import com.example.edutrack.repository.ClassRepository
import com.example.edutrack.utils.SampleDataCreator
import com.example.edutrack.repository.AttendanceRepository
import com.example.edutrack.utils.OfflineAttendanceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherDashboardActivity : AppCompatActivity() {

    private val viewModel: TeacherDashboardViewModel by viewModels()
    private lateinit var classAdapter: ClassAdapter

    private lateinit var tvTeacherName: TextView
    private lateinit var tvClassCount: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var rvClasses: RecyclerView
    private lateinit var fabAddClass: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        tvTeacherName = findViewById(R.id.tvTeacherName)
        tvClassCount = findViewById(R.id.tvClassCount)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        rvClasses = findViewById(R.id.rvClasses)
        fabAddClass = findViewById(R.id.fabAddClass)
    }

    private fun setupRecyclerView() {
        classAdapter = ClassAdapter(emptyList()) { classRoom ->
            openClassDetail(classRoom)
        }
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter = classAdapter
    }

    private fun setupClickListeners() {
        fabAddClass.setOnClickListener {
            showAddClassDialog()
        }

        findViewById<android.view.View>(R.id.btnNavSignOut).setOnClickListener {
            signOut()
        }

        findViewById<android.view.View>(R.id.btnNavProfile).setOnClickListener {
            Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.teacher.observe(this) { teacher ->
            teacher?.let {
                tvTeacherName.text = it.name
            }
        }

        viewModel.classes.observe(this) { classes ->
            classAdapter.updateClasses(classes)
            tvClassCount.text = classes.size.toString()
            tvStudentCount.text = viewModel.getTotalStudentCount().toString()
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Check for pending syncs
        checkPendingSyncs()
    }

    private fun checkPendingSyncs() {
        CoroutineScope(Dispatchers.IO).launch {
            val attendanceRepository = AttendanceRepository()
            val result = attendanceRepository.getUnsyncedCount()

            result.fold(
                onSuccess = { count ->
                    if (count > 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@TeacherDashboardActivity,
                                "$count attendance records pending sync",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Trigger sync if online
                        if (OfflineAttendanceManager.isOnline(this@TeacherDashboardActivity)) {
                            OfflineAttendanceManager.scheduleSyncWork(this@TeacherDashboardActivity)
                        }
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun showAddClassDialog() {
        val options = arrayOf("Create New Class", "Add Sample Data")

        AlertDialog.Builder(this)
            .setTitle("Add Class")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateClassDialog()
                    1 -> addSampleData()
                }
            }
            .show()
    }

    private fun showCreateClassDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Class Name (e.g., Grade 5-A)"

        AlertDialog.Builder(this)
            .setTitle("Create New Class")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val className = input.text.toString()
                if (className.isNotBlank()) {
                    createClass(className)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createClass(className: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            val classRoom = ClassRoom(
                className = className,
                teacherId = teacherId,
                studentIds = emptyList()
            )

            val result = ClassRepository().createClass(classRoom)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@TeacherDashboardActivity, "Class created!", Toast.LENGTH_SHORT).show()
                    viewModel.loadClasses()
                },
                onFailure = {
                    Toast.makeText(this@TeacherDashboardActivity, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun addSampleData() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        SampleDataCreator.createSampleData(teacherId) { success, message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (success) {
                    viewModel.loadClasses()
                }
            }
        }
    }

    private fun openClassDetail(classRoom: ClassRoom) {
        val intent = Intent(this, ClassDetailActivity::class.java)
        intent.putExtra("CLASS_ID", classRoom.classId)
        intent.putExtra("CLASS_NAME", classRoom.className)
        startActivity(intent)
    }

    private fun signOut() {
        viewModel.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}