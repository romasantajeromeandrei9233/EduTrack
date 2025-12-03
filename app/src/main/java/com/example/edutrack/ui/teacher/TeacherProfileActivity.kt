package com.example.edutrack.ui.teacher

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edutrack.R
import com.example.edutrack.ui.teacher.TeacherProfileActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TeacherProfileActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_profile)

        initializeViews()
        setupClickListeners()
        loadProfileData()
    }

    private fun initializeViews() {
        tvEmail = findViewById(R.id.tvEmail)
        etName = findViewById(R.id.etName)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("teachers").document(userId).get().await()

                withContext(Dispatchers.Main) {
                    tvEmail.text = auth.currentUser?.email
                    etName.setText(doc.getString("name"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TeacherProfileActivity,
                        "Failed to load profile: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val userId = auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("teachers").document(userId).update(
                    mapOf("name" to name)
                ).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TeacherProfileActivity,
                        "Profile updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"
                    Toast.makeText(
                        this@TeacherProfileActivity,
                        "Failed to save: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}