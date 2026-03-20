package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import com.example.pluribook.TAG
import com.example.pluribook.data.local.UserDao
import com.example.pluribook.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val userDao: UserDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private const val PROFILE_IMAGES_FOLDER = "profile_images"
        const val USERS_COLLECTION = "users"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection(USERS_COLLECTION)

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun login(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user
        if (user != null) {
            fetchAndCacheUser(user.uid)
        }
        return user
    }

    suspend fun signup(email: String, password: String, username: String, imageUri: Uri): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
        return if (firebaseUser != null) {
            val success = registerUser(firebaseUser.uid, email, username, imageUri)
            if (success) firebaseUser else null
        } else null
    }

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

            val user = User(uid = uid, email = email, username = username, photoUrl = downloadUrl)

            usersCollection.document(uid).set(user).await()
            userDao.saveUser(user)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user", e)
            false
        }
    }

    fun getUserStream(uid: String): Flow<User?> = userDao.getUserFlowByUid(uid)

    suspend fun getUserById(uid: String): User? = userDao.getUserByUid(uid)

    fun startRealtimeUserSync(uid: String): ListenerRegistration {
        return usersCollection.document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            snapshot?.toObject(User::class.java)?.let { user ->
                CoroutineScope(Dispatchers.IO).launch { userDao.saveUser(user) }
            }
        }
    }

    suspend fun fetchAndCacheUser(uid: String) {
        try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(User::class.java)?.let { userDao.saveUser(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user $uid", e)
        }
    }

    suspend fun updateUserProfile(uid: String, newUsername: String, newImageUri: Uri?, currentPhotoUrl: String): Boolean {
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

            usersCollection.document(uid).update(mapOf("username" to newUsername, "photoUrl" to photoUrlToSave)).await()

            userDao.getUserByUid(uid)?.let {
                userDao.saveUser(it.copy(username = newUsername, photoUrl = photoUrlToSave))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            false
        }
    }
}