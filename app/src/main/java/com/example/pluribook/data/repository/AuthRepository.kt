package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import com.example.pluribook.TAG
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

    companion object {
        private const val PROFILE_IMAGES_FOLDER = "profile_images"
        private const val USERS_COLLECTION = "users"
    }

    fun getFirebaseAuth() = auth

    suspend fun registerUser(uid: String, email: String, username: String, imageUri: Uri): Boolean {
        return try {
            val imageRef = storage.reference.child("$PROFILE_IMAGES_FOLDER/$uid.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            val profileUpdates = userProfileChangeRequest {
                displayName = username
                photoUri = Uri.parse(downloadUrl)
            }
            auth.currentUser?.updateProfile(profileUpdates)?.await()

            val newUser = User(email = email, username = username, photoUrl = downloadUrl, uid = uid)
            firestore.collection(USERS_COLLECTION).document(uid).set(newUser).await()

            userDao.saveUser(newUser)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user with uid $uid")
            e.printStackTrace()
            false
        }
    }

    suspend fun syncUserToLocalDatabase(uid: String) {
        try {
            val document = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (document.exists()) {
                val user = User(
                    email = document.getString("email") ?: "",
                    username = document.getString("username") ?: "Unknown",
                    photoUrl = document.getString("photoUrl") ?: "",
                    uid = uid
                )
                userDao.saveUser(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user with uid $uid to local database")
            e.printStackTrace()
        }
    }
}