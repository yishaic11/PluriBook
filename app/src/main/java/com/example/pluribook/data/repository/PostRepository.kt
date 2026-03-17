package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pluribook.TAG
import com.example.pluribook.data.local.PostDao
import com.example.pluribook.data.local.UserDao
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val postDao: PostDao,
    private val userDao: UserDao,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val POST_IMAGES_FOLDER = "post_images"
        const val POSTS_COLLECTION = "posts"
        private const val PAGE_SIZE = 10
    }

    private var lastVisibleDoc: DocumentSnapshot? = null
    private var isFetching = false
    private var isEndOfList = false

    fun getPostStream(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { postDao.getPagedPosts() }
        ).flow
    }

    fun getPostsBySenderStream(senderId: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { postDao.getPagedPostsBySenderId(senderId) }
        ).flow
    }

    fun getLikedPostsStream(postIds: List<String>): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { postDao.getPagedPostsByIds(postIds) }
        ).flow
    }

    suspend fun syncPostsFromFirebase(isRefresh: Boolean = false) {
        if (isFetching || (isEndOfList && !isRefresh)) return
        isFetching = true

        try {
            var query = firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            if (isRefresh) {
                lastVisibleDoc = null
                isEndOfList = false
            } else {
                lastVisibleDoc?.let { query = query.startAfter(it) }
            }

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                isEndOfList = true
            } else {
                lastVisibleDoc = snapshot.documents.lastOrNull()
                val posts = snapshot.toObjects(Post::class.java)

                if (isRefresh) postDao.clearAllPosts()
                postDao.insertPosts(posts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firebase", e)
        } finally {
            isFetching = false
        }
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

    suspend fun syncUserPosts(senderId: String) {
        try {
            val documents = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("senderId", senderId)
                .get()
                .await()

            val posts = documents.toObjects(Post::class.java)

            if (posts.isNotEmpty()) {
                postDao.insertPosts(posts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user posts for $senderId", e)
        }
    }

    suspend fun syncAndGetLikedPostIds(userId: String): List<String> {
        return try {
            val userDocument =
                firestore.collection(UserRepository.USERS_COLLECTION).document(userId).get().await()
            val user = userDocument.toObject(User::class.java)
            val likedPostIds = user?.likedPosts ?: emptyList()

            if (likedPostIds.isEmpty()) return emptyList()

            val allFetchedPosts = mutableListOf<Post>()

            likedPostIds.chunked(PAGE_SIZE).forEach { chunk ->
                val snapshot = firestore.collection(POSTS_COLLECTION)
                    .whereIn("id", chunk)
                    .get().await()
                allFetchedPosts.addAll(snapshot.toObjects(Post::class.java))
            }

            postDao.insertPosts(allFetchedPosts)
            if (user != null) userDao.saveUser(user)

            likedPostIds

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked posts, falling back to local storage", e)

            val localUser = userDao.getUserByUid(userId)
            localUser?.likedPosts ?: emptyList()
        }
    }

    suspend fun updatePost() {}

    suspend fun toggleLike(postId: String, currentUserId: String, isCurrentlyLiked: Boolean) {
        val post = postDao.getPostById(postId)
        val user = userDao.getUserByUid(currentUserId)

        if (post != null) {
            val newLikedBy =
                if (isCurrentlyLiked) post.likedBy - currentUserId else post.likedBy + currentUserId
            postDao.insertPost(post.copy(likedBy = newLikedBy))
        }

        if (user != null) {
            val newUserLikes =
                if (isCurrentlyLiked) user.likedPosts - postId else user.likedPosts + postId
            userDao.saveUser(user.copy(likedPosts = newUserLikes))
        }

        val postRef = firestore.collection(POSTS_COLLECTION).document(postId)
        val userRef = firestore.collection(UserRepository.USERS_COLLECTION).document(currentUserId)

        if (isCurrentlyLiked) {
            postRef.update("likedBy", FieldValue.arrayRemove(currentUserId))
            userRef.update("likedPosts", FieldValue.arrayRemove(postId))
        } else {
            postRef.update("likedBy", FieldValue.arrayUnion(currentUserId))
            userRef.update("likedPosts", FieldValue.arrayUnion(postId))
        }
    }

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