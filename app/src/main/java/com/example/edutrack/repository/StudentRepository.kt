package com.example.edutrack.repository

import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.Student
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudentRepository {

    private val db: FirebaseFirestore = FirestoreService.db
    private val studentsCollection = db.collection(FirestoreService.STUDENTS_COLLECTION)

    suspend fun createStudent(student: Student): Result<String> {
        return try {
            val docRef = studentsCollection.document()
            val studentWithId = student.copy(studentId = docRef.id)
            docRef.set(studentWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudent(studentId: String): Result<Student> {
        return try {
            val snapshot = studentsCollection.document(studentId).get().await()
            val student = snapshot.toObject(Student::class.java)
            if (student != null) {
                Result.success(student)
            } else {
                Result.failure(Exception("Student not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentsByClass(classId: String): Result<List<Student>> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("classId", classId)
                .get()
                .await()
            val students = snapshot.toObjects(Student::class.java)
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentsByParent(parentId: String): Result<List<Student>> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("parentId", parentId)
                .get()
                .await()
            val students = snapshot.toObjects(Student::class.java)
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStudent(student: Student): Result<Unit> {
        return try {
            studentsCollection.document(student.studentId).set(student).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(studentId: String): Result<Unit> {
        return try {
            studentsCollection.document(studentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}