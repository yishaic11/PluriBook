package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import com.example.pluribook.TAG
import com.example.pluribook.data.local.PostDao
import com.example.pluribook.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val postDao: PostDao
) {

    companion object {
        private const val POST_IMAGES_FOLDER = "post_images"
        private const val POSTS_COLLECTION = "posts"
    }

    suspend fun createPost(imageUri: Uri, description: String): Boolean {
        return try {
            val postId = UUID.randomUUID().toString()
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

            val storageRef = storage.reference.child("$POST_IMAGES_FOLDER/$postId.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val newPost = Post(
                id = postId,
                photoUrl = downloadUrl,
                description = description,
                senderId = userId
            )

            firestore.collection(POSTS_COLLECTION).document(postId).set(newPost).await()
            postDao.insertPost(newPost)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post")
            e.printStackTrace()
            false
        }
    }

    suspend fun getPostsBySender() {}

    suspend fun updatePost() {}

    suspend fun deletePost(postId: String): Boolean {
        return try {
            try {
                storage.reference.child("$POST_IMAGES_FOLDER/$postId.jpg").delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Image not found or already deleted: ${e.message}")
            }

            firestore.collection(POSTS_COLLECTION).document(postId).delete().await()

            postDao.deletePost(postId)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post: ${e.message}", e)
            false
        }
    }
}