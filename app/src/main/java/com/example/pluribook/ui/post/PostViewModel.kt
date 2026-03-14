package com.example.pluribook.ui.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val postRepository = PostRepository(
        postDao,
        userDao
    )
    private val userRepository = UserRepository(userDao)

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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
            try {
                val fetchedPost = withContext(Dispatchers.IO) {
                    app.database.postDao().getPostById(postId)
                }

                if (fetchedPost != null) {
                    _postState.value = ResourceState.Success(fetchedPost)
                    _isOwner.value = fetchedPost.senderId == currentUserId

                    val sender = withContext(Dispatchers.IO) {
                        userRepository.getUserProfile(fetchedPost.senderId)
                    }

                    _senderName.value = sender?.username ?: "Unknown User"
                    _senderPhotoUrl.value = sender?.photoUrl ?: ""
                } else {
                    _postState.value = ResourceState.Error("Post not found")
                }
            } catch (e: Exception) {
                _postState.value = ResourceState.Error(e.message ?: "An error occurred")
            }
        }
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