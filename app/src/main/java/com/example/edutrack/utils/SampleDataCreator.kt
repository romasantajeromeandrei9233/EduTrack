package com.example.edutrack.utils

import com.example.edutrack.model.ClassRoom
import com.example.edutrack.model.Student
import com.example.edutrack.repository.ClassRepository
import com.example.edutrack.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SampleDataCreator {

    fun createSampleData(teacherId: String, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val classRepo = ClassRepository()
                val studentRepo = StudentRepository()

                // Create a sample class
                val sampleClass = ClassRoom(
                    className = "Grade 5-A",
                    teacherId = teacherId,
                    studentIds = emptyList()
                )

                val classResult = classRepo.createClass(sampleClass)
                if (classResult.isFailure) {
                    onComplete(false, "Failed to create class")
                    return@launch
                }

                val classId = classResult.getOrNull() ?: ""

                // Create sample students
                val studentNames = listOf(
                    "John Smith",
                    "Emma Johnson",
                    "Michael Brown",
                    "Sophia Davis",
                    "William Wilson"
                )

                val studentIds = mutableListOf<String>()

                studentNames.forEach { name ->
                    val student = Student(
                        name = name,
                        classId = classId,
                        parentId = "", // Will be linked later
                        grade = "5"
                    )

                    val studentResult = studentRepo.createStudent(student)
                    if (studentResult.isSuccess) {
                        studentIds.add(studentResult.getOrNull() ?: "")
                    }
                }

                // Update class with student IDs
                val updatedClass = sampleClass.copy(
                    classId = classId,
                    studentIds = studentIds
                )
                classRepo.updateClass(updatedClass)

                onComplete(true, "Sample data created successfully!")

            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
}