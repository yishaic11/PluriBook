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

class UserRepository(
    private val userDao: UserDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private const val PROFILE_IMAGES_FOLDER = "profile_images"
        const val USERS_COLLECTION = "users"
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

            val newUser = User(
                uid = uid,
                email = email,
                username = username,
                photoUrl = downloadUrl,
                likedPosts = emptyList()
            )
            firestore.collection("users").document(uid).set(newUser).await()

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
                val likedPosts = (document.get("likedPosts") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val user = User(
                    uid = uid,
                    email = document.getString("email") ?: "",
                    username = document.getString("username") ?: "Unknown",
                    photoUrl = document.getString("photoUrl") ?: "",
                    likedPosts = likedPosts
                )
                userDao.saveUser(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user with uid $uid to local database")
            e.printStackTrace()
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        val localUser = userDao.getUserByUid(uid)
        if (localUser != null) return localUser

        return try {
            val document = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val remoteUser = document.toObject(User::class.java)
            if (remoteUser != null) {
                userDao.saveUser(remoteUser)
            }
            remoteUser
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(
        uid: String,
        newUsername: String,
        newImageUri: Uri?,
        currentPhotoUrl: String
    ): Boolean {
        return try {
            var photoUrlToSave = currentPhotoUrl
            if (newImageUri != null) {
                val imageRef = storage.reference.child("$PROFILE_IMAGES_FOLDER/$uid.jpg")
                imageRef.putFile(newImageUri).await()
                photoUrlToSave = imageRef.downloadUrl.await().toString()
            }

            val profileUpdates = userProfileChangeRequest {
                displayName = newUsername
                if (newImageUri != null) photoUri = Uri.parse(photoUrlToSave)
            }
            auth.currentUser?.updateProfile(profileUpdates)?.await()

            firestore.collection(USERS_COLLECTION).document(uid)
                .update(
                    mapOf(
                        "username" to newUsername,
                        "photoUrl" to photoUrlToSave
                    )
                ).await()

            val localUser = userDao.getUserByUid(uid)
            if (localUser != null) {
                userDao.saveUser(localUser.copy(username = newUsername, photoUrl = photoUrlToSave))
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile for uid $uid", e)
            false
        }
    }
}