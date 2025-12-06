package com.example.edutrack.ui.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class StudentDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StudentDetailActivity"
    }

    private lateinit var studentId: String
    private lateinit var studentName: String
    private lateinit var classId: String

    private lateinit var tvStudentName: TextView
    private lateinit var tvGrade: TextView
    private lateinit var tvParentStatus: TextView

    // FIX: Added parent detail views
    private lateinit var tvParentName: TextView
    private lateinit var tvParentPhone: TextView
    private lateinit var tvParentAddress: TextView

    private lateinit var tvCodeStatus: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvCodeExpiry: TextView
    private lateinit var btnGenerateCode: MaterialButton
    private lateinit var btnCopyCode: MaterialButton

    private val studentRepository = StudentRepository()
    private val invitationCodeRepository = InvitationCodeRepository()
    private val db = FirebaseFirestore.getInstance()
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentCode: InvitationCode? = null
    private var isLoading = false
    private lateinit var btnViewExcuses: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_student_detail)

            studentId = intent.getStringExtra("STUDENT_ID") ?: run {
                Log.e(TAG, "Missing STUDENT_ID in intent")
                Toast.makeText(this, "Error: Missing student information", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            studentName = intent.getStringExtra("STUDENT_NAME") ?: "Unknown Student"
            classId = intent.getStringExtra("CLASS_ID") ?: ""

            initializeViews()
            setupClickListeners()
            loadStudentData()
            loadInvitationCode()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading student details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            tvStudentName = findViewById(R.id.tvStudentName)
            tvGrade = findViewById(R.id.tvGrade)
            tvParentStatus = findViewById(R.id.tvParentStatus)
            tvParentName = findViewById(R.id.tvParentName)
            tvParentPhone = findViewById(R.id.tvParentPhone)
            tvParentAddress = findViewById(R.id.tvParentAddress)
            tvCodeStatus = findViewById(R.id.tvCodeStatus)
            tvCode = findViewById(R.id.tvCode)
            tvCodeExpiry = findViewById(R.id.tvCodeExpiry)
            btnGenerateCode = findViewById(R.id.btnGenerateCode)
            btnCopyCode = findViewById(R.id.btnCopyCode)
            btnViewExcuses = findViewById(R.id.btnViewExcuses)

            tvStudentName.text = studentName
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }

    private fun setupClickListeners() {
        try {
            findViewById<View>(R.id.btnBack).setOnClickListener {
                finish()
            }

            btnGenerateCode.setOnClickListener {
                if (!isLoading) {
                    generateInvitationCode()
                }
            }

            btnCopyCode.setOnClickListener {
                copyCodeToClipboard()
            }

            btnViewExcuses.setOnClickListener {
                openExcuseLetters()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun loadStudentData() {
        if (isLoading) return
        isLoading = true

        activityScope.launch {
            try {
                val result = studentRepository.getStudent(studentId)

                result.fold(
                    onSuccess = { student ->
                        try {
                            tvGrade.text = "Grade: ${student.grade}"

                            if (student.parentId.isNotBlank()) {
                                tvParentStatus.text = "Parent: Linked ✓"
                                tvParentStatus.setTextColor(getColor(R.color.status_present))

                                // FIX: Load parent details
                                loadParentDetails(student.parentId)
                            } else {
                                tvParentStatus.text = "Parent: Not Linked"
                                tvParentStatus.setTextColor(getColor(R.color.status_absent))
                                hideParentDetails()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating UI with student data: ${e.message}", e)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load student: ${exception.message}", exception)
                        Toast.makeText(
                            this@StudentDetailActivity,
                            "Failed to load student information",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadStudentData: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // FIX: Enhanced parent details loading with better error handling
    private fun loadParentDetails(parentId: String) {
        activityScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val parentDoc = db.collection("parents")
                        .document(parentId)
                        .get()
                        .await()

                    withContext(Dispatchers.Main) {
                        try {
                            if (parentDoc.exists()) {
                                val parentName = parentDoc.getString("name") ?: "N/A"
                                val parentPhone = parentDoc.getString("phoneNumber") ?: "N/A"
                                val parentAddress = parentDoc.getString("address") ?: "N/A"

                                // Show divider and section header
                                findViewById<View>(R.id.dividerParent)?.visibility = View.VISIBLE
                                findViewById<TextView>(R.id.tvParentSectionHeader)?.visibility = View.VISIBLE

                                // Update and show parent details
                                tvParentName.text = "Name: $parentName"
                                tvParentPhone.text = "Phone: $parentPhone"
                                tvParentAddress.text = "Address: $parentAddress"

                                tvParentName.visibility = View.VISIBLE
                                tvParentPhone.visibility = View.VISIBLE
                                tvParentAddress.visibility = View.VISIBLE

                                Log.d(TAG, "✅ Parent details loaded successfully")
                            } else {
                                Log.w(TAG, "Parent document not found: $parentId")
                                hideParentDetails()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating parent UI: ${e.message}", e)
                            hideParentDetails()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading parent details: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    hideParentDetails()
                }
            }
        }
    }

    private fun hideParentDetails() {
        try {
            findViewById<View>(R.id.dividerParent)?.visibility = View.GONE
            findViewById<TextView>(R.id.tvParentSectionHeader)?.visibility = View.GONE
            tvParentName.visibility = View.GONE
            tvParentPhone.visibility = View.GONE
            tvParentAddress.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding parent details: ${e.message}", e)
        }
    }

    private fun loadInvitationCode() {
        activityScope.launch {
            try {
                val result = invitationCodeRepository.getCodesForStudent(studentId)

                result.fold(
                    onSuccess = { codes ->
                        try {
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing invitation codes: ${e.message}", e)
                            showNoCodeState()
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load invitation codes: ${exception.message}", exception)
                        showNoCodeState()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadInvitationCode: ${e.message}", e)
                showNoCodeState()
            }
        }
    }

    private fun generateInvitationCode() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid

        if (teacherId == null) {
            Toast.makeText(this, "Error: Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (isLoading) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        btnGenerateCode.isEnabled = false
        tvCodeStatus.text = "Generating code..."

        activityScope.launch {
            try {
                val result = invitationCodeRepository.createInvitationCode(
                    studentId = studentId,
                    studentName = studentName,
                    teacherId = teacherId,
                    classId = classId
                )

                result.fold(
                    onSuccess = { code ->
                        displayCode(code)
                        // FIX: Updated message to reflect 12 hours
                        Toast.makeText(
                            this@StudentDetailActivity,
                            "Code generated! Valid for 12 hours.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to generate code: ${exception.message}", exception)
                        tvCodeStatus.text = "Failed to generate code"
                        Toast.makeText(
                            this@StudentDetailActivity,
                            "Error: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateInvitationCode: ${e.message}", e)
                tvCodeStatus.text = "Error generating code"
                Toast.makeText(
                    this@StudentDetailActivity,
                    "An error occurred. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
                btnGenerateCode.isEnabled = true
            }
        }
    }

    private fun openExcuseLetters() {
        val intent = Intent(this, ExcuseLettersActivity::class.java)
        intent.putExtra("STUDENT_ID", studentId)
        intent.putExtra("STUDENT_NAME", studentName)
        startActivity(intent)
    }

    private fun displayCode(code: InvitationCode) {
        try {
            currentCode = code

            tvCodeStatus.text = "Active invitation code:"
            tvCode.text = code.code
            tvCode.visibility = View.VISIBLE

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            tvCodeExpiry.text = "Expires: ${dateFormat.format(code.expiresAt.toDate())}"
            tvCodeExpiry.visibility = View.VISIBLE

            btnGenerateCode.text = "Generate New Code"
            btnCopyCode.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying code: ${e.message}", e)
        }
    }

    private fun showNoCodeState() {
        try {
            currentCode = null
            tvCodeStatus.text = "No active code"
            tvCode.visibility = View.GONE
            tvCodeExpiry.visibility = View.GONE
            btnGenerateCode.text = "Generate Invitation Code"
            btnCopyCode.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error showing no code state: ${e.message}", e)
        }
    }

    private fun copyCodeToClipboard() {
        try {
            currentCode?.let { code ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    val clip = ClipData.newPlainText("Invitation Code", code.code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Clipboard not available", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No code to copy", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying code: ${e.message}", e)
            Toast.makeText(this, "Failed to copy code", Toast.LENGTH_SHORT).show()
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

    // FIX: Reload data when returning to this activity
    override fun onResume() {
        super.onResume()
        loadStudentData()
        loadInvitationCode()
    }
}