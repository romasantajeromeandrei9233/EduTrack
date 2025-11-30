package com.example.edutrack.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreService {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Collection references
    const val USERS_COLLECTION = "users"
    const val TEACHERS_COLLECTION = "teachers"
    const val PARENTS_COLLECTION = "parents"
    const val STUDENTS_COLLECTION = "students"
    const val CLASSES_COLLECTION = "classes"
    const val ATTENDANCE_COLLECTION = "attendance"
    const val INVITATION_CODES_COLLECTION = "invitationCodes"

    init {
        // Enable offline persistence
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
}