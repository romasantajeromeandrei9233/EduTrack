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

    init {
        loadTeacherData()
        loadClasses()
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
                    _teacher.value = teacherData
                }
            } catch (e: Exception) {
                _error.value = "Failed to load teacher data: ${e.message}"
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

    fun getTotalStudentCount(): Int {
        return _classes.value?.sumOf { it.studentIds.size } ?: 0
    }

    fun signOut() {
        auth.signOut()
    }
}