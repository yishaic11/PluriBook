package com.example.pluribook.ui.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.example.pluribook.data.repository.CommentRepository
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.utils.ResourceState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.pluribook.TAG

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val commentDao = app.database.commentDao()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val commentRepository = CommentRepository(commentDao)
    private val postRepository = PostRepository(postDao = postDao, userDao = userDao)

    private val _postState = MutableLiveData<ResourceState<Post>>()
    val postState: LiveData<ResourceState<Post>> = _postState

    private val _isOwner = MutableLiveData<Boolean>()
    val isOwner: LiveData<Boolean> = _isOwner

    private val _senderName = MutableLiveData<String>()
    val senderName: LiveData<String> = _senderName

    private val _senderPhotoUrl = MutableLiveData<String>()
    val senderPhotoUrl: LiveData<String> = _senderPhotoUrl

    fun loadPost(postId: String) {
        _postState.value = ResourceState.Loading
        viewModelScope.launch {
            launch(Dispatchers.IO) { commentRepository.syncComments(postId) }

            val fetchedPost = withContext(Dispatchers.IO) {
                postDao.getPostById(postId)
            }
            if (fetchedPost != null) {
                _postState.value = ResourceState.Success(fetchedPost)
                _isOwner.value = fetchedPost.senderId == currentUserId

                fetchAuthorProfile(fetchedPost.senderId)
            } else {
                _postState.value = ResourceState.Error("Post not found")
            }
        }
    }

    private suspend fun fetchAuthorProfile(senderId: String) {
        try {
            val localUser = withContext(Dispatchers.IO) { userDao.getUserByUid(senderId) }
            if (localUser != null && localUser.username != "Unknown") {
                _senderName.value = localUser.username
                _senderPhotoUrl.value = localUser.photoUrl
                return
            }

            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(senderId).get().await()

            if (userDoc.exists()) {
                val name = userDoc.getString("username") ?: "Unknown User"
                val photoUrl = userDoc.getString("photoUrl") ?: ""

                _senderName.value = name
                _senderPhotoUrl.value = photoUrl

                withContext(Dispatchers.IO) {
                    val cachedAuthor = User(
                        uid = senderId,
                        username = name,
                        photoUrl = photoUrl,
                        email = "",
                        likedPosts = emptyList()
                    )
                    userDao.saveUser(cachedAuthor)
                }
            } else {
                _senderName.value = "Unknown User"
                _senderPhotoUrl.value = ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching author profile", e)
            _senderName.value = "Unknown User"
            _senderPhotoUrl.value = ""
        }
    }

    fun getCommentCount(postId: String): LiveData<Int> {
        return commentDao.getCommentCount(postId).asLiveData()
    }

    fun toggleLike(postId: String) {
        val uid = currentUserId ?: return
        val currentState = _postState.value

        if (currentState is ResourceState.Success) {
            val currentPost = currentState.data
            val isCurrentlyLiked = currentPost.likedBy.contains(uid)

            val newLikedBy =
                if (isCurrentlyLiked) currentPost.likedBy - uid else currentPost.likedBy + uid
            _postState.value = ResourceState.Success(currentPost.copy(likedBy = newLikedBy))

            viewModelScope.launch(Dispatchers.IO) {
                postRepository.toggleLike(postId, uid, isCurrentlyLiked)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            postRepository.deletePost(postId)
        }
    }
}