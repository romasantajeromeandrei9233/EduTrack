package com.example.edutrack.ui.teacher

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edutrack.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ExcuseLetter(
    val letterId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val date: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val letter: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val status: String = "pending"
)

class ExcuseLettersActivity : AppCompatActivity() {

    private lateinit var studentId: String
    private lateinit var studentName: String
    private lateinit var rvExcuseLetters: RecyclerView
    private lateinit var tvNoExcuses: TextView
    private lateinit var excuseAdapter: ExcuseLetterAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excuse_letters)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: ""

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadExcuseLetters()
    }

    private fun initializeViews() {
        findViewById<TextView>(R.id.tvStudentName).text = "Excuse Letters for $studentName"
        rvExcuseLetters = findViewById(R.id.rvExcuseLetters)
        tvNoExcuses = findViewById(R.id.tvNoExcuses)
    }

    private fun setupRecyclerView() {
        excuseAdapter = ExcuseLetterAdapter(emptyList())
        rvExcuseLetters.layoutManager = LinearLayoutManager(this)
        rvExcuseLetters.adapter = excuseAdapter
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadExcuseLetters() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("excuseLetters")
                    .whereEqualTo("studentId", studentId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val excuses = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ExcuseLetter::class.java)?.copy(letterId = doc.id)
                }

                withContext(Dispatchers.Main) {
                    if (excuses.isEmpty()) {
                        rvExcuseLetters.visibility = android.view.View.GONE
                        tvNoExcuses.visibility = android.view.View.VISIBLE
                    } else {
                        rvExcuseLetters.visibility = android.view.View.VISIBLE
                        tvNoExcuses.visibility = android.view.View.GONE
                        excuseAdapter.updateExcuses(excuses)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ExcuseLettersActivity,
                        "Failed to load excuse letters: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}