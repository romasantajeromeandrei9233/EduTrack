package com.example.edutrack.ui.parent

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
import com.example.edutrack.model.Student
import com.example.edutrack.repository.InvitationCodeRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentDashboardActivity : AppCompatActivity() {

    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var studentAdapter: ParentStudentAdapter

    private lateinit var tvParentName: TextView
    private lateinit var rvStudents: RecyclerView
    private lateinit var fabLinkChild: FloatingActionButton

    private val invitationCodeRepository = InvitationCodeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        tvParentName = findViewById(R.id.tvParentName)
        rvStudents = findViewById(R.id.rvStudents)
        fabLinkChild = findViewById(R.id.fabLinkChild)
    }

    private fun setupRecyclerView() {
        studentAdapter = ParentStudentAdapter(emptyList()) { student ->
            openStudentDetail(student)
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = studentAdapter
    }

    private fun setupClickListeners() {
        fabLinkChild.setOnClickListener {
            showLinkChildDialog()
        }

        findViewById<android.view.View>(R.id.btnNavSignOut).setOnClickListener {
            signOut()
        }

        findViewById<android.view.View>(R.id.btnNavProfile).setOnClickListener {
            Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.parent.observe(this) { parent ->
            parent?.let {
                tvParentName.text = it.name
            }
        }

        viewModel.students.observe(this) { students ->
            if (students.isEmpty()) {
                Toast.makeText(this, "No linked students. Click + to link a child.", Toast.LENGTH_LONG).show()
            }
            studentAdapter.updateStudents(students)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLinkChildDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_link_child, null)
        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etCode)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLink).setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()
            if (code.isNotBlank()) {
                if (code.length == 6) {
                    linkChild(code)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Code must be 6 characters", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun linkChild(code: String) {
        val parentId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Show loading
        Toast.makeText(this, "Linking child...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            val result = invitationCodeRepository.useInvitationCode(code, parentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { message ->
                        Toast.makeText(
                            this@ParentDashboardActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        // Reload students
                        viewModel.loadLinkedStudents()
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            this@ParentDashboardActivity,
                            "Failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun openStudentDetail(student: Student) {
        val intent = Intent(this, StudentAttendanceActivity::class.java)
        intent.putExtra("STUDENT_ID", student.studentId)
        intent.putExtra("STUDENT_NAME", student.name)
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