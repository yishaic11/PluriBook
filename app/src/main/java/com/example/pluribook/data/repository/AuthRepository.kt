package com.example.pluribook.data.repository

import android.net.Uri
import com.example.pluribook.data.local.UserDao
import com.example.pluribook.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val userDao: UserDao
) {
    fun getFirebaseAuth() = auth

    suspend fun registerUser(uid: String, email: String, username: String, imageUri: Uri): Boolean {
        return try {
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            val profileUpdates = userProfileChangeRequest {
                displayName = username
                photoUri = Uri.parse(downloadUrl)
            }
            auth.currentUser?.updateProfile(profileUpdates)?.await()

            val newUser = User(email = email, username = username, photoUrl = downloadUrl)
            firestore.collection("users").document(uid).set(newUser).await()

            userDao.saveUser(newUser)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncUserToLocalDatabase(uid: String) {
        try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                val user = User(
                    email = document.getString("email") ?: "",
                    username = document.getString("username") ?: "Unknown",
                    photoUrl = document.getString("photoUrl") ?: ""
                )
                userDao.saveUser(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}