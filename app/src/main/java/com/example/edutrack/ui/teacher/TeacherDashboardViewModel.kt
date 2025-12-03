package com.example.edutrack.ui.teacher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.model.ClassRoom
import com.example.edutrack.model.Teacher
import com.example.edutrack.repository.ClassRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TeacherDashboardViewModel : ViewModel() {

    private val classRepository = ClassRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _teacher = MutableLiveData<Teacher>()
    val teacher: LiveData<Teacher> = _teacher

    private val _classes = MutableLiveData<List<ClassRoom>>()
    val classes: LiveData<List<ClassRoom>> = _classes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // FIX: Add Firestore listener for real-time updates
    private var classesListener: ListenerRegistration? = null

    init {
        loadTeacherData()
        setupRealtimeClassListener()
        registerFCMToken()
    }

    private fun loadTeacherData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val doc = db.collection("teachers")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val teacherData = doc.toObject(Teacher::class.java)
                    _teacher.value = teacherData!!
                }
            } catch (e: Exception) {
                _error.value = "Failed to load teacher data: ${e.message}"
            }
        }
    }

    private fun registerFCMToken() {
        viewModelScope.launch {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        saveFCMToken(token)
                    } else {
                        android.util.Log.e("FCM", "Failed to get token", task.exception)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error getting FCM token: ${e.message}")
            }
        }
    }

    private fun saveFCMToken(token: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    db.collection("teachers")
                        .document(currentUser.uid)
                        .update("fcmToken", token)
                        .await()

                    android.util.Log.d("FCM", "Teacher token saved: $token")
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Failed to save token: ${e.message}")
            }
        }
    }

    // FIX: Setup real-time listener for classes
    private fun setupRealtimeClassListener() {
        val currentUser = auth.currentUser ?: return

        classesListener = db.collection("classes")
            .whereEqualTo("teacherId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = "Failed to listen for class updates: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val classList = snapshot.toObjects(ClassRoom::class.java)
                    _classes.value = classList
                    android.util.Log.d("TeacherDashboard", "✅ Classes updated in real-time: ${classList.size} classes")
                }
            }
    }

    fun loadClasses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val result = classRepository.getClassesByTeacher(currentUser.uid)
                    result.fold(
                        onSuccess = { classList ->
                            _classes.value = classList
                        },
                        onFailure = { exception ->
                            _error.value = "Failed to load classes: ${exception.message}"
                        }
                    )
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // FIX: Calculate total student count with real-time data
    fun getTotalStudentCount(): Int {
        return _classes.value?.sumOf { it.studentIds.size } ?: 0
    }

    fun signOut() {
        auth.signOut()
    }

    // FIX: Clean up listener when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        classesListener?.remove()
    }
}