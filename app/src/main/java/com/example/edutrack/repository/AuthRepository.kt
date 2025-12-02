package com.example.edutrack.repository

import com.example.edutrack.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Store user data in Firestore
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to email,
                "name" to name,
                "role" to role.name
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            // Create role-specific document
            when (role) {
                UserRole.TEACHER -> {
                    firestore.collection("teachers").document(user.uid).set(
                        hashMapOf(
                            "teacherId" to user.uid,
                            "name" to name,
                            "email" to email,
                            "classList" to emptyList<String>()
                        )
                    ).await()
                }
                UserRole.PARENT -> {
                    firestore.collection("parents").document(user.uid).set(
                        hashMapOf(
                            "parentId" to user.uid,
                            "name" to name,
                            "email" to email,
                            "phoneNumber" to "",
                            "address" to "",
                            "linkedStudentIds" to emptyList<String>(),
                            "fcmToken" to ""
                        )
                    ).await()
                }
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(uid: String): Result<UserRole> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val roleString = document.getString("role") ?: throw Exception("Role not found")
            val role = UserRole.valueOf(roleString)
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}