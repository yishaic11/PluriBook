package com.example.pluribook.ui.post

import android.app.Application
import androidx.lifecycle.*
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.CommentRepository
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication

    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val commentDao = app.database.commentDao()
    private val userRepository = UserRepository(userDao)
    private val postRepository = PostRepository(postDao, userDao)
    private val commentRepository = CommentRepository(commentDao)

    val currentUserId = userRepository.getCurrentUserId()

    private val _postState = MutableLiveData<ResourceState<Post>>()
    val postState: LiveData<ResourceState<Post>> = _postState

    private val _deleteState = MutableLiveData<ResourceState<Unit>>()
    val deleteState: LiveData<ResourceState<Unit>> = _deleteState

    private val _senderName = MutableLiveData<String>()
    val senderName: LiveData<String> = _senderName

    private val _senderPhotoUrl = MutableLiveData<String>()
    val senderPhotoUrl: LiveData<String> = _senderPhotoUrl

    private val _isOwner = MutableLiveData<Boolean>()
    val isOwner: LiveData<Boolean> = _isOwner

    private var senderSyncJob: Job? = null
    private var senderListener: ListenerRegistration? = null
    private var postSyncJob: Job? = null
    private var postListener: ListenerRegistration? = null

    fun loadPost(postId: String) {
        _postState.value = ResourceState.Loading
        commentRepository.syncComments(postId)

        postListener?.remove()
        postListener = postRepository.startRealtimePostSync(postId)

        postSyncJob?.cancel()
        postSyncJob = viewModelScope.launch(Dispatchers.IO) {
            launch { postRepository.fetchAndCachePost(postId) }
            postRepository.getPostByIdStream(postId).collect { post ->
                withContext(Dispatchers.Main) {
                    if (post != null) {
                        _postState.value = ResourceState.Success(post)
                        _isOwner.value = post.senderId == currentUserId
                        if (senderListener == null) loadSenderProfile(post.senderId)
                    }
                }
            }
        }
    }

    private fun loadSenderProfile(uid: String) {
        senderListener?.remove()
        senderListener = userRepository.startRealtimeUserSync(uid)
        senderSyncJob?.cancel()
        senderSyncJob = viewModelScope.launch(Dispatchers.IO) {
            launch { userRepository.fetchAndCacheUser(uid) }
            userRepository.getUserStream(uid).collect { user ->
                withContext(Dispatchers.Main) {
                    _senderName.value = user?.username ?: "Unknown User"
                    _senderPhotoUrl.value = user?.photoUrl ?: ""
                }
            }
        }
    }

    fun getCommentCount(postId: String): LiveData<Int> =
        commentRepository.getCommentStreamCount(postId).asLiveData()

    fun toggleLike(postId: String) {
        val uid = currentUserId ?: return
        val currentState = _postState.value
        if (currentState is ResourceState.Success) {
            val post = currentState.data
            val isLiked = post.likedBy.contains(uid)
            val newLikedBy = if (isLiked) post.likedBy - uid else post.likedBy + uid
            _postState.value = ResourceState.Success(post.copy(likedBy = newLikedBy))
            viewModelScope.launch(Dispatchers.IO) {
                postRepository.toggleLike(
                    postId,
                    uid,
                    isLiked
                )
            }
        }
    }

    fun deletePost(postId: String) {
        _deleteState.value = ResourceState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val success = postRepository.deletePost(postId)
            withContext(Dispatchers.Main) {
                if (success) {
                    _deleteState.value = ResourceState.Success(Unit)
                } else {
                    _deleteState.value =
                        ResourceState.Error("Failed to delete post. Please try again.")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        senderListener?.remove()
        postListener?.remove()
        commentRepository.stopSyncingComments()
    }
}