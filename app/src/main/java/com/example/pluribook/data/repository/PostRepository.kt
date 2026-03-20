package com.example.pluribook.data.repository

import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pluribook.TAG
import com.example.pluribook.data.api.BookItem
import com.example.pluribook.data.api.BookNetworkClient
import com.example.pluribook.data.local.PostDao
import com.example.pluribook.data.local.UserDao
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.await
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

    fun getPostByIdStream(postId: String): Flow<Post?> = postDao.getPostFlowById(postId)

    fun startRealtimePostSync(postId: String): ListenerRegistration {
        return firestore.collection(POSTS_COLLECTION).document(postId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.toObject(Post::class.java)?.let { post ->
                    CoroutineScope(Dispatchers.IO).launch { postDao.insertPost(post) }
                }
            }
    }

    suspend fun searchBooks(query: String): List<BookItem> {
        return try {
            val response = BookNetworkClient.bookApi.fetchBooks(query).await()
            response.items ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Search books network error", e)
            emptyList()
        }
    }

    suspend fun fetchAndCachePost(postId: String) {
        try {
            val document = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            document.toObject(Post::class.java)?.let { postDao.insertPost(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching post $postId", e)
        }
    }

    suspend fun createPost(
        imageUri: Uri?,
        defaultImageUrl: String?,
        description: String,
        bookTitle: String,
        bookAuthor: String,
        bookSummary: String,
        bookRating: Double
    ): Boolean {
        return try {
            val postId = UUID.randomUUID().toString()
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

            val finalPhotoUrl = if (imageUri != null) {
                val storageRef = storage.reference.child("$POST_IMAGES_FOLDER/$postId.jpg")
                storageRef.putFile(imageUri).await()
                storageRef.downloadUrl.await().toString()
            } else {
                defaultImageUrl ?: ""
            }

            val newPost = Post(
                id = postId,
                photoUrl = finalPhotoUrl,
                description = description,
                senderId = userId,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                bookSummary = bookSummary,
                bookRating = bookRating
            )

            firestore.collection(POSTS_COLLECTION).document(postId).set(newPost).await()
            postDao.insertPost(newPost)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Create post failed", e)
            false
        }
    }

    suspend fun updatePost(
        postId: String, imageUri: Uri?, currentPhotoUrl: String, description: String,
        bookTitle: String, bookAuthor: String, bookSummary: String, bookRating: Double
    ): Boolean {
        return try {
            var finalPhotoUrl = currentPhotoUrl
            if (imageUri != null) {
                val storageRef = storage.reference.child("$POST_IMAGES_FOLDER/$postId.jpg")
                storageRef.putFile(imageUri).await()
                finalPhotoUrl = storageRef.downloadUrl.await().toString()
            }
            val updates = mapOf(
                "description" to description,
                "photoUrl" to finalPhotoUrl,
                "bookTitle" to bookTitle,
                "bookAuthor" to bookAuthor,
                "bookSummary" to bookSummary,
                "bookRating" to bookRating
            )
            firestore.collection(POSTS_COLLECTION).document(postId).update(updates).await()
            postDao.updatePost(
                postId,
                description,
                finalPhotoUrl,
                bookTitle,
                bookAuthor,
                bookSummary,
                bookRating
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Update post failed", e)
            false
        }
    }

    suspend fun fetchPost(postId: String): Post? {
        return try {
            val document = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            val post = document.toObject(Post::class.java)
            if (post != null) postDao.insertPost(post)
            post
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncPostsFromFirebase(isRefresh: Boolean = false) {
        if (isFetching || (isEndOfList && !isRefresh)) return
        isFetching = true
        try {
            var query = firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(PAGE_SIZE.toLong())
            if (isRefresh) {
                lastVisibleDoc = null; isEndOfList = false
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
        } finally {
            isFetching = false
        }
    }

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

    suspend fun syncUserPosts(senderId: String) {
        val documents =
            firestore.collection(POSTS_COLLECTION).whereEqualTo("senderId", senderId).get().await()
        postDao.insertPosts(documents.toObjects(Post::class.java))
    }

    suspend fun syncAndGetLikedPostIds(userId: String): List<String> {
        val userDocument =
            firestore.collection(UserRepository.USERS_COLLECTION).document(userId).get().await()
        val likedPostIds = userDocument.toObject(User::class.java)?.likedPosts ?: emptyList()
        if (likedPostIds.isNotEmpty()) {
            likedPostIds.chunked(10).forEach { chunk ->
                val snap = firestore.collection(POSTS_COLLECTION).whereIn("id", chunk).get().await()
                postDao.insertPosts(snap.toObjects(Post::class.java))
            }
        }
        return likedPostIds
    }

    suspend fun deletePost(postId: String): Boolean {
        return withContext(NonCancellable) {
            try {
                val post = postDao.getPostById(postId)
                    ?: firestore.collection(POSTS_COLLECTION).document(postId).get().await()
                        .toObject(Post::class.java)

                post?.likedBy?.let { userIds ->
                    if (userIds.isNotEmpty()) {
                        val batch = firestore.batch()
                        userIds.forEach { uid ->
                            val userRef =
                                firestore.collection(UserRepository.USERS_COLLECTION).document(uid)
                            batch.update(userRef, "likedPosts", FieldValue.arrayRemove(postId))

                            val localUser = userDao.getUserByUid(uid)
                            if (localUser != null) {
                                val updatedLikedPosts = localUser.likedPosts - postId
                                userDao.saveUser(localUser.copy(likedPosts = updatedLikedPosts))
                            }
                        }
                        batch.commit().await()
                    }
                }

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
}