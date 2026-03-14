package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import com.example.pluribook.TAG
import com.example.pluribook.data.local.PostsDao
import com.example.pluribook.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostsRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val postsDao: PostsDao
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

            postsDao.insertPost(newPost)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post")
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllPosts() {}

    suspend fun getPostsBySender() {}

    suspend fun updatePost() {}

    suspend fun deletePost() {}
}