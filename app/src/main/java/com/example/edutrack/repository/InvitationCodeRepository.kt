package com.example.edutrack.repository

import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.InvitationCode
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.random.Random

class InvitationCodeRepository {

    private val db: FirebaseFirestore = FirestoreService.db
    private val codesCollection = db.collection(FirestoreService.INVITATION_CODES_COLLECTION)
    private val studentsCollection = db.collection(FirestoreService.STUDENTS_COLLECTION)
    private val parentsCollection = db.collection(FirestoreService.PARENTS_COLLECTION)

    /**
     * Generate a random 6-character alphanumeric code
     */
    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Create invitation code for a student
     */
    suspend fun createInvitationCode(
        studentId: String,
        studentName: String,
        teacherId: String,
        classId: String
    ): Result<InvitationCode> {
        return try {
            // Generate unique code
            var code = generateRandomCode()
            var isUnique = false
            var attempts = 0

            // Ensure code is unique
            while (!isUnique && attempts < 10) {
                val existingCode = codesCollection
                    .whereEqualTo("code", code)
                    .whereEqualTo("isUsed", false)
                    .get()
                    .await()

                if (existingCode.isEmpty) {
                    isUnique = true
                } else {
                    code = generateRandomCode()
                    attempts++
                }
            }

            if (!isUnique) {
                return Result.failure(Exception("Failed to generate unique code"))
            }

            // Set expiration to 24 hours from now
            val expiresAt = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 24)
            }.time

            val docRef = codesCollection.document()
            val invitationCode = InvitationCode(
                codeId = docRef.id,
                code = code,
                studentId = studentId,
                studentName = studentName,
                teacherId = teacherId,
                classId = classId,
                createdAt = Timestamp.now(),
                expiresAt = Timestamp(expiresAt),
                isUsed = false,
                usedBy = ""
            )

            docRef.set(invitationCode).await()

            Result.success(invitationCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate and retrieve invitation code
     */
    suspend fun validateCode(code: String): Result<InvitationCode> {
        return try {
            val snapshot = codesCollection
                .whereEqualTo("code", code.uppercase())
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Invalid code"))
            }

            val invitationCode = snapshot.documents[0].toObject(InvitationCode::class.java)
                ?: return Result.failure(Exception("Code not found"))

            // Check if already used
            if (invitationCode.isUsed) {
                return Result.failure(Exception("Code already used"))
            }

            // Check if expired
            val now = Timestamp.now()
            if (invitationCode.expiresAt.seconds < now.seconds) {
                return Result.failure(Exception("Code expired"))
            }

            Result.success(invitationCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Use invitation code to link parent and student
     */
    suspend fun useInvitationCode(code: String, parentId: String): Result<String> {
        return try {
            // Validate code first
            val validationResult = validateCode(code)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }

            val invitationCode = validationResult.getOrNull()!!

            // Update student's parentId
            studentsCollection.document(invitationCode.studentId)
                .update("parentId", parentId)
                .await()

            // Update parent's linkedStudentIds
            val parentDoc = parentsCollection.document(parentId).get().await()
            val currentStudentIds = parentDoc.get("linkedStudentIds") as? List<String> ?: emptyList()
            val updatedStudentIds = currentStudentIds.toMutableList()
            if (!updatedStudentIds.contains(invitationCode.studentId)) {
                updatedStudentIds.add(invitationCode.studentId)
            }

            parentsCollection.document(parentId)
                .update("linkedStudentIds", updatedStudentIds)
                .await()

            // Mark code as used
            codesCollection.document(invitationCode.codeId)
                .update(
                    mapOf(
                        "isUsed" to true,
                        "usedBy" to parentId
                    )
                )
                .await()

            Result.success("Student linked successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get invitation codes for a student
     */
    suspend fun getCodesForStudent(studentId: String): Result<List<InvitationCode>> {
        return try {
            val snapshot = codesCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val codes = snapshot.toObjects(InvitationCode::class.java)
            Result.success(codes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete invitation code
     */
    suspend fun deleteCode(codeId: String): Result<Unit> {
        return try {
            codesCollection.document(codeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}