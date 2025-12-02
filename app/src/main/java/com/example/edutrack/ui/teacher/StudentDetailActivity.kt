package com.example.edutrack.ui.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edutrack.R
import com.example.edutrack.model.InvitationCode
import com.example.edutrack.repository.InvitationCodeRepository
import com.example.edutrack.repository.StudentRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class StudentDetailActivity : AppCompatActivity() {

    private lateinit var studentId: String
    private lateinit var studentName: String
    private lateinit var classId: String

    private lateinit var tvStudentName: TextView
    private lateinit var tvGrade: TextView
    private lateinit var tvParentStatus: TextView
    private lateinit var tvCodeStatus: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvCodeExpiry: TextView
    private lateinit var btnGenerateCode: MaterialButton
    private lateinit var btnCopyCode: MaterialButton

    private val studentRepository = StudentRepository()
    private val invitationCodeRepository = InvitationCodeRepository()

    private var currentCode: InvitationCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: ""
        classId = intent.getStringExtra("CLASS_ID") ?: ""

        initializeViews()
        setupClickListeners()
        loadStudentData()
        loadInvitationCode()
    }

    private fun initializeViews() {
        tvStudentName = findViewById(R.id.tvStudentName)
        tvGrade = findViewById(R.id.tvGrade)
        tvParentStatus = findViewById(R.id.tvParentStatus)
        tvCodeStatus = findViewById(R.id.tvCodeStatus)
        tvCode = findViewById(R.id.tvCode)
        tvCodeExpiry = findViewById(R.id.tvCodeExpiry)
        btnGenerateCode = findViewById(R.id.btnGenerateCode)
        btnCopyCode = findViewById(R.id.btnCopyCode)

        tvStudentName.text = studentName
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnGenerateCode.setOnClickListener {
            generateInvitationCode()
        }

        btnCopyCode.setOnClickListener {
            copyCodeToClipboard()
        }
    }

    private fun loadStudentData() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = studentRepository.getStudent(studentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { student ->
                        tvGrade.text = "Grade: ${student.grade}"

                        if (student.parentId.isNotBlank()) {
                            tvParentStatus.text = "Parent: Linked"
                            tvParentStatus.setTextColor(getColor(R.color.status_present))
                        } else {
                            tvParentStatus.text = "Parent: Not Linked"
                            tvParentStatus.setTextColor(getColor(R.color.status_absent))
                        }
                    },
                    onFailure = { }
                )
            }
        }
    }

    private fun loadInvitationCode() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = invitationCodeRepository.getCodesForStudent(studentId)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { codes ->
                        // Find active (unused, not expired) code
                        val now = System.currentTimeMillis() / 1000
                        val activeCode = codes.firstOrNull { code ->
                            !code.isUsed && code.expiresAt.seconds > now
                        }

                        if (activeCode != null) {
                            displayCode(activeCode)
                        } else {
                            showNoCodeState()
                        }
                    },
                    onFailure = {
                        showNoCodeState()
                    }
                )
            }
        }
    }

    private fun generateInvitationCode() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        btnGenerateCode.isEnabled = false
        tvCodeStatus.text = "Generating code..."

        CoroutineScope(Dispatchers.IO).launch {
            val result = invitationCodeRepository.createInvitationCode(
                studentId = studentId,
                studentName = studentName,
                teacherId = teacherId,
                classId = classId
            )

            withContext(Dispatchers.Main) {
                btnGenerateCode.isEnabled = true

                result.fold(
                    onSuccess = { code ->
                        displayCode(code)
                        Toast.makeText(
                            this@StudentDetailActivity,
                            "Code generated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { exception ->
                        tvCodeStatus.text = "Failed to generate code"
                        Toast.makeText(
                            this@StudentDetailActivity,
                            "Error: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun displayCode(code: InvitationCode) {
        currentCode = code

        tvCodeStatus.text = "Active invitation code:"
        tvCode.text = code.code
        tvCode.visibility = View.VISIBLE

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        tvCodeExpiry.text = "Expires: ${dateFormat.format(code.expiresAt.toDate())}"
        tvCodeExpiry.visibility = View.VISIBLE

        btnGenerateCode.text = "Generate New Code"
        btnCopyCode.visibility = View.VISIBLE
    }

    private fun showNoCodeState() {
        tvCodeStatus.text = "No active code"
        tvCode.visibility = View.GONE
        tvCodeExpiry.visibility = View.GONE
        btnGenerateCode.text = "Generate Invitation Code"
        btnCopyCode.visibility = View.GONE
    }

    private fun copyCodeToClipboard() {
        currentCode?.let { code ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Invitation Code", code.code)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }
}