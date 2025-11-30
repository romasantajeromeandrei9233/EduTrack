package com.example.edutrack.ui.parent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutrack.model.Parent
import com.example.edutrack.model.Student
import com.example.edutrack.repository.StudentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ParentDashboardViewModel : ViewModel() {

    private val studentRepository = StudentRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _parent = MutableLiveData<Parent>()
    val parent: LiveData<Parent> = _parent

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadParentData()
        loadLinkedStudents()
    }

    private fun loadParentData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val doc = db.collection("parents")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val parentData = doc.toObject(Parent::class.java)
                    _parent.value = parentData
                }
            } catch (e: Exception) {
                _error.value = "Failed to load parent data: ${e.message}"
            }
        }
    }

    fun loadLinkedStudents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val result = studentRepository.getStudentsByParent(currentUser.uid)
                    result.fold(
                        onSuccess = { studentList ->
                            _students.value = studentList
                        },
                        onFailure = { exception ->
                            _error.value = "Failed to load students: ${exception.message}"
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

    fun signOut() {
        auth.signOut()
    }
}