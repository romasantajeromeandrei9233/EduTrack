package com.example.edutrack.repository

import android.util.Log
import com.example.edutrack.firebase.FirestoreService
import com.example.edutrack.model.InvitationCode
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.random.Random

class InvitationCodeRepository {

    companion object {
        private const val TAG = "InvitationCodeRepo"
    }

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
     * FIX: Changed expiration to 12 hours instead of 24
     */
    suspend fun createInvitationCode(
        studentId: String,
        studentName: String,
        teacherId: String,
        classId: String
    ): Result<InvitationCode> {
        return try {
            Log.d(TAG, "Creating invitation code for student: $studentName")

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
                Log.e(TAG, "Failed to generate unique code after 10 attempts")
                return Result.failure(Exception("Failed to generate unique code"))
            }

            // FIX: Set expiration to 12 hours from now (was 24)
            val expiresAt = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 12)
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

            Log.d(TAG, "✅ Invitation code created: $code (expires in 12 hours)")
            Result.success(invitationCode)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create invitation code: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Validate and retrieve invitation code
     */
    suspend fun validateCode(code: String): Result<InvitationCode> {
        return try {
            Log.d(TAG, "Validating code: $code")

            val snapshot = codesCollection
                .whereEqualTo("code", code.uppercase())
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "Invalid code: $code")
                return Result.failure(Exception("Invalid code"))
            }

            val invitationCode = snapshot.documents[0].toObject(InvitationCode::class.java)
                ?: return Result.failure(Exception("Code not found"))

            // Check if already used
            if (invitationCode.isUsed) {
                Log.w(TAG, "Code already used: $code")
                return Result.failure(Exception("Code already used"))
            }

            // Check if expired
            val now = Timestamp.now()
            if (invitationCode.expiresAt.seconds < now.seconds) {
                Log.w(TAG, "Code expired: $code")
                return Result.failure(Exception("Code expired"))
            }

            Log.d(TAG, "✅ Code validated successfully: $code")
            Result.success(invitationCode)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Code validation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Use invitation code to link parent and student
     * FIX: Improved transaction handling to prevent permission errors
     */
    suspend fun useInvitationCode(code: String, parentId: String): Result<String> {
        return try {
            Log.d(TAG, "Attempting to use code: $code for parent: $parentId")

            // Validate code first
            val validationResult = validateCode(code)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull()!!)
            }

            val invitationCode = validationResult.getOrNull()!!

            // FIX: Use Firestore transaction for atomic operations
            db.runTransaction { transaction ->
                // 1. Get student document reference
                val studentRef = studentsCollection.document(invitationCode.studentId)
                val studentSnapshot = transaction.get(studentRef)

                if (!studentSnapshot.exists()) {
                    throw Exception("Student not found")
                }

                // 2. Get parent document reference
                val parentRef = parentsCollection.document(parentId)
                val parentSnapshot = transaction.get(parentRef)

                if (!parentSnapshot.exists()) {
                    throw Exception("Parent not found")
                }

                // 3. Update student's parentId
                transaction.update(studentRef, "parentId", parentId)
                Log.d(TAG, "Updated student parentId")

                // 4. Update parent's linkedStudentIds
                val currentStudentIds = parentSnapshot.get("linkedStudentIds") as? List<String> ?: emptyList()
                val updatedStudentIds = currentStudentIds.toMutableList()

                if (!updatedStudentIds.contains(invitationCode.studentId)) {
                    updatedStudentIds.add(invitationCode.studentId)
                }

                transaction.update(parentRef, "linkedStudentIds", updatedStudentIds)
                Log.d(TAG, "Updated parent linkedStudentIds")

                // 5. Mark code as used
                val codeRef = codesCollection.document(invitationCode.codeId)
                transaction.update(codeRef, mapOf(
                    "isUsed" to true,
                    "usedBy" to parentId
                ))
                Log.d(TAG, "Marked code as used")

                null
            }.await()

            Log.d(TAG, "✅ Student linked successfully!")
            Result.success("Student linked successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to link student: ${e.message}", e)
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
            Log.d(TAG, "Retrieved ${codes.size} codes for student: $studentId")
            Result.success(codes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get codes for student: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete invitation code
     */
    suspend fun deleteCode(codeId: String): Result<Unit> {
        return try {
            codesCollection.document(codeId).delete().await()
            Log.d(TAG, "Deleted code: $codeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete code: ${e.message}", e)
            Result.failure(e)
        }
    }
}