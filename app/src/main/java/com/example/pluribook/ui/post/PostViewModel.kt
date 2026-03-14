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
import com.example.pluribook.utils.ResourceState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val postDao = (application as PluribookApplication).database.postDao()
    private val userDao = (application as PluribookApplication).database.userDao()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val repository = PostRepository(
        FirebaseFirestore.getInstance(),
        FirebaseStorage.getInstance(),
        FirebaseAuth.getInstance(),
        postDao,
        userDao
    )

    private val _postState = MutableLiveData<ResourceState<Post>>()
    val postState: LiveData<ResourceState<Post>> = _postState

    private val _isOwner = MutableLiveData<Boolean>()
    val isOwner: LiveData<Boolean> = _isOwner

    private val _authorName = MutableLiveData<String>()
    val authorName: LiveData<String> = _authorName

    private val _authorPhotoUrl = MutableLiveData<String>()
    val authorPhotoUrl: LiveData<String> = _authorPhotoUrl

    fun loadPost(postId: String) {
        _postState.value = ResourceState.Loading
        viewModelScope.launch {
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
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(senderId).get().await()

            if (userDoc.exists()) {
                _authorName.value = userDoc.getString("username") ?: "Unknown User"
                _authorPhotoUrl.value = userDoc.getString("photoUrl") ?: ""
            } else {
                _authorName.value = "Unknown User"
                _authorPhotoUrl.value = ""
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error fetching author profile", e)
            _authorName.value = "Unknown User"
            _authorPhotoUrl.value = ""
        }
    }

    fun toggleLike(postId: String) {
        val uid = currentUserId ?: return

        val currentState = _postState.value
        if (currentState !is ResourceState.Success) return
        val currentPost = currentState.data

        val isCurrentlyLiked = currentPost.likedBy.contains(uid)

        val newLikedBy = if (isCurrentlyLiked) {
            currentPost.likedBy - uid
        } else {
            currentPost.likedBy + uid
        }
        _postState.value = ResourceState.Success(currentPost.copy(likedBy = newLikedBy))

        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleLike(postId, uid, isCurrentlyLiked)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deletePost(postId)
            }
        }
    }
}