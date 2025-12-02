package com.example.edutrack.repository

import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.ClassRoom
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClassRepository {

    private val db: FirebaseFirestore = FirestoreService.db
    private val classesCollection = db.collection(FirestoreService.CLASSES_COLLECTION)

    suspend fun createClass(classRoom: ClassRoom): Result<String> {
        return try {
            val docRef = classesCollection.document()
            val classWithId = classRoom.copy(classId = docRef.id)
            docRef.set(classWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClass(classId: String): Result<ClassRoom> {
        return try {
            val snapshot = classesCollection.document(classId).get().await()
            val classRoom = snapshot.toObject(ClassRoom::class.java)
            if (classRoom != null) {
                Result.success(classRoom)
            } else {
                Result.failure(Exception("Class not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClassesByTeacher(teacherId: String): Result<List<ClassRoom>> {
        return try {
            val snapshot = classesCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            val classes = snapshot.toObjects(ClassRoom::class.java)
            Result.success(classes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateClass(classRoom: ClassRoom): Result<Unit> {
        return try {
            classesCollection.document(classRoom.classId).set(classRoom).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStudentToClass(classId: String, studentId: String): Result<Unit> {
        return try {
            val classDoc = classesCollection.document(classId).get().await()
            val classRoom = classDoc.toObject(ClassRoom::class.java)
            if (classRoom != null) {
                val updatedStudentIds = classRoom.studentIds.toMutableList()
                if (!updatedStudentIds.contains(studentId)) {
                    updatedStudentIds.add(studentId)
                    classesCollection.document(classId)
                        .update("studentIds", updatedStudentIds)
                        .await()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Class not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeStudentFromClass(classId: String, studentId: String): Result<Unit> {
        return try {
            val classDoc = classesCollection.document(classId).get().await()
            val classRoom = classDoc.toObject(ClassRoom::class.java)
            if (classRoom != null) {
                val updatedStudentIds = classRoom.studentIds.toMutableList()
                updatedStudentIds.remove(studentId)
                classesCollection.document(classId)
                    .update("studentIds", updatedStudentIds)
                    .await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Class not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteClass(classId: String): Result<Unit> {
        return try {
            classesCollection.document(classId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}